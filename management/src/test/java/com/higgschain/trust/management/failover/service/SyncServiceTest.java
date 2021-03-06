package com.higgschain.trust.management.failover.service;

import com.higgschain.trust.consensus.config.NodeState;
import com.higgschain.trust.consensus.config.NodeStateEnum;
import com.higgschain.trust.management.exception.ManagementError;
import com.higgschain.trust.management.failover.config.FailoverProperties;
import com.higgschain.trust.slave.common.enums.SlaveErrorEnum;
import com.higgschain.trust.slave.common.exception.SlaveException;
import com.higgschain.trust.slave.core.repository.BlockRepository;
import com.higgschain.trust.slave.core.service.block.BlockService;
import com.higgschain.trust.slave.core.service.pack.PackageService;
import com.higgschain.trust.slave.model.bo.Block;
import com.higgschain.trust.slave.model.bo.BlockHeader;
import com.higgschain.trust.slave.model.bo.context.PackContext;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.stubbing.Answer;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.testng.Assert.assertEquals;

/**
 * The type Sync service test.
 */
@RunWith(PowerMockRunner.class) public class SyncServiceTest {

    /**
     * The Sync service.
     */
    @InjectMocks @Autowired
    SyncService syncService;
    /**
     * The Properties.
     */
    @Mock FailoverProperties properties;
    /**
     * The Block service.
     */
    @Mock BlockService blockService;
    /**
     * The Block repository.
     */
    @Mock BlockRepository blockRepository;
    /**
     * The Block sync service.
     */
    @Mock
    BlockSyncService blockSyncService;
    /**
     * The Package service.
     */
    @Mock PackageService packageService;
    /**
     * The Node state.
     */
    @Mock NodeState nodeState;
    /**
     * The Tx nested.
     */
    @Mock TransactionTemplate txNested;

    /**
     * The Current height.
     */
    long currentHeight = 1;
    /**
     * The Header.
     */
    @Mock BlockHeader header;

    /**
     * The Blocks.
     */
    @Mock List<Block> blocks;

    /**
     * Before method.
     */
    @BeforeMethod public void beforeMethod() {
        MockitoAnnotations.initMocks(this);
        Mockito.reset(properties, nodeState);
        Mockito.when(nodeState.isState(NodeStateEnum.AutoSync, NodeStateEnum.ArtificialSync)).thenReturn(true);
        Mockito.when(blockRepository.getMaxHeight()).thenReturn(currentHeight);
        Mockito.when(properties.getHeaderStep()).thenReturn(10);
    }

    /**
     * Test sync not state.
     */
    @Test public void testSyncNotState() {
        Mockito.when(nodeState.isState(NodeStateEnum.AutoSync, NodeStateEnum.ArtificialSync)).thenReturn(false);
        syncService.autoSync();
        Mockito.verify(blockRepository, Mockito.times(0)).getMaxHeight();
    }

    /**
     * Test sync get cluster height failed.
     */
    @Test public void testSyncGetClusterHeightFailed() {
        Mockito.when(blockSyncService.getClusterHeight(Matchers.anyInt())).thenReturn(null);
        try {
            syncService.autoSync();
        } catch (SlaveException e) {
            assertEquals(e.getCode(), SlaveErrorEnum.SLAVE_CONSENSUS_GET_RESULT_FAILED);
        }
    }

    /**
     * Test sync with param not state.
     */
    @Test public void testSyncWithParamNotState() {
        long startHeight = currentHeight + 1;
        int size = 10;
        Mockito.when(nodeState.isState(NodeStateEnum.AutoSync, NodeStateEnum.ArtificialSync)).thenReturn(false);
        try {
            syncService.sync(Matchers.anyLong(), Matchers.anyInt());
        } catch (SlaveException e) {
            assertEquals(e.getCode(), ManagementError.MANAGEMENT_FAILOVER_STATE_NOT_ALLOWED);
        }
        Mockito.verify(blockRepository, Mockito.times(0)).getMaxHeight();
    }

    /**
     * Test sync with param height not current.
     */
    @Test public void testSyncWithParamHeightNotCurrent() {
        long startHeight = currentHeight + 1;
        int size = 10;
        try {
            syncService.sync(currentHeight, size);
        } catch (SlaveException e) {
            assertEquals(e.getCode(), ManagementError.MANAGEMENT_FAILOVER_START_HEIGHT_ERROR);
        }
    }

