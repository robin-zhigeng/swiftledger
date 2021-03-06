package com.higgschain.trust.management.failover.service;

import com.higgschain.trust.consensus.config.NodeProperties;
import com.higgschain.trust.consensus.config.NodeState;
import com.higgschain.trust.management.exception.FailoverExecption;
import com.higgschain.trust.management.exception.ManagementError;
import com.higgschain.trust.slave.core.repository.BlockRepository;
import com.higgschain.trust.slave.core.service.block.BlockService;
import com.higgschain.trust.slave.model.bo.Block;
import com.higgschain.trust.slave.model.bo.BlockHeader;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * The type Self checking service test.
 */
@RunWith(PowerMockRunner.class) public class SelfCheckingServiceTest {

    /**
     * The Self checking service.
     */
    @Autowired @InjectMocks
    SelfCheckingService selfCheckingService;

    /**
     * The Node state.
     */
    @Mock NodeState nodeState;
    /**
     * The Block sync service.
     */
    @Mock
    BlockSyncService blockSyncService;
    /**
     * The Block service.
     */
    @Mock BlockService blockService;
    /**
     * The Block repository.
     */
    @Mock BlockRepository blockRepository;

    /**
     * The Block.
     */
    @Mock Block block;
    /**
     * The Header.
     */
    @Mock BlockHeader header;
    /**
     * The Properties.
     */
    @Mock NodeProperties properties;

    /**
     * Before.
     */
    @BeforeClass public void before() {
        MockitoAnnotations.initMocks(this);
        long height = 1L;
        Mockito.when(blockService.getMaxHeight()).thenReturn(height);
        Mockito.when(block.getBlockHeader()).thenReturn(header);
        Mockito.when(blockRepository.getBlock(height)).thenReturn(block);
        Mockito.when(properties.getStartupRetryTime()).thenReturn(1);
    }

    /**
     * Before method.
     */
    @BeforeMethod public void beforeMethod() {
        Mockito.reset(nodeState);
    }

    /**
     * Test check true not master.
     */
    @Test public void testCheckTrueNotMaster() {

        Mockito.when(nodeState.isMaster()).thenReturn(false);
        Mockito.when(blockSyncService.validating(block)).thenReturn(true);
        Mockito.when(blockSyncService.bftValidating(header)).thenReturn(true);

        selfCheckingService.autoCheck();
    }

    /**
     * Test check true not master 1.
     */
    @Test public void testCheckTrueNotMaster1() {
        Mockito.when(nodeState.isMaster()).thenReturn(false);

        Mockito.when(blockSyncService.validating(block)).thenReturn(true);
        Mockito.when(blockSyncService.bftValidating(header)).thenReturn(null, true);
        Mockito.when(properties.getStartupRetryTime()).thenReturn(2);

        selfCheckingService.autoCheck();
    }

    /**
     * Test check false not master.
     */
    @Test public void testCheckFalseNotMaster() {
        Mockito.when(nodeState.isMaster()).thenReturn(false);

        Mockito.when(blockSyncService.validating(block)).thenReturn(false);
        try{
            selfCheckingService.autoCheck();
        }catch (FailoverExecption e) {
            assertEquals(e.getCode(), ManagementError.MANAGEMENT_STARTUP_SELF_CHECK_FAILED);
        }
    }

    /**
     * Test check false not master 2.
     */
    @Test public void testCheckFalseNotMaster2() {
        Mockito.when(nodeState.isMaster()).thenReturn(false);

        Mockito.when(blockSyncService.validating(block)).thenReturn(true);
        Mockito.when(blockSyncService.bftValidating(header)).thenReturn(null, false);

        try{
            selfCheckingService.autoCheck();
        }catch (FailoverExecption e) {
            assertEquals(e.getCode(), ManagementError.MANAGEMENT_STARTUP_SELF_CHECK_FAILED);
        }
    }

    /**
     * Test check bft valid out.
     */
    //    @Test
    public void testCheckBftValidOut() {
        Mockito.when(nodeState.isMaster()).thenReturn(false);

        Mockito.when(blockSyncService.validating(block)).thenReturn(true);
        Mockito.when(blockSyncService.bftValidating(header)).thenReturn(null);

        try{
            selfCheckingService.autoCheck();
        }catch (FailoverExecption e) {
            assertEquals(e.getCode(), ManagementError.MANAGEMENT_STARTUP_SELF_CHECK_FAILED);
        }
    }
}