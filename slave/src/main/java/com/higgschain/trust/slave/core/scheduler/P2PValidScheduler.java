package com.higgschain.trust.slave.core.scheduler;

import com.higgschain.trust.common.enums.MonitorTargetEnum;
import com.higgschain.trust.common.utils.MonitorLogUtils;
import com.higgschain.trust.consensus.config.NodeState;
import com.higgschain.trust.consensus.config.NodeStateEnum;
import com.higgschain.trust.slave.common.enums.SlaveErrorEnum;
import com.higgschain.trust.slave.common.exception.SlaveException;
import com.higgschain.trust.slave.core.repository.PackageRepository;
import com.higgschain.trust.slave.core.repository.PendingTxRepository;
import com.higgschain.trust.slave.core.service.pack.PackageService;
import com.higgschain.trust.slave.integration.block.BlockChainClient;
import com.higgschain.trust.slave.model.bo.BlockHeader;
import com.higgschain.trust.slave.model.enums.biz.PackageStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * The type P 2 p valid scheduler.
 *
 * @author tangfashuang
 * @date 2018 /04/09 15:30
 */
@ConditionalOnProperty(name = "higgs.trust.isSlave", havingValue = "true", matchIfMissing = true) @Component
@Slf4j public class P2PValidScheduler {
    @Autowired private TransactionTemplate txRequired;

    @Autowired private PackageRepository packageRepository;

    @Autowired private PendingTxRepository pendingTxRepository;

    @Autowired private PackageService packageService;

    @Autowired private NodeState nodeState;

    @Autowired private BlockChainClient blockChainClient;

    @Value("${p2p.valid.limit:1000}") private int validLimit;
    @Value("${p2p.valid.p2pRetryNum:3}") private int p2pRetryNum;
    @Value("${p2p.valid.exeRetryNum:10}") private int exeRetryNum;

    /**
     * p2p valid handler
     */
    @Scheduled(fixedDelayString = "${p2p.valid.delay:10000}") public void exe() {
        if (!nodeState.isState(NodeStateEnum.Running)) {
            return;
        }
        //remove already blocked packages and pending-transactions
        removeAlreadyBlocked();
        //get max height by 'WAIT_PERSIST_CONSENSUS' status
        Long maxHeight = packageRepository.getMaxHeightByStatus(PackageStatusEnum.WAIT_PERSIST_CONSENSUS);
        if (maxHeight == null || maxHeight.equals(0L)) {
            return;
        }
        Long startHeight = maxHeight;
        int size = 1;
        List<BlockHeader> blockHeaders = null;
        int num = 0;
        do {
            try {
                log.info("start get header from p2p layer,startHeight:{},size:{}", startHeight, size);
                blockHeaders = blockChainClient.getBlockHeaders(nodeState.notMeNodeNameReg(), maxHeight, size);
            } catch (Throwable t) {
                log.error("get header has error by p2p layer,startHeight:{},size:{}", startHeight, size, t);
            }
            if (!CollectionUtils.isEmpty(blockHeaders)) {
                break;
            }
            sleep(100L + 500 * num);
        } while (++num < p2pRetryNum);

        if (CollectionUtils.isEmpty(blockHeaders)) {
            log.warn("get header is empty by p2p layer");
            return;
        }
        //process for each
        for (BlockHeader header : blockHeaders) {
            doPersisted(header,true);
        }
        //get min height by 'WAIT_PERSIST_CONSENSUS' status
        Long minHeight = packageRepository.getMinHeightByStatus(PackageStatusEnum.WAIT_PERSIST_CONSENSUS);
        if (minHeight == null || minHeight.equals(0L)) {
            return;
        }
        //process the remaining for each
        for (Long i = minHeight; i < maxHeight; i++) {
            BlockHeader header = new BlockHeader();
            header.setHeight(i);
            doPersisted(header,false);
        }
    }

    /**
     * process header
     *
     * @param header
     */
    private void doPersisted(BlockHeader header,boolean isCompare) {
        int i = 0;
        boolean isSuccess = false;
        do {
            try {
                packageService.persisted(header,isCompare);
                isSuccess = true;
                break;
            } catch (SlaveException e) {
                if(e.getCode() == SlaveErrorEnum.SLAVE_PACKAGE_TWO_HEADER_UNEQUAL_ERROR){
                    nodeState.changeState(nodeState.getState(), NodeStateEnum.Offline);
                    log.warn("doPersisted height:{} is fail so change status to offline", header.getHeight());
                    MonitorLogUtils.logIntMonitorInfo(MonitorTargetEnum.SLAVE_PACKAGE_PROCESS_ERROR.getMonitorTarget(), 1);
                    throw new RuntimeException("doPersisted is fail retry:" + exeRetryNum);
                }
            } catch (Throwable t) {
                log.error("doPersisted has error", t);
            }
            sleep(100L + 100 * i);
        } while (++i < exeRetryNum);
        if (!isSuccess) {
            throw new RuntimeException("doPersisted is fail retry:" + exeRetryNum);
        }
    }

    /**
     * remove already blocked packages and pending-transactions
     *
     */
    private void removeAlreadyBlocked(){
        //get max persisted height
        Long maxPersistedHeight = packageRepository.getMaxHeightByStatus(PackageStatusEnum.PERSISTED);
        if(maxPersistedHeight == null || maxPersistedHeight.equals(0L)){
            return;
        }
        //start
        boolean result = txRequired.execute(transactionStatus -> {
            //get heights by status=PERSISTED
            List<Long> heights = packageRepository.getBlockHeightsByStatus(PackageStatusEnum.PERSISTED);
            if(CollectionUtils.isEmpty(heights)){
                log.info("package.PERSISTED.heights is empty ");
                return false;
            }
            //delete pending tx by height
            heights.forEach(height->{
                pendingTxRepository.deleteByHeight(height);
            });
            //detete package
            int r = packageRepository.deleteLessThanHeightAndStatus(maxPersistedHeight,PackageStatusEnum.PERSISTED);
            if(r != 0){
                log.info("deleted package less than height:{},size:{}",maxPersistedHeight,r);
            }else{
                throw new RuntimeException("delete package less than height:"+maxPersistedHeight+" and status:PERSISTED is fail");
            }
            return true;
        });
    }
    /**
     * thread sleep
     *
     * @param times
     */
    private void sleep(Long times) {
        try {
            Thread.sleep(times);
        } catch (InterruptedException e) {
            log.error("has InterruptedException", e);
        }
    }
}
