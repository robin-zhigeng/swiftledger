package commands

import com.higgschain.trust.slave.api.BlockChainService
import com.higgschain.trust.slave.core.service.block.BlockService
import com.higgschain.trust.slave.model.bo.Block
import com.higgschain.trust.slave.model.bo.BlockHeader
import com.higgschain.trust.slave.model.bo.CoreTransaction
import lombok.extern.slf4j.Slf4j
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.time.DateFormatUtils
import org.crsh.cli.Command
import org.crsh.cli.Option
import org.crsh.cli.Usage
import org.crsh.command.InvocationContext
import org.springframework.beans.factory.BeanFactory

/*
 * suimi
 */

/**
 *  The type Block.
 */
@Slf4j
@Usage("about block info operation")
class block {

    @Usage('get the current block height')
    @Command
    def height(InvocationContext context) {
        BeanFactory beans = context.attributes['spring.beanfactory']
        def blockService = beans.getBean(BlockService.class)
        height = blockService.getMaxHeight().toString()
        out.println("The block height is $height")
    }

    @Usage('get the block info')
    @Command
    def info(InvocationContext context, @Usage('the block height') @Option(names = ["e", "height"]) String heightStr) {
        BeanFactory beans = context.attributes['spring.beanfactory']
        def blockService = beans.getBean(BlockService.class)
        long height
        if (StringUtils.isBlank(heightStr)) {
            height = blockService.getMaxHeight();
        } else {
            height = Long.parseLong(heightStr)
        }
        def block = blockService.queryBlock(height)
        if (block == null) {
            out.println("The block height is invalid")
        } else {
            printBlock(context, block)
        }
    }

    def printBlock(InvocationContext context, Block block) {
        def header = block.getBlockHeader()
        printBlockHeader(context, header,block.getSignedTxList().size())
        block.getSignedTxList().forEach({ tx -> printTx(context, tx.getCoreTx()) })
    }

    def printBlockHeader(InvocationContext context, BlockHeader header,int number) {
        context.provide(Header: "Height", "": header.height)
        context.provide("Name": "Version", "Value": header.getVersion())
        context.provide("Name": "PreviousHash", "Value": header.getPreviousHash())
        context.provide("Name": "BlockHash", "Value": header.blockHash)
        context.provide("Name": "BlockTime", "Value": DateFormatUtils.format(header.blockTime, "yyyy-MM-dd HH:mm:ss.SSS"))
        context.provide("Name": "BlockTxNum", "Value": number)
        context.provide("Name": "TotalTxNum", "Value": header.totalTxNum)
        out.println("")
    }

    def printTx(InvocationContext context, CoreTransaction ctx) {
        BeanFactory beans = context.attributes['spring.beanfactory']
        def blockChainService = beans.getBean(BlockChainService.class)
        context.provide("\tTxInfo": "\tTxID", "": ctx.txId)
        context.provide(["Name": "\tPolicyId", "Value": ctx.policyId])
        context.provide(["Name": "\tPolicyType", "Value": blockChainService.getPolicyNameById(ctx.getPolicyId())])
        context.provide(["Name": "\tSender", "Value": ctx.sender])
        context.provide(["Name": "\tSendTime", "Value": DateFormatUtils.format(ctx.sendTime, "yyyy-MM-dd HH:mm:ss.SSS")])
        out.println("")
    }

}
