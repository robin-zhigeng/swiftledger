package com.higgschain.trust.consensus.p2pvalid.api;

import com.higgschain.trust.config.view.IClusterViewManager;
import com.higgschain.trust.consensus.p2pvalid.core.ResponseCommand;
import com.higgschain.trust.consensus.p2pvalid.core.ValidCommandWrap;
import com.higgschain.trust.consensus.p2pvalid.core.ValidResponseWrap;
import com.higgschain.trust.network.HttpClient;
import com.higgschain.trust.network.NetworkManage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The type Http p 2 p consensus client.
 *
 * @author duhongming
 * @date 2018 /9/13
 */
@ConditionalOnProperty(name = "network.rpc", havingValue = "http")
@Component
@Slf4j
public class HttpP2pConsensusClient implements P2pConsensusClient {

    private static final String RECEIVE_COMMAND_URL = "/consensus/p2p/receive_command";
    private static final String RECEIVE_COMMAND_SYNC_URL = "/consensus/p2p/receive_command_sync";

    @Autowired
    private IClusterViewManager viewManager;

    /**
     * Instantiates a new Http p 2 p consensus client.
     */
    public HttpP2pConsensusClient() {
        log.info("Use HttpP2pConsensusClient");
    }

    @Override
    public ValidResponseWrap<ResponseCommand> send(String nodeName, ValidCommandWrap validCommandWrap) {
        return NetworkManage.getInstance().httpClient().postJson(nodeName, RECEIVE_COMMAND_URL, validCommandWrap, ValidResponseWrap.class);
    }

    @Override
    public ValidResponseWrap<ResponseCommand> syncSend(String nodeName, ValidCommandWrap validCommandWrap) {
        return NetworkManage.getInstance().httpClient().postJson(nodeName, RECEIVE_COMMAND_SYNC_URL, validCommandWrap, ValidResponseWrap.class);
    }

    @Override
    public ValidResponseWrap<ResponseCommand> syncSendFeign(String nodeNameReg, ValidCommandWrap validCommandWrap) {
        final HttpClient httpClient = NetworkManage.getInstance().httpClient();
        List<String> names = viewManager.getCurrentView().getNodeNames();
        String nodeName = httpClient.getRandomPeer(names).getNodeName();
        return httpClient.postJson(nodeName, RECEIVE_COMMAND_SYNC_URL, validCommandWrap, ValidResponseWrap.class);
    }
}
