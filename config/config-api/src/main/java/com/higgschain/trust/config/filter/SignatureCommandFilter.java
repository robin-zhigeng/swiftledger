/*
 * Copyright (c) 2013-2017, suimi
 */
package com.higgschain.trust.config.filter;

import com.higgschain.trust.config.crypto.CryptoUtil;
import com.higgschain.trust.config.view.ClusterView;
import com.higgschain.trust.config.view.IClusterViewManager;
import com.higgschain.trust.consensus.core.ConsensusCommit;
import com.higgschain.trust.consensus.core.command.AbstractConsensusCommand;
import com.higgschain.trust.consensus.core.command.SignatureCommand;
import com.higgschain.trust.consensus.core.filter.CommandFilter;
import com.higgschain.trust.consensus.core.filter.CommandFilterChain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * The type Signature command filter.
 *
 * @author suimi
 * @date 2018 /6/1
 */
@Order(1) @Component @Slf4j public class SignatureCommandFilter implements CommandFilter {

    @Autowired private IClusterViewManager viewManager;

    @Override
    public void doFilter(ConsensusCommit<? extends AbstractConsensusCommand> commit, CommandFilterChain chain) {
        if (commit.operation() instanceof SignatureCommand) {
            SignatureCommand command = (SignatureCommand)commit.operation();
            String nodeName = command.getNodeName();
            ClusterView view = viewManager.getView(command.getView());
            String publicKey = view.getPubKey(nodeName);
            boolean verify =
                CryptoUtil.getProtocolCrypto().verify(command.getSignValue(), command.getSignature(), publicKey);
            if (log.isDebugEnabled()) {
                log.debug("command sign verify result:{}", verify);
            }
            if (!verify) {
                log.warn("command sign verify failed, node:{}", nodeName);
                log.debug("command sign info: node:{}, pubkey:{}, signValue:{}", nodeName, publicKey, command.getSignValue());
                commit.close();
                return;
            }
        }
        chain.doFilter(commit);
    }
}
