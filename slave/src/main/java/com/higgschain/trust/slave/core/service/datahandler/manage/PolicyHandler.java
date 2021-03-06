package com.higgschain.trust.slave.core.service.datahandler.manage;

import com.higgschain.trust.slave.model.bo.manage.Policy;
import com.higgschain.trust.slave.model.bo.manage.RegisterPolicy;

/**
 * The interface Policy handler.
 *
 * @author tangfashuang
 * @date 2018 /04/17 19:26
 * @desc policy handler interface
 */
public interface PolicyHandler {

    /**
     * get policy by id
     *
     * @param policyId the policy id
     * @return policy
     */
    Policy getPolicy(String policyId);

    /**
     * register policy
     *
     * @param registerPolicy the register policy
     */
    void registerPolicy(RegisterPolicy registerPolicy);
}
