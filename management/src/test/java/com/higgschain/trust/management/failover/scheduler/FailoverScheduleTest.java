package com.higgschain.trust.management.failover.scheduler;

import com.higgschain.trust.consensus.config.NodeState;
import com.higgschain.trust.consensus.config.NodeStateEnum;
import com.higgschain.trust.management.exception.FailoverExecption;
import com.higgschain.trust.management.exception.ManagementError;
import com.higgschain.trust.management.failover.config.FailoverProperties;
import com.higgschain.trust.management.failover.service.BlockSyncService;
import com.higgschain.trust.slave.core.repository.BlockRepository;
import com.higgschain.trust.slave.core.repository.PackageRepository;
import com.higgschain.trust.slave.core.service.block.BlockService;
import com.higgschain.trust.slave.core.service.pack.PackageService;
import com.higgschain.trust.slave.model.bo.Block;
import com.higgschain.trust.slave.model.bo.BlockHeader;
import com.higgschain.trust.slave.model.bo.Package;
import com.higgschain.trust.slave.model.bo.context.PackContext;
import com.higgschain.trust.slave.model.enums.biz.PackageStatusEnum;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;

import static org.testng.Assert.*;

/**
 * The type Failover schedule test.
 */
@RunWith(PowerMockRunner.class) public class FailoverScheduleTest {

    /**
     * The Failover schedule.
     */
    @InjectMocks @Autowired
    FailoverSchedule failoverSchedule;

    /**
     * The Block sync service.
     */
    @Mock BlockSyncService blockSyncService;
    /**
     * The Block service.
     */
    @Mock BlockService blockService;
    /**
     * The Package service.
     */
    @Mock PackageService packageService;
    /**
     * The Block repository.
     */
    @Mock BlockRepository blockRepository;
    /**
     * The Package repository.
     */
    @Mock PackageRepository packageRepository;
    /**
     * The Node state.
     */
    @Mock NodeState nodeState;
    /**
     * The Properties.
     */
    @Mock FailoverProperties properties;
    /**
     * The Pack.
     */
    @Mock Package pack;
    /**
     * The Current block.
     */
    @Mock Block currentBlock;
    /**
     * The Current header.
     */
    @Mock BlockHeader currentHeader;
    /**
     * The Tx nested.
     */
    @Mock TransactionTemplate txNested;
    private String currentHash = "preHash";