    /**
     * Test sync with param get headers failed.
     */
    @Test public void testSyncWithParamGetHeadersFailed() {
        long startHeight = currentHeight + 1;
        int size = 10;
        Integer times = 3;
        Mockito.when(properties.getTryTimes()).thenReturn(times);
        Mockito.when(blockRepository.getBlockHeader(currentHeight)).thenReturn(header);
        Mockito.when(blockSyncService.getHeaders(startHeight, size)).thenReturn(null, Collections.emptyList());
        try {
            syncService.sync(startHeight, size);
        } catch (SlaveException e) {
            assertEquals(e.getCode(), ManagementError.MANAGEMENT_FAILOVER_GET_VALIDATING_HEADERS_FAILED);
        }

        //try times test
        Mockito.reset(properties);
        Mockito.when(properties.getTryTimes()).thenReturn(times);
        try {
            syncService.sync(startHeight, size);
        } catch (SlaveException e) {
            assertEquals(e.getCode(), ManagementError.MANAGEMENT_FAILOVER_GET_VALIDATING_HEADERS_FAILED);
        }
        Mockito.verify(properties, Mockito.times(times)).getTryTimes();

        //validating headers failed
        Mockito.when(blockSyncService.getHeaders(Matchers.anyLong(), Matchers.anyInt()))
            .thenAnswer((Answer<List<BlockHeader>>)invocation -> {
                Object[] arguments = invocation.getArguments();
                return mockHeaders((Long)arguments[0], (int)arguments[1]);
            });
        Mockito.when(blockSyncService.validating(Matchers.anyString(), Matchers.anyList())).thenReturn(false);
        try {
            syncService.sync(startHeight, size);
        } catch (SlaveException e) {
            assertEquals(e.getCode(), ManagementError.MANAGEMENT_FAILOVER_GET_VALIDATING_HEADERS_FAILED);
        }

        //bft validating failed
        Mockito.when(blockSyncService.getHeaders(Matchers.anyLong(), Matchers.anyInt()))
            .thenAnswer((Answer<List<BlockHeader>>)invocation -> {
                Object[] arguments = invocation.getArguments();
                return mockHeaders((Long)arguments[0], (int)arguments[1]);
            });
        Mockito.when(blockSyncService.validating(Matchers.anyString(), Matchers.anyList())).thenReturn(true);
        Mockito.when(blockSyncService.bftValidating(Matchers.any())).thenReturn(null, false);
        try {
            syncService.sync(startHeight, size);
        } catch (SlaveException e) {
            assertEquals(e.getCode(), ManagementError.MANAGEMENT_FAILOVER_GET_VALIDATING_HEADERS_FAILED);
        }

        try {
            syncService.sync(startHeight, size);
        } catch (SlaveException e) {
            assertEquals(e.getCode(), ManagementError.MANAGEMENT_FAILOVER_GET_VALIDATING_HEADERS_FAILED);
        }
    }

    /**
     * Test sync with param get blocks failed.
     */
    @Test public void testSyncWithParamGetBlocksFailed() {
        long startHeight = currentHeight + 1;
        int size = 100, times = 3, blockStep = 10;
        List<BlockHeader> headers = mockHeaders(startHeight, size);
        Mockito.when(properties.getTryTimes()).thenReturn(times);
        Mockito.when(properties.getBlockStep()).thenReturn(blockStep);
        Mockito.when(blockRepository.getBlockHeader(currentHeight)).thenReturn(header);
        Mockito.when(blockSyncService.getHeaders(startHeight, size)).thenReturn(headers);
        Mockito.when(blockSyncService.validating(Matchers.anyString(), Matchers.anyList())).thenReturn(true);
        Mockito.when(blockSyncService.bftValidating(Matchers.any())).thenReturn(true);
        Mockito.when(blockSyncService.getBlocks(Matchers.anyLong(), Matchers.anyInt()))
            .thenReturn(null, Collections.emptyList());

        //block null,empty
        try {
            syncService.sync(startHeight, size);
        } catch (SlaveException e) {
            assertEquals(e.getCode(), ManagementError.MANAGEMENT_FAILOVER_GET_VALIDATING_BLOCKS_FAILED);
        }
    }

