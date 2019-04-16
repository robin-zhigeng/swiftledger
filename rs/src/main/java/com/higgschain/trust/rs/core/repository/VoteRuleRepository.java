package com.higgschain.trust.rs.core.repository;

import com.higgschain.trust.rs.common.config.RsConfig;
import com.higgschain.trust.rs.core.api.enums.CallbackTypeEnum;
import com.higgschain.trust.rs.core.bo.VoteRule;
import com.higgschain.trust.rs.core.dao.VoteRuleDao;
import com.higgschain.trust.rs.core.dao.VoteRuleJDBCDao;
import com.higgschain.trust.rs.core.dao.po.VoteRulePO;
import com.higgschain.trust.rs.core.dao.rocks.VoteRuleRocksDao;
import com.higgschain.trust.slave.api.enums.manage.VotePatternEnum;
import com.higgschain.trust.slave.common.enums.SlaveErrorEnum;
import com.higgschain.trust.slave.common.exception.SlaveException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * The type Vote rule repository.
 *
 * @author liuyu
 * @description
 * @date 2018 -06-06
 */
@Slf4j @Repository public class VoteRuleRepository {
    @Autowired private RsConfig rsConfig;
    @Autowired private VoteRuleDao voteRuleDao;
    @Autowired private VoteRuleRocksDao voteRuleRocksDao;
    @Autowired private VoteRuleJDBCDao voteRuleJDBCDao;

    /**
     * create new vote-rule for policy
     *
     * @param voteRule the vote rule
     */
    public void add(VoteRule voteRule) {
        VoteRulePO voteRulePO = new VoteRulePO();
        voteRulePO.setPolicyId(voteRule.getPolicyId());
        voteRulePO.setVotePattern(voteRule.getVotePattern().getCode());
        voteRulePO.setCallbackType(voteRule.getCallbackType().getCode());
        if (rsConfig.isUseMySQL()) {
            try {
                voteRuleDao.add(voteRulePO);
            } catch (DuplicateKeyException e) {
                log.error("[add.vote-rule] is idempotent by policyId:{}", voteRule.getPolicyId());
                throw new SlaveException(SlaveErrorEnum.SLAVE_IDEMPOTENT);
            }
        } else {
            voteRuleRocksDao.saveWithTransaction(voteRulePO);
        }
    }

    /**
     * query vote-rule by policy id
     *
     * @param policyId the policy id
     * @return vote rule
     */
    public VoteRule queryByPolicyId(String policyId) {
        VoteRulePO voteRulePO;
        if (rsConfig.isUseMySQL()) {
            voteRulePO = voteRuleDao.queryByPolicyId(policyId);
        } else {
            voteRulePO = voteRuleRocksDao.get(policyId);
        }

        if (voteRulePO == null) {
            return null;
        }
        VoteRule voteRule = new VoteRule();
        voteRule.setPolicyId(policyId);
        voteRule.setVotePattern(VotePatternEnum.fromCode(voteRulePO.getVotePattern()));
        voteRule.setCallbackType(CallbackTypeEnum.fromCode(voteRulePO.getCallbackType()));
        return voteRule;
    }

    /**
     * batch insert
     *
     * @param voteRules the vote rules
     */
    public void batchInsert(List<VoteRule> voteRules) {
        List<VoteRulePO> voteRulePOs = new ArrayList<>(voteRules.size());
        for(VoteRule voteRule : voteRules){
            VoteRulePO voteRulePO = new VoteRulePO();
            voteRulePO.setPolicyId(voteRule.getPolicyId());
            voteRulePO.setVotePattern(voteRule.getVotePattern().getCode());
            voteRulePO.setCallbackType(voteRule.getCallbackType().getCode());
            voteRulePOs.add(voteRulePO);
        }
        try {
            if (rsConfig.isUseMySQL()) {
                voteRuleJDBCDao.batchInsert(voteRulePOs);
            } else {
                voteRuleRocksDao.batchInsert(voteRulePOs);
            }
        } catch (DuplicateKeyException e) {
            log.error("[add.vote-rule] is idempotent");
            throw new SlaveException(SlaveErrorEnum.SLAVE_IDEMPOTENT);
        }
    }
}