    /**
     * Before.
     */
    @BeforeClass public void before() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(currentBlock.getBlockHeader()).thenReturn(currentHeader);
    }

    /**
     * Before method.
     */
    @BeforeMethod public void beforeMethod() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(currentBlock.getBlockHeader()).thenReturn(currentHeader);
        Mockito.when(currentHeader.getBlockHash()).thenReturn(currentHash);
        Mockito.when(txNested.execute(Matchers.any(TransactionCallbackWithoutResult.class))).thenAnswer(invocation -> {
            TransactionCallback o = (TransactionCallback)invocation.getArguments()[0];
            return o.doInTransaction(new SimpleTransactionStatus());
        });
    }

    /**
     * Test failover not running.
     */
    @Test public void testFailoverNotRunning() {
        Mockito.when(nodeState.isState(NodeStateEnum.Running)).thenReturn(false);
        failoverSchedule.failover();
        Mockito.verify(blockRepository, Mockito.times(0)).getBlockHeader(Matchers.anyLong());
    }

    /**
     * Test failover not height.
     */
    @Test public void testFailoverNotHeight() {
        long height = 1L;
        Mockito.when(nodeState.isState(NodeStateEnum.Running)).thenReturn(true);
        Mockito.when(blockService.getMaxHeight()).thenReturn(height);

        failoverSchedule.failover();
        Mockito.verify(blockRepository, Mockito.times(0)).getBlockHeader(Matchers.anyLong());
        Mockito.verify(packageRepository, Mockito.times(0)).load(Matchers.anyLong());

        Package pack = Mockito.mock(Package.class);

        Mockito.when(packageRepository.load(height + 1)).thenReturn(pack);
        failoverSchedule.failover();
        Mockito.verify(blockRepository, Mockito.times(0)).getBlockHeader(Matchers.anyLong());

        Mockito.when(pack.getStatus()).thenReturn(PackageStatusEnum.RECEIVED);
        failoverSchedule.failover();
        Mockito.verify(blockRepository, Mockito.times(0)).getBlockHeader(Matchers.anyLong());

        Mockito.when(pack.getStatus()).thenReturn(PackageStatusEnum.FAILOVER);
        failoverSchedule.failover();
        Mockito.verify(blockRepository, Mockito.times(0)).getBlockHeader(Matchers.anyLong());
    }

    /**
     * Test failover not package.
     */
    @Test public void testFailoverNotPackage() {
        long height = 1L;
        Mockito.when(nodeState.isState(NodeStateEnum.Running)).thenReturn(true);
        Mockito.when(blockService.getMaxHeight()).thenReturn(height);

        Mockito.when(packageRepository.load(height + 1)).thenReturn(null);

        failoverSchedule.failover();

        Mockito.verify(blockRepository, Mockito.times(0)).getBlockHeader(Matchers.anyLong());
    }

    /**
     * Test failover step.
     */
    @Test public void testFailoverStep() {
        long height = 2L;
        int times = 5;
        Mockito.when(properties.getFailoverStep()).thenReturn(times);
        Mockito.when(nodeState.isState(NodeStateEnum.Running)).thenReturn(true);
        Mockito.when(nodeState.isState(NodeStateEnum.Running, NodeStateEnum.ArtificialSync)).thenReturn(true);
        Mockito.when(blockService.getMaxHeight()).thenReturn(height - 1);


        Mockito.when(packageRepository.load(height)).thenReturn(null);
        Mockito.when(blockRepository.getBlockHeader(height - 1)).thenReturn(currentHeader);

        ArrayList<Block> blocks = new ArrayList<>();
        Block block = Mockito.mock(Block.class);
        BlockHeader header = Mockito.mock(BlockHeader.class);
        Mockito.when(header.getHeight()).thenReturn(height);
        Mockito.when(block.getBlockHeader()).thenReturn(header);
        blocks.add(block);
        Mockito.when(blockSyncService.getBlocks(height, 1)).thenReturn(blocks);
        Mockito.doReturn(true).when(blockSyncService).validating(currentHash, block);

//        Mockito.when(blockService.getTempHeader(height, BlockHeaderTypeEnum.CONSENSUS_VALIDATE_TYPE)).thenReturn(header);
        PackContext context = Mockito.mock(PackContext.class);
        Mockito.when(packageService.createPackContext(Matchers.any())).thenReturn(context);
        Mockito.when(context.getCurrentBlock()).thenReturn(block);

        Mockito.when(blockService.compareBlockHeader(header, header)).thenReturn(true);

//        Mockito.when(blockService.getTempHeader(height, BlockHeaderTypeEnum.CONSENSUS_PERSIST_TYPE)).thenReturn(header);
        failoverSchedule.failover();
        Mockito.verify(properties, Mockito.times(times)).getFailoverStep();
    }

    /**
     * Test failover slave exception.
     */
    @Test public void testFailoverSLAVEException() {
        long height = 2L;
        Mockito.when(nodeState.isState(NodeStateEnum.Running)).thenReturn(true);
        Mockito.when(nodeState.isState(NodeStateEnum.Running, NodeStateEnum.ArtificialSync)).thenReturn(true);
        Mockito.when(blockService.getMaxHeight()).thenReturn(height - 1);


        Mockito.when(packageRepository.load(height)).thenReturn(null);
        Mockito.when(blockRepository.getBlockHeader(height - 1)).thenReturn(currentHeader);

        ArrayList<Block> blocks = new ArrayList<>();
        Block block = Mockito.mock(Block.class);
        BlockHeader header = Mockito.mock(BlockHeader.class);
        Mockito.when(header.getHeight()).thenReturn(height);
        Mockito.when(block.getBlockHeader()).thenReturn(header);
        blocks.add(block);
        Mockito.when(blockSyncService.getBlocks(height, 1)).thenReturn(blocks);
        Mockito.doReturn(true).when(blockSyncService).validating(currentHash, block);

//        Mockito.when(blockService.getTempHeader(height, BlockHeaderTypeEnum.CONSENSUS_VALIDATE_TYPE)).thenReturn(header);
        PackContext context = Mockito.mock(PackContext.class);
        Mockito.when(packageService.createPackContext(Matchers.any())).thenReturn(context);
        Mockito.when(context.getCurrentBlock()).thenReturn(block);

//        Mockito.when(blockService.getTempHeader(height, BlockHeaderTypeEnum.CONSENSUS_PERSIST_TYPE)).thenReturn(header);
        Mockito.when(blockService.compareBlockHeader(header, header)).thenReturn(true, false);
        failoverSchedule.failover();
    }

    /**
     * Test failover other exception.
     */
    @Test public void testFailoverOtherException() {
        Mockito.when(nodeState.isState(NodeStateEnum.Running)).thenReturn(true);
        Mockito.when(nodeState.isState(NodeStateEnum.Running, NodeStateEnum.ArtificialSync)).thenReturn(true);
        Mockito.when(blockService.getMaxHeight()).thenThrow(Exception.class);

        failoverSchedule.failover();
        Mockito.verify(properties, Mockito.times(0)).getFailoverStep();
    }

    /**
     * Test failover height false.
     */
    @Test public void testFailoverHeightFalse() {
        long height = 1L;
        ArrayList<Block> blocks = new ArrayList<>();
        Block block = Mockito.mock(Block.class);
        blocks.add(block);
        Mockito.when(blockSyncService.getBlocks(height, 1)).thenReturn(blocks);
        Mockito.when(blockRepository.getBlockHeader(height - 1)).thenReturn(currentHeader);
        Mockito.doReturn(true).when(blockSyncService).validating(currentHash, block);

        //不存在比较高的初始package
        Mockito.when(nodeState.isState(NodeStateEnum.Running, NodeStateEnum.ArtificialSync)).thenReturn(true);

        assertFalse(failoverSchedule.failover(height));

        //当前处理高度package不为FAILOVER
        Mockito.when(packageRepository.load(height)).thenReturn(pack);
        Mockito.when(pack.getStatus()).thenReturn(PackageStatusEnum.RECEIVED);
        assertFalse(failoverSchedule.failover(height));

        //package不存在，插入失败
        Mockito.when(packageRepository.load(height)).thenReturn(null);
        Mockito.doThrow(Exception.class).when(packageRepository).save(Matchers.any());
        assertFalse(failoverSchedule.failover(height));
    }

    /**
     * Test failover height.
     */
    @Test public void testFailoverHeight() {
        long height = 2L;
        Mockito.when(nodeState.isState(NodeStateEnum.Running, NodeStateEnum.ArtificialSync)).thenReturn(true);

        Mockito.when(packageRepository.load(height)).thenReturn(null);
        Mockito.when(blockRepository.getBlockHeader(height - 1)).thenReturn(currentHeader);
        Mockito.when(blockSyncService.getBlocks(height, 1)).thenReturn(null, new ArrayList<>());
        int times = 5;
        Mockito.when(properties.getTryTimes()).thenReturn(times);

        try {
            failoverSchedule.failover(height);
        } catch (FailoverExecption e) {
            assertEquals(e.getCode(), ManagementError.MANAGEMENT_FAILOVER_GET_VALIDATING_BLOCKS_FAILED);
        }
        Mockito.verify(blockSyncService, Mockito.times(times)).getBlocks(height, 1);
    }

    /**
     * Test failover block.
     */
    @Test public void testFailoverBlock() {
        long height = 2L;
        Mockito.when(nodeState.isState(NodeStateEnum.Running, NodeStateEnum.ArtificialSync)).thenReturn(true);
        Mockito.when(packageRepository.load(height)).thenReturn(null);
        Mockito.when(blockRepository.getBlockHeader(height - 1)).thenReturn(currentHeader);

        ArrayList<Block> blocks = new ArrayList<>();
        Block block = Mockito.mock(Block.class);
        BlockHeader header = Mockito.mock(BlockHeader.class);
        Mockito.when(header.getHeight()).thenReturn(height);
        Mockito.when(block.getBlockHeader()).thenReturn(header);
        blocks.add(block);
        Mockito.when(blockSyncService.getBlocks(height, 1)).thenReturn(blocks);
        Mockito.doReturn(true).when(blockSyncService).validating(currentHash, block);


        //no validate header
//        Mockito.when(blockService.getTempHeader(height, BlockHeaderTypeEnum.CONSENSUS_VALIDATE_TYPE)).thenReturn(null);
        try {
            failoverSchedule.failover(height);
        } catch (FailoverExecption e) {
            assertEquals(e.getCode(), ManagementError.MANAGEMENT_FAILOVER_CONSENSUS_VALIDATE_NOT_EXIST);
        }

        //header compare: validating failed, has validate header
//        Mockito.when(blockService.getTempHeader(height, BlockHeaderTypeEnum.CONSENSUS_VALIDATE_TYPE)).thenReturn(header);
        PackContext context = Mockito.mock(PackContext.class);
        Mockito.when(packageService.createPackContext(Matchers.any())).thenReturn(context);
        Mockito.when(context.getCurrentBlock()).thenReturn(block);

        Mockito.when(blockService.compareBlockHeader(header, header)).thenReturn(false);
        try {
            failoverSchedule.failover(height);
        } catch (FailoverExecption e) {
            assertEquals(e.getCode(), ManagementError.MANAGEMENT_FAILOVER_BLOCK_VALIDATE_RESULT_INVALID);
        }

        //header compare: validating passed, no persist header
        Mockito.when(blockService.compareBlockHeader(header, header)).thenReturn(true);
//        Mockito.when(blockService.getTempHeader(height, BlockHeaderTypeEnum.CONSENSUS_PERSIST_TYPE)).thenReturn(null);
        try {
            failoverSchedule.failover(height);
        } catch (FailoverExecption e) {
            assertEquals(e.getCode(), ManagementError.MANAGEMENT_FAILOVER_CONSENSUS_PERSIST_NOT_EXIST);
        }

        //header compare: validating passed，persisting failed
//        Mockito.when(blockService.getTempHeader(height, BlockHeaderTypeEnum.CONSENSUS_PERSIST_TYPE)).thenReturn(header);
        Mockito.when(blockService.compareBlockHeader(header, header)).thenReturn(true, false);
        try {
            failoverSchedule.failover(height);
        } catch (FailoverExecption e) {
            assertEquals(e.getCode(), ManagementError.MANAGEMENT_FAILOVER_BLOCK_PERSIST_RESULT_INVALID);
        }
        //header compare:  validating passed，persisting passed
        Mockito.when(blockService.compareBlockHeader(header, header)).thenReturn(true);
        assertTrue(failoverSchedule.failover(height));
    }

}