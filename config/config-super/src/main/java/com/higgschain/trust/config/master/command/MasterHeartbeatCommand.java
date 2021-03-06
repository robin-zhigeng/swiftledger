/*
 * Copyright (c) 2013-2017, suimi
 */
package com.higgschain.trust.config.master.command;

import com.higgschain.trust.consensus.core.command.AbstractConsensusCommand;
import com.higgschain.trust.consensus.core.command.SignatureCommand;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * The type Master heartbeat command.
 *
 * @author suimi
 * @date 2018 /6/5
 */
@ToString(callSuper = true, exclude = {"sign"}) public class MasterHeartbeatCommand
    extends AbstractConsensusCommand<Long> implements SignatureCommand {

    private static final long serialVersionUID = 4579567364750332581L;

    /**
     * the cluster view number
     */
    @Getter private long view;

    /**
     * master name
     */
    private String masterName;

    /**
     * signature
     */
    @Setter private String sign;

    /**
     * Instantiates a new Master heartbeat command.
     *
     * @param term       the term
     * @param view       the view
     * @param masterName the master name
     */
    public MasterHeartbeatCommand(long term, long view, String masterName) {
        super(term);
        this.view = view;
        this.masterName = masterName;
    }

    @Override public String getNodeName() {
        return masterName;
    }

    @Override public String getSignValue() {
        return masterName + get() + view;
    }

    @Override public String getSignature() {
        return sign;
    }
}
