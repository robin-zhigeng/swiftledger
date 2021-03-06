package com.higgschain.trust.slave.core.service.action.account;

import com.higgschain.trust.common.utils.Profiler;
import com.higgschain.trust.contract.ExecuteContext;
import com.higgschain.trust.slave.common.enums.SlaveErrorEnum;
import com.higgschain.trust.slave.common.exception.SlaveException;
import com.higgschain.trust.slave.core.service.action.ActionHandler;
import com.higgschain.trust.slave.core.service.datahandler.account.AccountSnapshotHandler;
import com.higgschain.trust.slave.model.bo.account.AccountFreezeRecord;
import com.higgschain.trust.slave.model.bo.account.AccountInfo;
import com.higgschain.trust.slave.model.bo.account.AccountUnFreeze;
import com.higgschain.trust.slave.model.bo.action.Action;
import com.higgschain.trust.slave.model.bo.context.ActionData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * The type Account un freeze handler.
 *
 * @author liuyu
 * @description unfreeze account balance
 * @date 2018 -03-29
 */
@Slf4j @Component public class AccountUnFreezeHandler implements ActionHandler {
    /**
     * The Account snapshot handler.
     */
    @Autowired
    AccountSnapshotHandler accountSnapshotHandler;

    @Override public void verifyParams(Action action) throws SlaveException {
        AccountUnFreeze bo = (AccountUnFreeze)action;
        if(StringUtils.isEmpty(bo.getAccountNo())){
            log.error("[verifyParams] accountNo is null param:{}",bo);
            throw new SlaveException(SlaveErrorEnum.SLAVE_PARAM_VALIDATE_ERROR);
        }
        if(StringUtils.isEmpty(bo.getBizFlowNo()) || bo.getBizFlowNo().length() > 64){
            log.error("[verifyParams] bizFlowNo is null or illegal param:{}",bo);
            throw new SlaveException(SlaveErrorEnum.SLAVE_PARAM_VALIDATE_ERROR);
        }
        if(bo.getAmount() == null){
            log.error("[verifyParams] amount is null or illegal param:{}",bo);
            throw new SlaveException(SlaveErrorEnum.SLAVE_ACCOUNT_FREEZE_AMOUNT_ERROR);
        }
    }

    @Override public void process(ActionData actionData) {
        AccountUnFreeze bo = (AccountUnFreeze)actionData.getCurrentAction();
        //
        unFreeze(bo,actionData.getCurrentBlock().getBlockHeader().getHeight());
    }

    /**
     * Un freeze.
     *
     * @param bo          the bo
     * @param blockHeight the block height
     */
    public void unFreeze(AccountUnFreeze bo,Long blockHeight){
        //validate business
        //check record is exists
        AccountFreezeRecord freezeRecord = accountSnapshotHandler.getAccountFreezeRecord(bo.getBizFlowNo(), bo.getAccountNo());
        if (freezeRecord == null) {
            log.error("[accountUnFreeze.process] freezeRecord is not exists flowNo:{},accountNo:{}", bo.getBizFlowNo(),
                bo.getAccountNo());
            throw new SlaveException(SlaveErrorEnum.SLAVE_ACCOUNT_FREEZE_RECORD_IS_NOT_EXISTS_ERROR);
        }
        BigDecimal happenAmount = bo.getAmount();
        //check amount
        if (happenAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("[accountUnFreeze.process] amount is check fail by amount:{}", happenAmount);
            throw new SlaveException(SlaveErrorEnum.SLAVE_ACCOUNT_UNFREEZE_AMOUNT_ERROR);
        }
        //check can unfreeze amount
        BigDecimal afterAmount = freezeRecord.getAmount().subtract(happenAmount);
        if (afterAmount.compareTo(BigDecimal.ZERO) < 0) {
            log.error("[accountUnFreeze.process] can unfreeze amount is not enough by accountNo:{}",
                bo.getAccountNo());
            throw new SlaveException(SlaveErrorEnum.SLAVE_ACCOUNT_BALANCE_IS_NOT_ENOUGH_ERROR);
        }
        //get by accountNo
        AccountInfo accountInfo = accountSnapshotHandler.getAccountInfo(bo.getAccountNo());
        if (accountInfo == null) {
            log.error("[accountUnFreeze.process] account info is not exists by accountNo:{}", bo.getAccountNo());
            throw new SlaveException(SlaveErrorEnum.SLAVE_ACCOUNT_IS_NOT_EXISTS_ERROR);
        }
        //check freeze amount of account info
        BigDecimal afterOfAccount = accountInfo.getFreezeAmount().subtract(happenAmount);
        if (afterOfAccount.compareTo(BigDecimal.ZERO) < 0) {
            log.error("[accountUnFreeze.process] can unfreeze amount is not enough by accountNo:{}",
                bo.getAccountNo());
            throw new SlaveException(SlaveErrorEnum.SLAVE_ACCOUNT_BALANCE_IS_NOT_ENOUGH_ERROR);
        }
        //check contract address
        try {
            Profiler.enter("[checkContract]");
            checkContract(freezeRecord.getContractAddr());
        }finally {
            Profiler.release();
        }
        log.debug("[accountUnFreeze.process] before-freeze-record:{}", freezeRecord.getAmount());
        log.debug("[accountUnFreeze.process] after-freeze-record:{}", afterAmount);
        log.debug("[accountUnFreeze.process] before-freeze-account:{}", accountInfo.getFreezeAmount());
        log.debug("[accountUnFreeze.process] after-freeze-account:{}", afterOfAccount);
        //unfreeze
        try {
            Profiler.enter("[persistForUnFreeze]");
            accountSnapshotHandler.unfreeze(bo, freezeRecord, blockHeight);
        }finally {
            Profiler.release();
        }
    }

    /**
     * check contract
     *
     * @param contractBindHashOfRecord
     */
    private void checkContract(String contractBindHashOfRecord){
        if (StringUtils.isEmpty(contractBindHashOfRecord)) {
            return;
        }
        ExecuteContext executeContext = ExecuteContext.getCurrent();
        if(executeContext == null){
            log.error("[accountUnFreeze.checkContract] executeContext is not exist");
            throw new SlaveException(SlaveErrorEnum.SLAVE_CONTRACT_NOT_EXIST_ERROR);
        }
        if(executeContext.getContract() == null){
            log.error("[accountUnFreeze.checkContract] executeContext.getContract is not exist");
            throw new SlaveException(SlaveErrorEnum.SLAVE_CONTRACT_NOT_EXIST_ERROR);
        }
        //current execute contract addr
        String bindHash = executeContext.getStateInstanceKey();
        //compare to bindHash of freeze record
        if(!StringUtils.equals(contractBindHashOfRecord,bindHash)){
            log.error("[accountUnFreeze.checkContract] contractBindHashOfRecord is unequals bindHash");
            throw new SlaveException(SlaveErrorEnum.SLAVE_CONTRACT_NOT_EXIST_ERROR);
        }
    }
}
