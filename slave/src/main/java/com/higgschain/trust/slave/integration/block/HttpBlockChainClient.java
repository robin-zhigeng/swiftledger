package com.higgschain.trust.slave.integration.block;

import com.higgschain.trust.config.view.IClusterViewManager;
import com.higgschain.trust.network.HttpClient;
import com.higgschain.trust.network.NetworkManage;
import com.higgschain.trust.common.vo.RespData;
import com.higgschain.trust.slave.api.vo.TransactionVO;
import com.higgschain.trust.slave.model.bo.Block;
import com.higgschain.trust.slave.model.bo.BlockHeader;
import com.higgschain.trust.slave.model.bo.SignedTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.List;

/**
 * The type Http block chain client.
 *
 * @author duhongming
 * @date 2018 /9/14
 */
@ConditionalOnProperty(name = "network.rpc", havingValue = "http")
@Component
@Slf4j
public class HttpBlockChainClient implements BlockChainClient {

    private static Type respDataListType;

    @Autowired
    private IClusterViewManager viewManager;

    static {
        try {
            respDataListType = (new Object() {
                public RespData<List<TransactionVO>> respDataList;
            }).getClass().getField("respDataList").getGenericType();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    /**
     * Instantiates a new Http block chain client.
     */
    public HttpBlockChainClient() {
        log.info("Use HttpBlockChainClient");
    }

    @Override
    public List<BlockHeader> getBlockHeaders(String nodeNameReg, long startHeight, int size) {
        HttpClient httpClient = NetworkManage.getInstance().httpClient();
        String url = String.format("/block/header/get?startHeight=%d&size=%d", startHeight, size);
        List<String> names = viewManager.getCurrentView().getNodeNames();
        String nodeName = httpClient.getRandomPeer(names).getNodeName();
        return httpClient.getList(nodeName, url, BlockHeader.class);
    }

    @Override
    public List<BlockHeader> getBlockHeadersFromNode(String nodeName, long startHeight, int size) {
        HttpClient httpClient = NetworkManage.getInstance().httpClient();
        String url = String.format("/block/header/get?startHeight=%d&size=%d", startHeight, size);
        return httpClient.getList(nodeName, url, BlockHeader.class);
    }

    @Override
    public List<Block> getBlocks(String nodeNameReg, long startHeight, int size) {
        HttpClient httpClient = NetworkManage.getInstance().httpClient();
        String url = String.format("/block/get?startHeight=%d&size=%d", startHeight, size);
        List<String> names = viewManager.getCurrentView().getNodeNames();
        String nodeName = httpClient.getRandomPeer(names).getNodeName();
        return httpClient.getList(nodeName, url, Block.class);
    }

    @Override
    public List<Block> getBlocksFromNode(String nodeName, long startHeight, int size) {
        HttpClient httpClient = NetworkManage.getInstance().httpClient();
        String url = String.format("/block/get?startHeight=%d&size=%d", startHeight, size);
        return httpClient.getList(nodeName, url, Block.class);
    }

    @Override
    public RespData<List<TransactionVO>> submitToMaster(String nodeName, List<SignedTransaction> transactions) {
        String url = "/transaction/master/submit";
        HttpClient httpClient = NetworkManage.getInstance().httpClient();
        return httpClient.postJson(nodeName, url, transactions, respDataListType);
    }
}
