/*
 * Copyright (c) 2013-2017, suimi
 */
package com.higgschain.trust.consensus.core.filter;

import com.higgschain.trust.consensus.core.ConsensusCommit;
import com.higgschain.trust.consensus.core.command.AbstractConsensusCommand;

/**
 * The interface Command filter chain.
 */
public interface CommandFilterChain {

    /**
     * Do filter.
     *
     * @param commit the commit
     */
    void doFilter(ConsensusCommit<? extends AbstractConsensusCommand> commit);
}