    /**
     * Test sync with param get blocks bft failed.
     */
    @Test public void testSyncWithParamGetBlocksBftFailed() {
        long startHeight = currentHeight + 1;
        int size = 100, times = 3, blockStep = 10;
        List<BlockHeader> headers = mockHeaders(startHeight, size);
        Mockito.when(properties.getTryTimes()).thenReturn(times);
        Mockito.when(properties.getBlockStep()).thenReturn(blockStep);
        Mockito.when(blockRepository.getBlockHeader(currentHeight)).thenReturn(header);
        Mockito.when(blockSyncService.getHeaders(startHeight, size)).thenReturn(headers);
        Mockito.when(blockSyncService.validating(Matchers.anyString(), Matchers.anyList())).thenReturn(true);
        Mockito.when(blockSyncService.bftValidating(Matchers.any())).thenReturn(true);

        List<Block> blocks = mockBlocks(startHeight, size);
        Mockito.when(blockSyncService.getBlocks(Matchers.anyLong(), Matchers.anyInt())).thenReturn(blocks);
        Mockito.when(blockSyncService.validatingBlocks(Matchers.any(), Matchers.any())).thenReturn(false);
        try {
            syncService.sync(startHeight, size);
        } catch (SlaveException e) {
            assertEquals(e.getCode(), ManagementError.MANAGEMENT_FAILOVER_GET_VALIDATING_BLOCKS_FAILED);
        }
        Mockito.verify(blockSyncService, Mockito.times(times)).getBlocks(Matchers.anyLong(), Matchers.anyInt());
    }

    /**
     * Test sync with param get block valid.
     */
    @Test public void testSyncWithParamGetBlockValid() {
        long startHeight = currentHeight + 1;
        int size = 100, times = 3, blockStep = 10;
        List<BlockHeader> headers = mockHeaders(startHeight, size);
        Mockito.when(properties.getTryTimes()).thenReturn(times);
        Mockito.when(properties.getBlockStep()).thenReturn(blockStep);
        Mockito.when(blockRepository.getBlockHeader(currentHeight)).thenReturn(header);
        Mockito.when(blockSyncService.getHeaders(startHeight, size)).thenReturn(headers);
        Mockito.when(blockSyncService.validating(Matchers.anyString(), Matchers.anyList())).thenReturn(true);
        Mockito.when(blockSyncService.bftValidating(Matchers.any())).thenReturn(true);

        List<Block> blocks = mockBlocks(startHeight, size);
        Mockito.when(blockSyncService.getBlocks(Matchers.anyLong(), Matchers.anyInt())).thenReturn(blocks);
        Mockito.when(blockSyncService.validatingBlocks(Matchers.any(), Matchers.any())).thenReturn(true);
        Mockito.when(blockService.compareBlockHeader(Matchers.any(), Matchers.any())).thenReturn(false);
        try {
            syncService.sync(startHeight, size);
        } catch (SlaveException e) {
            assertEquals(e.getCode(), ManagementError.MANAGEMENT_FAILOVER_SYNC_BLOCK_VALIDATING_FAILED);
        }
        Mockito.verify(blockService, Mockito.times(1)).compareBlockHeader(Matchers.any(), Matchers.any());
    }

    /**
     * Test sync block.
     */
    @Test public void testSyncBlock() {
        long startHeight = currentHeight + 1;
        int size = 100, times = 3, blockStep = 10;
        List<BlockHeader> headers = mockHeaders(startHeight, size);
        Mockito.when(properties.getTryTimes()).thenReturn(times);
        Mockito.when(properties.getBlockStep()).thenReturn(blockStep);
        Mockito.when(blockRepository.getBlockHeader(currentHeight)).thenReturn(header);
        Mockito.when(blockSyncService.getHeaders(startHeight, size)).thenReturn(headers);
        Mockito.when(blockSyncService.validating(Matchers.anyString(), Matchers.anyList())).thenReturn(true);
        Mockito.when(blockSyncService.bftValidating(Matchers.any())).thenReturn(true);

        Mockito.when(blockSyncService.getBlocks(Matchers.anyLong(), Matchers.anyInt()))
            .thenAnswer((Answer<List<Block>>)invocation -> {
                Object[] arguments = invocation.getArguments();
                return mockBlocks((Long)arguments[0], (int)arguments[1]);
            });
        Mockito.when(blockSyncService.validatingBlocks(Matchers.any(), Matchers.any())).thenReturn(true);
        Mockito.when(blockService.compareBlockHeader(Matchers.any(), Matchers.any())).thenReturn(true);
        PackContext pack = Mockito.mock(PackContext.class);
        Block block = Mockito.mock(Block.class);
        BlockHeader theader = Mockito.mock(BlockHeader.class);
        Mockito.when(block.getBlockHeader()).thenReturn(theader);
        Mockito.when(pack.getCurrentBlock()).thenReturn(block);
        Mockito.when(packageService.createPackContext(Matchers.any())).thenReturn(pack);
        syncService.sync(startHeight, size);
//        Mockito.verify(packageService, Mockito.times(size)).persisting(Matchers.any());
    }

