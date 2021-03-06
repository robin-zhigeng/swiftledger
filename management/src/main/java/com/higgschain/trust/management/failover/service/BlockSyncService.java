package com.higgschain.trust.management.failover.service;

import com.higgschain.trust.consensus.config.NodeState;
import com.higgschain.trust.slave.core.service.block.BlockService;
import com.higgschain.trust.slave.core.service.block.hash.TxRootHashBuilder;
import com.higgschain.trust.slave.core.service.consensus.cluster.IClusterService;
import com.higgschain.trust.slave.integration.block.BlockChainClient;
import com.higgschain.trust.slave.model.bo.Block;
import com.higgschain.trust.slave.model.bo.BlockHeader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The type Block sync service.
 */
@Service @Slf4j public class BlockSyncService {
    @Autowired private BlockService blockService;
    @Autowired private BlockChainClient blockChainClient;
    @Autowired private TxRootHashBuilder txRootHashBuilder;
    @Autowired private NodeState nodeState;
    @Autowired private IClusterService clusterService;

    /**
     * 从startHeight开始获取size个blockheader
     *
     * @param startHeight 开始高度
     * @param size        数量
     * @return blockheader列表 headers
     */
    public List<BlockHeader> getHeaders(long startHeight, int size) {
        return blockChainClient.getBlockHeaders(nodeState.notMeNodeNameReg(), startHeight, size);
    }

    /**
     * 从startHeight开始获取size个blockheader
     *
     * @param startHeight 开始高度
     * @param size        数量
     * @param nodeName    the node name
     * @return blockheader列表 headers from node
     */
    public List<BlockHeader> getHeadersFromNode(long startHeight, int size, String nodeName) {
        return blockChainClient.getBlockHeadersFromNode(nodeName, startHeight, size);
    }

    /**
     * 从startHeight开始获取size个block
     *
     * @param startHeight 开始高度
     * @param size        数量
     * @return blockheader列表 blocks
     */
    public List<Block> getBlocks(long startHeight, int size) {
        return blockChainClient.getBlocks(nodeState.notMeNodeNameReg(), startHeight, size);
    }

    /**
     * 从startHeight开始获取size个block
     *
     * @param startHeight 开始高度
     * @param size        数量
     * @param nodeName    the node name
     * @return blockheader列表 blocks from node
     */
    public List<Block> getBlocksFromNode(long startHeight, int size, String nodeName) {
        return blockChainClient.getBlocksFromNode(nodeName, startHeight, size);
    }

    /**
     * bft验证blockheader
     *
     * @param blockHeader blockheader
     * @return 协议验证结果, if timeout will return null
     */
    public Boolean bftValidating(BlockHeader blockHeader) {
        Boolean aBoolean = clusterService.validatingHeader(blockHeader);
        if (log.isDebugEnabled()) {
            log.debug("the blockheader:{} validated result by bft :{}", blockHeader.getHeight(), aBoolean);
        }
        return aBoolean;
    }

    /**
     * get the cluster height
     *
     * @param size the size
     * @return cluster height
     */
    public Long getClusterHeight(int size) {
        Long clusterHeight = clusterService.getClusterHeight(size);
        log.info("get the cluster height:{}", clusterHeight);
        return clusterHeight;
    }

    /**
     * 获取集群安全高度
     *
     * @param tryTimes the try times
     * @return 高度 safe height
     */
    public Long getSafeHeight(int tryTimes) {
        Long safeHeight;
        int times = 0;
        do {
            safeHeight = clusterService.getSafeHeight();
            if (safeHeight != null) {
                break;
            }
            try {
                Thread.sleep(3 * 1000);
            } catch (InterruptedException e) {
                log.warn("get safe height thread interrupted.", e);
            }
        } while (++times < tryTimes);
        log.info("get the safe height:{}", safeHeight);
        return safeHeight;
    }

