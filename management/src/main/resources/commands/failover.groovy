package commands


import com.higgschain.trust.config.view.IClusterViewManager
import com.higgschain.trust.consensus.config.NodeState
import com.higgschain.trust.consensus.config.NodeStateEnum
import com.higgschain.trust.management.failover.scheduler.FailoverSchedule
import com.higgschain.trust.management.failover.service.SelfCheckingService
import com.higgschain.trust.management.failover.service.SyncService
import com.higgschain.trust.slave.core.service.block.BlockService
import lombok.extern.slf4j.Slf4j
import org.apache.commons.lang3.StringUtils
import org.crsh.cli.*
import org.crsh.command.InvocationContext
import org.springframework.beans.factory.BeanFactory

/*
 * Copyright (c) 2013-2017, suimi
 */

/**
 *  The type Failover.
 */
@Slf4j
@Usage("get the node info")
class failover {

    @Usage('auto sync the batch blocks, get the blocks from other node and validate block by raft/b2p channel and execute it, this option will auto change the node state.')
    @Command
    def autoSync(InvocationContext context) {
        BeanFactory beans = context.attributes['spring.beanfactory']
        def syncService = beans.getBean(SyncService.class)
        def nodeState = beans.getBean(NodeState.class)
        if (!nodeState.isState(NodeStateEnum.AutoSync)) {
            out.println("Node state is $nodeState.state, not allowed auto sync block")
            return
        }
        def selfCheckService = beans.getBean(SelfCheckingService.class)
        def result = selfCheckService.selfCheck(3)
        if (!result) {
            out.println("self check failed, please check the current block")
            return
        }
        syncService.asyncAutoSync()
        def blockService = beans.getBean(BlockService.class)
        def height = blockService.getMaxHeight().toString()
        out.println("auto sync blocks successful, current height:$height")
    }


    @Usage('sync batch blocks, get the blocks from other node and validate block by raft/b2p channel and execute it')
    @Command
    def batch(InvocationContext context,
              @Required @Argument String startHeight,
              @Required @Argument int size, @Option(names = ["f", "from"]) String fromNode) {
        BeanFactory beans = context.attributes['spring.beanfactory']
        def nodeState = beans.getBean(NodeState.class)
        def blockService = beans.getBean(BlockService.class)
        def syncService = beans.getBean(SyncService.class)
        if (!nodeState.isState(NodeStateEnum.ArtificialSync)) {
            out.println("Node state is $nodeState.state, not allowed sync block")
            return
        }

        if (StringUtils.isBlank(fromNode)) {
            syncService.sync(Long.parseLong(startHeight), size)
            def height = blockService.getMaxHeight().toString()
            out.println("sync blocks successful, current height:$height")
        } else {
            def viewManager = beans.getBean(IClusterViewManager.class)
            def currentView = viewManager.getCurrentView();
            if (!currentView.nodeNames.contains(fromNode)) {
                out.println("The from node: $fromNode not exist")
                return
            }
            syncService.sync(Long.parseLong(startHeight), size, fromNode)
            def height = blockService.getMaxHeight().toString()
            out.println("sync blocks from $fromNode successful, current height:$height")
        }
    }

    @Usage('failover single block, which will get the block from other node, transfer to package, validating/persisting the package transaction and validate the result with received consensus validating/persisting block header')
    @Command
    def single(InvocationContext context, @Required @Argument String height) {
        BeanFactory beans = context.attributes['spring.beanfactory']
        def nodeState = beans.getBean(NodeState.class)
        if (!nodeState.isState(NodeStateEnum.ArtificialSync)) {
            out.println("Node state is $nodeState.state, not allowed sync block")
            return
        }
        def failoverSchedule = beans.getBean(FailoverSchedule.class)
        return failoverSchedule.failover(Long.parseLong(height))
    }

    @Usage('check the current block of node')
    @Command
    def selfCheck(InvocationContext context) {
        BeanFactory beans = context.attributes['spring.beanfactory']
        def selfCheckService = beans.getBean(SelfCheckingService.class)
        def result = selfCheckService.selfCheck(1)
        out.println("Self check result: $result")
    }

    @Usage('sync the genesis block')
    @Command
    def genesis(InvocationContext context, @Option(names = ["f", "from"]) String fromNode) {
        BeanFactory beans = context.attributes['spring.beanfactory']
        def nodeState = beans.getBean(NodeState.class)
        def blockService = beans.getBean(BlockService.class)
        def syncService = beans.getBean(SyncService.class)
        if (!nodeState.isState(NodeStateEnum.ArtificialSync)) {
            out.println("Node state is $nodeState.state, not allowed sync block")
            return
        }

        if (StringUtils.isBlank(fromNode)) {
            syncService.syncGenesis()
            def height = blockService.getMaxHeight().toString()
            out.println("sync blocks successful, current height:$height")
        } else {
            def viewManager = beans.getBean(IClusterViewManager.class)
            def currentView = viewManager.getCurrentView()
            if (!currentView.nodeNames.contains(fromNode)) {
                out.println("The from node: $fromNode not exist")
                return
            }
            syncService.syncGenesis(fromNode)
            def height = blockService.getMaxHeight().toString()
            out.println("sync blocks from $fromNode successful, current height:$height")
        }
    }
}
