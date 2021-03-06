package com.higgschain.trust.consensus.p2pvalid.core.service;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.higgschain.trust.common.utils.BeanConvertor;
import com.higgschain.trust.config.crypto.CryptoUtil;
import com.higgschain.trust.config.view.ClusterView;
import com.higgschain.trust.config.view.IClusterViewManager;
import com.higgschain.trust.consensus.config.NodeState;
import com.higgschain.trust.consensus.config.NodeStateEnum;
import com.higgschain.trust.consensus.p2pvalid.core.P2PValidCommit;
import com.higgschain.trust.consensus.p2pvalid.core.ValidCommandWrap;
import com.higgschain.trust.consensus.p2pvalid.core.ValidConsensus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * The type P 2 p receive service.
 *
 * @author liuyu
 * @description
 * @date 2018 -08-20
 */
@Component @Slf4j public class P2PReceiveService implements InitializingBean {
    @Autowired private NodeState nodeState;
    @Autowired private IClusterViewManager viewManager;
    @Autowired private ValidConsensus validConsensus;
    @Autowired private ThreadPoolTaskExecutor p2pReceiveExecutor;
    /**
     * store max received command number
     */
    @Value("${p2p.receive.maxCommandNum:5000}") int maxCommandNum;
    /**
     * The Retry num.
     */
    @Value("${p2p.receive.retryNum:100}") int retryNum;

    /**
     * store received command
     */
    private ConcurrentLinkedHashMap<String, ConcurrentHashMap<String, ValidCommandWrap>> receivedCommand = null;

    /**
     * store executed commands
     */
    private ConcurrentLinkedHashMap<String, Integer> executedCommand = null;

    @Override public void afterPropertiesSet() throws Exception {
        receivedCommand = new ConcurrentLinkedHashMap.Builder<String, ConcurrentHashMap<String, ValidCommandWrap>>()
            .maximumWeightedCapacity(maxCommandNum).listener((key, value) -> {
                if (log.isDebugEnabled()) {
                    log.debug("[receivedCommand]Evicted key:{},value:{}", key, value);
                }
            }).build();

        executedCommand = new ConcurrentLinkedHashMap.Builder<String, Integer>().
            maximumWeightedCapacity(maxCommandNum).listener((key, value) -> {
            if (log.isDebugEnabled()) {
                log.debug("[executedCommand]Evicted key:{},value:{}", key, value);
            }
        }).build();

    }

    /**
     * process received command
     *
     * @param validCommandWrap the valid command wrap
     */
    public void receive(ValidCommandWrap validCommandWrap) {
        if (log.isDebugEnabled()) {
            log.debug("p2p.receive fromNode:{},messageDigest:{}", validCommandWrap.getFromNode(),
                validCommandWrap.getValidCommand().getMessageDigestHash());
        }
        if (!nodeState.isState(NodeStateEnum.Running)) {
            throw new RuntimeException(String.format("the node state is not running, please try again latter"));
        }
        String messageDigest = validCommandWrap.getValidCommand().getMessageDigestHash();
        ClusterView view = viewManager.getView(validCommandWrap.getValidCommand().getView());
        if (view == null || StringUtils.isBlank(view.getPubKey(validCommandWrap.getFromNode()))) {
            throw new RuntimeException(String.format("the view not exist or not have pubkey"));
        }
        String pubKey = view.getPubKey(validCommandWrap.getFromNode());
        if (!CryptoUtil.getProtocolCrypto().verify(messageDigest, validCommandWrap.getSign(), pubKey)) {
            throw new RuntimeException(String
                .format("check sign failed for node %s, validCommandWrap %s, pubKey %s", validCommandWrap.getFromNode(),
                    validCommandWrap, pubKey));
        }
        String fromNode = validCommandWrap.getFromNode();
        ConcurrentHashMap<String, ValidCommandWrap> _new = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, ValidCommandWrap> _old = receivedCommand.putIfAbsent(messageDigest, _new);
        //add command to memory for first
        if (_old != null) {
            _new = _old;
        }
        _new.put(fromNode, validCommandWrap);
        //check threshold
        int applyThreshold = Math.min(view.getAppliedQuorum(), view.getNodeNames().size());
        if (_new.size() < applyThreshold) {
            if (log.isDebugEnabled()) {
                log.debug("command.size is less than applyThreshold:{}", applyThreshold);
            }
            return;
        }
        Integer v = executedCommand.putIfAbsent(messageDigest, 0);
        if (v != null) {
            log.warn("command is already executed");
            return;
        }
        ValidCommandWrap o = BeanConvertor.convertBean(validCommandWrap, ValidCommandWrap.class);
        p2pReceiveExecutor.execute(() -> {
            P2PValidCommit validCommit = new P2PValidCommit(o.getValidCommand());
            int num = 0;
            do {
                try {
                    validConsensus.getValidExecutor().execute(validCommit);
                    if (validCommit.isClosed()) {
                        if (log.isDebugEnabled()) {
                            log.debug("execute validCommit:{} is success", validCommit);
                        }
                        break;
                    }
                } catch (Throwable t) {
                    log.error("execute validCommit:{} has error:{}", validCommit, t);
                }
                try {
                    Thread.sleep(100L + 500 * num);
                } catch (InterruptedException e) {
                    log.error("has InterruptedException", e);
                }
            } while (++num < retryNum);
        });
    }
}