    /**
     * Test sync block validating failed.
     */
    @Test public void testSyncBlockValidatingFailed() {
        long startHeight = currentHeight + 1;
        int size = 100, times = 3, blockStep = 10;
        List<BlockHeader> headers = mockHeaders(startHeight, size);
        Mockito.when(properties.getTryTimes()).thenReturn(times);
        Mockito.when(properties.getBlockStep()).thenReturn(blockStep);
        Mockito.when(blockRepository.getBlockHeader(currentHeight)).thenReturn(header);
        Mockito.when(blockSyncService.getHeaders(startHeight, size)).thenReturn(headers);
        Mockito.when(blockSyncService.validating(Matchers.anyString(), Matchers.anyList())).thenReturn(true);
        Mockito.when(blockSyncService.bftValidating(Matchers.any())).thenReturn(true);

        Mockito.when(blockSyncService.getBlocks(Matchers.anyLong(), Matchers.anyInt()))
            .thenAnswer((Answer<List<Block>>)invocation -> {
                Object[] arguments = invocation.getArguments();
                return mockBlocks((Long)arguments[0], (int)arguments[1]);
            });
        Mockito.when(blockSyncService.validatingBlocks(Matchers.any(), Matchers.any())).thenReturn(true);
        Mockito.when(blockService.compareBlockHeader(Matchers.any(), Matchers.any())).thenReturn(true, false);
        PackContext pack = Mockito.mock(PackContext.class);
        Block block = Mockito.mock(Block.class);
        BlockHeader theader = Mockito.mock(BlockHeader.class);
        Mockito.when(block.getBlockHeader()).thenReturn(theader);
        Mockito.when(pack.getCurrentBlock()).thenReturn(block);
        Mockito.when(packageService.createPackContext(Matchers.any())).thenReturn(pack);
        try {
            syncService.sync(startHeight, size);
        } catch (SlaveException e) {
            assertEquals(e.getCode(), ManagementError.MANAGEMENT_FAILOVER_SYNC_BLOCK_PERSIST_RESULT_INVALID);
        }
    }

    /**
     * Test sync block persisting failed.
     */
    @Test public void testSyncBlockPersistingFailed() {
        long startHeight = currentHeight + 1;
        int size = 100, times = 3, blockStep = 10;
        List<BlockHeader> headers = mockHeaders(startHeight, size);
        Mockito.when(properties.getTryTimes()).thenReturn(times);
        Mockito.when(properties.getBlockStep()).thenReturn(blockStep);
        Mockito.when(blockRepository.getBlockHeader(currentHeight)).thenReturn(header);
        Mockito.when(blockSyncService.getHeaders(startHeight, size)).thenReturn(headers);
        Mockito.when(blockSyncService.validating(Matchers.anyString(), Matchers.anyList())).thenReturn(true);
        Mockito.when(blockSyncService.bftValidating(Matchers.any())).thenReturn(true);

        Mockito.when(blockSyncService.getBlocks(Matchers.anyLong(), Matchers.anyInt()))
            .thenAnswer((Answer<List<Block>>)invocation -> {
                Object[] arguments = invocation.getArguments();
                return mockBlocks((Long)arguments[0], (int)arguments[1]);
            });
        Mockito.when(blockSyncService.validatingBlocks(Matchers.any(), Matchers.any())).thenReturn(true);
        Mockito.when(blockService.compareBlockHeader(Matchers.any(), Matchers.any())).thenReturn(true, true, false);
        PackContext pack = Mockito.mock(PackContext.class);
        Block block = Mockito.mock(Block.class);
        BlockHeader theader = Mockito.mock(BlockHeader.class);
        Mockito.when(block.getBlockHeader()).thenReturn(theader);
        Mockito.when(pack.getCurrentBlock()).thenReturn(block);
        Mockito.when(packageService.createPackContext(Matchers.any())).thenReturn(pack);
        try {
            syncService.sync(startHeight, size);
        } catch (SlaveException e) {
            assertEquals(e.getCode(), ManagementError.MANAGEMENT_FAILOVER_SYNC_BLOCK_PERSIST_RESULT_INVALID);
        }
    }

