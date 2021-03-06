package com.higgschain.trust.management.failover.scheduler;

import com.higgschain.trust.common.dao.RocksUtils;
import com.higgschain.trust.common.enums.MonitorTargetEnum;
import com.higgschain.trust.common.utils.MonitorLogUtils;
import com.higgschain.trust.common.utils.ThreadLocalUtils;
import com.higgschain.trust.consensus.config.NodeState;
import com.higgschain.trust.consensus.config.NodeStateEnum;
import com.higgschain.trust.management.exception.FailoverExecption;
import com.higgschain.trust.management.exception.ManagementError;
import com.higgschain.trust.management.failover.config.FailoverProperties;
import com.higgschain.trust.management.failover.service.BlockSyncService;
import com.higgschain.trust.management.failover.service.SyncService;
import com.higgschain.trust.slave.common.config.InitConfig;
import com.higgschain.trust.slave.common.exception.SlaveException;
import com.higgschain.trust.slave.core.repository.BlockRepository;
import com.higgschain.trust.slave.core.repository.PackageRepository;
import com.higgschain.trust.slave.core.service.block.BlockService;
import com.higgschain.trust.slave.core.service.consensus.view.ClusterViewService;
import com.higgschain.trust.slave.core.service.pack.PackageProcess;
import com.higgschain.trust.slave.core.service.pack.PackageService;
import com.higgschain.trust.slave.model.bo.Block;
import com.higgschain.trust.slave.model.bo.BlockHeader;
import com.higgschain.trust.slave.model.bo.Package;
import com.higgschain.trust.slave.model.bo.context.PackContext;
import com.higgschain.trust.slave.model.enums.biz.PackageStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.Transaction;
import org.rocksdb.WriteOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * The type Failover schedule.
 */
@Service
@Slf4j
public class FailoverSchedule {

    @Autowired private BlockSyncService blockSyncService;
    @Autowired private BlockService blockService;
    @Autowired private PackageService packageService;
    @Autowired private BlockRepository blockRepository;
    @Autowired private PackageRepository packageRepository;
    @Autowired private NodeState nodeState;
    @Autowired private FailoverProperties properties;
    @Autowired private TransactionTemplate txNested;
    @Autowired private InitConfig initConfig;
    @Autowired private PackageProcess packageProcess;
    @Autowired private SyncService syncService;
    @Autowired private ClusterViewService clusterViewService;

    /**
     * 自动failover，判断状态是否为NodeStateEnum.Running
     */
    @Scheduled(fixedDelayString = "${trust.schedule.failover:10000}")
    public void failover() {
        if (!nodeState.isState(NodeStateEnum.Running)) {
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("failover starting ...");
        }
        int size = 0;
        try {
            do {
                Long height = blockService.getMaxHeight();
                long nextHeight = height + 1;
                if (!needFailover(nextHeight)) {
                    break;
                }
                if (!failover(nextHeight)) {
                    log.warn("failover block:{} failed", nextHeight);
                    break;
                }
            } while (++size < properties.getFailoverStep());
        } catch (SlaveException e) {
            if (e.getCode() == ManagementError.MANAGEMENT_FAILOVER_BLOCK_PERSIST_RESULT_INVALID) {
                nodeState.changeState(NodeStateEnum.Running, NodeStateEnum.Offline);
            }
            log.error("failover block error：{}", e.getCode().getDescription(), e);
            MonitorLogUtils.logIntMonitorInfo(MonitorTargetEnum.FAILOVER_BLOCK_ERROR, 1);
        } catch (Exception e) {
            log.error("failover error", e);
            MonitorLogUtils.logIntMonitorInfo(MonitorTargetEnum.FAILOVER_BLOCK_ERROR, 1);
        }
    }

    /**
     * standby 自动 failover，判断状态是否为NodeStateEnum.Running
     */
    @Scheduled(fixedDelayString = "${trust.schedule.failover:10000}")
    public void standbyFailover() {
        //if not standby no need to autoSync
        if (!nodeState.isState(NodeStateEnum.Standby)) {
            return;
        }

        //init cluster view from cluster every task
        clusterViewService.initClusterViewFromCluster();

        //autoSync block
        syncService.autoSync();
    }