    /**
     * 本地验证block transactions hash
     *
     * @param previousHash previous block hash
     * @param blocks       block列表
     * @return 验证结果 boolean
     */
    public boolean validatingBlocks(String previousHash, List<Block> blocks) {
        if (blocks == null || blocks.isEmpty() || StringUtils.isBlank(previousHash)) {
            return false;
        }
        String preHash = previousHash;
        for (Block block : blocks) {
            BlockHeader blockHeader = block.getBlockHeader();
            if (validating(preHash, block)) {
                preHash = blockHeader.getBlockHash();
            } else {
                log.warn("validating block chain failed at {}:{}", blockHeader.getHeight(), blockHeader.getBlockHash());
                return false;
            }
        }
        return true;
    }

    /**
     * 本地验证block transactions hash
     *
     * @param previousHash previous block hash
     * @param block        block
     * @return 验证结果 boolean
     */
    public boolean validating(String previousHash, Block block) {
        if (block == null || StringUtils.isBlank(previousHash)) {
            return false;
        }
        BlockHeader blockHeader = block.getBlockHeader();
        if (!previousHash.equalsIgnoreCase(blockHeader.getPreviousHash())) {
            if (log.isTraceEnabled()) {
                log.trace("validating block : {} previous block hash failed.", blockHeader.getHeight());
            }
            return false;
        }
        return validating(block);
    }

    /**
     * 本地验证block transactions hash
     *
     * @param block block
     * @return 验证结果 boolean
     */
    public boolean validating(Block block) {
        if (block == null) {
            return false;
        }
        BlockHeader blockHeader = block.getBlockHeader();
        if (!validating(blockHeader)) {
            return false;
        }
        String txRootHash = txRootHashBuilder.buildTxs(block.getSignedTxList());

        if (!blockHeader.getStateRootHash().getTxRootHash().equalsIgnoreCase(txRootHash)) {
            if (log.isTraceEnabled()) {
                log.trace("validating block: {} tx root hash failed.", blockHeader.getHeight());
            }
            return false;
        }
        return true;
    }

    /**
     * 本地验证区块链hash
     *
     * @param blockHeaders 要验证的blockheader列表
     * @return 验证结果 boolean
     */
    public boolean validating(List<BlockHeader> blockHeaders) {
        if (blockHeaders == null || blockHeaders.isEmpty()) {
            return false;
        }
        String preHash = blockHeaders.get(0).getPreviousHash();
        return validating(preHash, blockHeaders);
    }

    /**
     * 本地验证区块链hash
     *
     * @param previousHash 前一区块hash
     * @param blockHeaders 要验证的blockheader列表
     * @return 验证结果 boolean
     */
    public boolean validating(String previousHash, List<BlockHeader> blockHeaders) {
        if (blockHeaders == null || blockHeaders.isEmpty() || StringUtils.isBlank(previousHash)) {
            return false;
        }
        String preHash = previousHash;
        for (BlockHeader blockHeader : blockHeaders) {
            if (validating(preHash, blockHeader)) {
                preHash = blockHeader.getBlockHash();
            } else {
                log.warn("validating block chain headers failed at {}:{}", blockHeader.getHeight(),
                    blockHeader.getBlockHash());
                return false;
            }
        }
        return true;
    }

    /**
     * validating block header with previous hash
     *
     * @param previousHash the previous hash
     * @param blockHeader  the block header
     * @return boolean
     */
    public boolean validating(String previousHash, BlockHeader blockHeader) {
        if (blockHeader == null || StringUtils.isBlank(previousHash)) {
            return false;
        }
        boolean validated = previousHash.equalsIgnoreCase(blockHeader.getPreviousHash()) && validating(blockHeader);
        if (!validated && log.isDebugEnabled()) {
            log.debug("validating block chain header : {} hash failed.", blockHeader.getHeight());
        }
        return validated;
    }

    /**
     * validating block header
     *
     * @param blockHeader the block header
     * @return boolean
     */
    public boolean validating(BlockHeader blockHeader) {
        if (blockHeader == null) {
            return false;
        }
        if (blockHeader.getStateRootHash() == null || StringUtils
            .isBlank(blockHeader.getStateRootHash().getTxRootHash())) {
            return false;
        }
        String currentHash = blockService.buildBlockHash(blockHeader);
        boolean validated = currentHash.equalsIgnoreCase(blockHeader.getBlockHash());
        if (!validated && log.isDebugEnabled()) {
            log.debug("validating block header : {} hash failed.", blockHeader.getHeight());
        }
        return validated;
    }

}