    /**
     * Test sync.
     */
    @Test public void testSync() {
        int times = 3, headerStep = 20, blockStep = 10;
        long clusterHeight = 100L, cacheMinHeight = 160;
        AtomicLong blockHeight = new AtomicLong(currentHeight);
        AtomicInteger getHeaderTime = new AtomicInteger();
        Mockito.when(blockRepository.getMaxHeight()).thenReturn(blockHeight.longValue()).thenAnswer(invocation -> {
            return blockHeight.addAndGet(getHeaderTime.incrementAndGet() % 2 == 0 ? headerStep : 0);
        });
        Mockito.when(blockSyncService.getClusterHeight(Matchers.anyInt())).thenReturn(clusterHeight);
        Mockito.when(properties.getTryTimes()).thenReturn(times);
        Mockito.when(properties.getBlockStep()).thenReturn(blockStep);
        Mockito.when(properties.getHeaderStep()).thenReturn(headerStep);
        Mockito.when(blockRepository.getBlockHeader(Matchers.anyLong())).thenAnswer(invocation -> {
            Object[] arguments = invocation.getArguments();
            BlockHeader header = Mockito.mock(BlockHeader.class);
            Mockito.when(header.getHeight()).thenReturn((Long)arguments[0]);
            return header;
        });
        Mockito.when(blockSyncService.getHeaders(Matchers.anyLong(), Matchers.anyInt()))
            .thenAnswer((Answer<List<BlockHeader>>)invocation -> {
                Object[] arguments = invocation.getArguments();
                return mockHeaders((Long)arguments[0], (int)arguments[1]);
            });
        Mockito.when(blockSyncService.validating(Matchers.anyString(), Matchers.anyList())).thenReturn(true);
        Mockito.when(blockSyncService.bftValidating(Matchers.any())).thenReturn(true);

        Mockito.when(blockSyncService.getBlocks(Matchers.anyLong(), Matchers.anyInt()))
            .thenAnswer((Answer<List<Block>>)invocation -> {
                Object[] arguments = invocation.getArguments();
                return mockBlocks((Long)arguments[0], (int)arguments[1]);
            });
        Mockito.when(blockSyncService.validatingBlocks(Matchers.any(), Matchers.any())).thenReturn(true);
        Mockito.when(blockService.compareBlockHeader(Matchers.any(), Matchers.any())).thenReturn(true);
        PackContext pack = Mockito.mock(PackContext.class);
        Block block = Mockito.mock(Block.class);
        BlockHeader theader = Mockito.mock(BlockHeader.class);
        Mockito.when(block.getBlockHeader()).thenReturn(theader);
        Mockito.when(pack.getCurrentBlock()).thenReturn(block);
        Mockito.when(packageService.createPackContext(Matchers.any())).thenReturn(pack);
        syncService.autoSync();
    }

    private List<BlockHeader> mockHeaders(long startHeight, int size) {
        List<BlockHeader> headers = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            BlockHeader mock = Mockito.mock(BlockHeader.class);
            Mockito.when(mock.getHeight()).thenReturn(startHeight + i);
            headers.add(mock);
        }
        return headers;
    }

    private List<Block> mockBlocks(long startHeight, int size) {
        List<Block> headers = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Block block = Mockito.mock(Block.class);
            BlockHeader header = Mockito.mock(BlockHeader.class);
            Mockito.when(header.getHeight()).thenReturn(startHeight + i);
            Mockito.when(block.getBlockHeader()).thenReturn(header);
            headers.add(block);
        }
        return headers;
    }
}