    /**
     * failover指定高度
     *
     * @param height 区块高度
     * @return the boolean
     */
    public synchronized boolean failover(long height) {
        if (!nodeState.isState(NodeStateEnum.Running, NodeStateEnum.ArtificialSync)) {
            throw new FailoverExecption(ManagementError.MANAGEMENT_FAILOVER_STATE_NOT_ALLOWED);
        }
        log.info("failover block:{}", height);
        BlockHeader preBlockHeader = blockRepository.getBlockHeader(height - 1);
        int tryTimes = 0;
        Block block = null;
        boolean validated = false;
        do {
            List<Block> blocks = blockSyncService.getBlocks(height, 1);
            if (blocks != null && !blocks.isEmpty()) {
                block = blocks.get(0);
                if (log.isDebugEnabled()) {
                    log.debug("got the block:{}", block);
                }
                validated = blockSyncService.validating(preBlockHeader.getBlockHash(), block);
            }
        } while (!validated && ++tryTimes < properties.getTryTimes());
        if (!validated) {
            throw new FailoverExecption(ManagementError.MANAGEMENT_FAILOVER_GET_VALIDATING_BLOCKS_FAILED);
        }
        if (!checkAndInsert(height)) {
            return false;
        }
        Block finalBlock = block;
        failoverBlock(finalBlock);

        return true;
    }

    /**
     * 检查是否需要failover
     *
     * @param height
     * @return
     */
    private boolean needFailover(long height) {
        Long maxHeight = packageRepository.getMaxHeight();
        //no package or next block height is bigger than max pack block height.No need to
        if (maxHeight == null || height > maxHeight.longValue()) {
            return false;
        }
        //next block height  in  packs  and status is fail over  .Need to
        Package pack = packageRepository.load(height);
        if (pack != null && pack.getStatus() == PackageStatusEnum.FAILOVER) {
            return true;
        }
        //next block height is smaller than max pack height and not pack for next block . Need to
        if (height < maxHeight.longValue() && pack == null) {
            return true;
        }

        return false;
    }

    /**
     * check whether failover is needed, and insert the failover package if necessary.
     *
     * @param height 区块高度
     * @return
     */
    private boolean checkAndInsert(long height) {
        if (log.isDebugEnabled()) {
            log.debug("check and instert package:{} for failover", height);
        }
        Long maxPackHeight = packageRepository.getMaxHeight();
        if (maxPackHeight == null || height > maxPackHeight.longValue()) {
            return false;
        }
        Package pack = packageRepository.load(height);
        if (pack == null) {
            return insertFailoverPackage(height);
        } else {
            return pack.getStatus() == PackageStatusEnum.FAILOVER;
        }
    }

    /**
     * insert the failover package if the height package not exist
     *
     * @param height the height of package
     * @return
     */
    private boolean insertFailoverPackage(long height) {
        if (log.isDebugEnabled()) {
            log.debug("insert failover package:{}", height);
        }
        Package pack = new Package();
        pack.setPackageTime(System.currentTimeMillis());
        pack.setHeight(height);
        pack.setStatus(PackageStatusEnum.FAILOVER);
        try {
            if (!initConfig.isUseMySQL()) {
                Transaction tx = RocksUtils.beginTransaction(new WriteOptions());
                try {
                    ThreadLocalUtils.putRocksTx(tx);
                    packageRepository.save(pack);
                    RocksUtils.txCommit(tx);
                } finally {
                    ThreadLocalUtils.clearRocksTx();
                }
            } else {
                packageRepository.save(pack);
            }
        } catch (Exception e) {
            log.warn("insert failover package failed.", e);
            return false;
        }
        return true;
    }

    /**
     * failover block,执行validating,结果db验证，执行persisting,结果db验证，验证结果db不存在时结束，下次继续验证
     *
     * @param block 区块
     * @return 同步结果
     */
    private void failoverBlock(Block block) {
        log.info("failover block:{}", block);
        BlockHeader blockHeader = block.getBlockHeader();
        Package pack = new Package();
        pack.setPackageTime(blockHeader.getBlockTime());
        pack.setHeight(blockHeader.getHeight());
        pack.setStatus(PackageStatusEnum.FAILOVER);
        pack.setSignedTxList(block.getSignedTxList());
        PackContext packContext = packageService.createPackContext(pack);
        if (initConfig.isUseMySQL()) {
            txNested.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    packageService.process(packContext, true, false);
                }
            });
        } else {
            try {
                Transaction tx = RocksUtils.beginTransaction(new WriteOptions());
                ThreadLocalUtils.putRocksTx(tx);
                packageService.process(packContext, true, false);
                RocksUtils.txCommit(tx);
            } finally {
                ThreadLocalUtils.clearRocksTx();
            }
        }
        //update block height in memory
        packageProcess.updateProcessedHeight(blockHeader.getHeight());
    }
}
