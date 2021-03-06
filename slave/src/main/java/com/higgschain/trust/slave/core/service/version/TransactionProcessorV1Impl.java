package com.higgschain.trust.slave.core.service.version;

import com.alibaba.fastjson.JSON;
import com.higgschain.trust.evmcontract.core.TransactionResultInfo;
import com.higgschain.trust.evmcontract.facade.ContractExecutionResult;
import com.higgschain.trust.evmcontract.facade.exception.ContractExecutionException;
import com.higgschain.trust.slave.api.enums.ActionTypeEnum;
import com.higgschain.trust.slave.api.enums.VersionEnum;
import com.higgschain.trust.slave.common.enums.SlaveErrorEnum;
import com.higgschain.trust.slave.common.exception.SlaveException;
import com.higgschain.trust.slave.core.Blockchain;
import com.higgschain.trust.slave.core.service.action.ActionHandler;
import com.higgschain.trust.slave.core.service.action.account.*;
import com.higgschain.trust.slave.core.service.action.ca.CaAuthHandler;
import com.higgschain.trust.slave.core.service.action.ca.CaCancelHandler;
import com.higgschain.trust.slave.core.service.action.ca.CaUpdateHandler;
import com.higgschain.trust.slave.core.service.action.contract.*;
import com.higgschain.trust.slave.core.service.action.dataidentity.DataIdentityActionHandler;
import com.higgschain.trust.slave.core.service.action.manage.CancelRsHandler;
import com.higgschain.trust.slave.core.service.action.manage.RegisterPolicyHandler;
import com.higgschain.trust.slave.core.service.action.manage.RegisterRsHandler;
import com.higgschain.trust.slave.core.service.action.node.NodeJoinHandler;
import com.higgschain.trust.slave.core.service.action.node.NodeLeaveHandler;
import com.higgschain.trust.slave.core.service.action.utxo.UTXOActionHandler;
import com.higgschain.trust.slave.core.service.contract.StandardExecuteContextData;
import com.higgschain.trust.slave.core.service.contract.StandardSmartContract;
import com.higgschain.trust.slave.core.service.snapshot.agent.AccountContractBindingSnapshotAgent;
import com.higgschain.trust.slave.model.bo.CoreTransaction;
import com.higgschain.trust.slave.model.bo.account.AccountFreeze;
import com.higgschain.trust.slave.model.bo.account.AccountOperation;
import com.higgschain.trust.slave.model.bo.account.AccountTradeInfo;
import com.higgschain.trust.slave.model.bo.account.AccountUnFreeze;
import com.higgschain.trust.slave.model.bo.action.Action;
import com.higgschain.trust.slave.model.bo.context.ActionData;
import com.higgschain.trust.slave.model.bo.context.TransactionData;
import com.higgschain.trust.slave.model.bo.contract.AccountContractBinding;
import com.higgschain.trust.slave.model.bo.contract.ContractCreationV2Action;
import com.higgschain.trust.slave.model.bo.contract.ContractInvokeV2Action;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * The type Transaction processor v 1.
 *
 * @author WangQuanzhou
 * @desc transaction processor V1
 * @date 2018 /3/28 18:01
 */
@Slf4j
@Component
public class TransactionProcessorV1Impl implements TransactionProcessor, InitializingBean {

    /**
     * The Tx processor holder.
     */
    @Autowired
    TxProcessorHolder txProcessorHolder;

    @Autowired
    private OpenAccountHandler openAccountHandler;
    @Autowired
    private AccountOperationHandler accountOperationHandler;
    @Autowired
    private AccountFreezeHandler accountFreezeHandler;
    @Autowired
    private AccountUnFreezeHandler accountUnFreezeHandler;
    @Autowired
    private UTXOActionHandler utxoActionHandler;
    @Autowired
    private RegisterRsHandler registerRsHandler;
    @Autowired
    private RegisterPolicyHandler registerPolicyHandler;
    @Autowired
    private IssueCurrencyHandler issueCurrencyHandler;
    @Autowired
    private DataIdentityActionHandler dataIdentityActionHandler;
    @Autowired
    private ContractCreationHandler contractCreationHandler;
    @Autowired
    private ContractInvokeHandler contractInvokeHandler;
    @Autowired
    private ContractStateMigrationHandler contractStateMigrationHandler;
    @Autowired
    private AccountContractBindingHandler accountContractBindingHandler;
    @Autowired
    private AccountContractBindingSnapshotAgent accountContractBindingSnapshotAgent;
    @Autowired
    private StandardSmartContract standardSmartContract;
    @Autowired
    private CaAuthHandler caAuthHandler;
    @Autowired
    private CaCancelHandler caCancelHandler;
    @Autowired
    private CaUpdateHandler caUpdateHandler;
    @Autowired
    private CancelRsHandler cancelRsHandler;
    @Autowired
    private NodeJoinHandler nodeJoinHandler;
    @Autowired
    private NodeLeaveHandler nodeLeaveHandler;
    @Autowired
    private ContractInvokeV2Handler contractInvokeV2Handler;
    @Autowired
    private ContractCreationV2Handler contractCreationV2Handler;

    @Autowired
    private Blockchain blockchain;


    @Override
    public void afterPropertiesSet() throws Exception {
        txProcessorHolder.registVerisonProcessor(VersionEnum.V1, this);
    }

    @Override
    public void process(TransactionData transactionData) {
        boolean hashEvmContract = false;
        boolean isCreateEvmContract = false;
        CoreTransaction coreTx = transactionData.getCurrentTransaction().getCoreTx();
        log.debug("[process]coreTx:{}", coreTx);
        List<Action> actionList = coreTx.getActionList();
        if (CollectionUtils.isEmpty(actionList)) {
            return;
        }
        //sort by index
        Collections.sort(actionList, new Comparator<Action>() {
            @Override
            public int compare(Action o1, Action o2) {
                return o1.getIndex() > o2.getIndex() ? 1 : -1;
            }
        });
        //for each
        for (Action action : actionList) {
            if (action instanceof ContractCreationV2Action || action instanceof ContractInvokeV2Action) {
                if (hashEvmContract) {
                    log.error("One transaction only contain one contract operation");
                    throw new SlaveException(SlaveErrorEnum.SLAVE_TX_NOT_ONLY_ONE_CONTRACT_ACTION_EXCEPTION);
                }
                hashEvmContract = true;
            }
            //set current action
            transactionData.setCurrentAction(action);

            //handle action
            ActionHandler actionHandler = getHandlerByType(action.getType());
            if (actionHandler == null) {
                log.error("[process] get action handler is null by action type:{}", action.getType());
                throw new SlaveException(SlaveErrorEnum.SLAVE_ACTION_HANDLER_IS_NOT_EXISTS_EXCEPTION);
            }
            //TODO do not bind account with contract
            //exeContract(action, transactionData.parseActionData());

            //execute action
            actionHandler.process(transactionData.parseActionData());
            if (hashEvmContract) {
                processEvmContractResult(transactionData.getCurrentPackage().getHeight(), coreTx, action);
            }
        }
    }

    /**
     * get action handler by action type
     *
     * @param typeEnum
     * @return
     */
    @Override
    public ActionHandler getHandlerByType(ActionTypeEnum typeEnum) {
        if (null == typeEnum) {
            log.error("[getHandlerByType] action type is null");
            throw new SlaveException(SlaveErrorEnum.SLAVE_ACTION_NOT_EXISTS_EXCEPTION,
                    "[getHandlerByType] action type is null");
        }
        switch (typeEnum) {
            case OPEN_ACCOUNT:
                return openAccountHandler;
            case UTXO:
                return utxoActionHandler;
            case FREEZE:
                return accountFreezeHandler;
            case UNFREEZE:
                return accountUnFreezeHandler;
            case ACCOUNTING:
                return accountOperationHandler;
            case REGISTER_RS:
                return registerRsHandler;
            case RS_CANCEL:
                return cancelRsHandler;
            case REGISTER_POLICY:
                return registerPolicyHandler;
            case ISSUE_CURRENCY:
                return issueCurrencyHandler;
            case CREATE_DATA_IDENTITY:
                return dataIdentityActionHandler;
            case BIND_CONTRACT:
                return accountContractBindingHandler;
            case TRIGGER_CONTRACT:
                return contractInvokeHandler;
            case REGISTER_CONTRACT:
                return contractCreationHandler;
            case CONTRACT_STATE_MIGRATION:
                return contractStateMigrationHandler;
            case CA_AUTH:
                return caAuthHandler;
            case CA_CANCEL:
                return caCancelHandler;
            case CA_UPDATE:
                return caUpdateHandler;
            case NODE_JOIN:
                return nodeJoinHandler;
            case NODE_LEAVE:
                return nodeLeaveHandler;
            case CONTRACT_INVOKED:
                return contractInvokeV2Handler;
            case CONTRACT_CREATION:
                return contractCreationV2Handler;
            default:
        }
        log.error("[getHandlerByType] action type not exist exception, actionType={}", JSON.toJSONString(typeEnum));
        throw new SlaveException(SlaveErrorEnum.SLAVE_ACTION_NOT_EXISTS_EXCEPTION,
                "[getHandlerByType] action type not exist exception");
    }


    private void processEvmContractResult(long blockHeight, CoreTransaction tx, Action action) {
        ContractExecutionResult executionResult = ContractExecutionResult.getCurrentResult();
        if (executionResult != null) {
            ContractExecutionResult.clearCurrentResult();
            if (executionResult.getException() != null) {
                log.warn(executionResult.getException().getMessage());
            }

            TransactionResultInfo resultInfo = new TransactionResultInfo(blockHeight, tx.getTxId().getBytes(), 1,
                    executionResult.getBloomFilter(), executionResult.getLogInfoList(), executionResult.getResult());
            if (action instanceof ContractCreationV2Action) {
                resultInfo.setCreatedAddress(executionResult.getReceiverAddress());
            }
            resultInfo.setInvokeMethod(executionResult.getMethod());
            String errorMessage = executionResult.getErrorMessage();
            if (executionResult.getRevert()) {
                resultInfo.setError(StringUtils.isNotEmpty(errorMessage) ? errorMessage : "reverted");
                blockchain.putResultInfo(resultInfo);
                throw new ContractExecutionException(String.format("Contract revert at %s{%s}: %s",
                        Hex.toHexString(executionResult.getReceiverAddress()), executionResult.getMethod(), executionResult.getErrorMessage()));
            } else if (executionResult.getException() != null) {
                resultInfo.setError(StringUtils.isNotEmpty(errorMessage) ? errorMessage : "exception");
                blockchain.putResultInfo(resultInfo);
                throw new ContractExecutionException(String.format("Contract exception occurred at %s{%s}: %s",
                        Hex.toHexString(executionResult.getReceiverAddress()), executionResult.getMethod(), executionResult.getException().getMessage()));
            }
            blockchain.putResultInfo(resultInfo);
        }
    }


    private void exeContract(Action action, ActionData actionData) {
        List<String> accountNos = new ArrayList<>();
        switch (action.getType()) {
            case FREEZE:
                AccountFreeze accountFreeze = (AccountFreeze) action;
                accountNos.add(accountFreeze.getAccountNo());
                break;
            case UNFREEZE:
                AccountUnFreeze accountUnFreeze = (AccountUnFreeze) action;
                accountNos.add(accountUnFreeze.getAccountNo());
                break;
            case ACCOUNTING:
                AccountOperation accountOperation = (AccountOperation) action;
                List<AccountTradeInfo> debitTradeInfo = accountOperation.getDebitTradeInfo();
                Map<String, Object> map = new HashMap<>();
                if (!CollectionUtils.isEmpty(debitTradeInfo)) {
                    for (AccountTradeInfo accountTradeInfo : debitTradeInfo) {
                        map.put(accountTradeInfo.getAccountNo(), accountTradeInfo);
                    }
                }
                List<AccountTradeInfo> creditTradeInfo = accountOperation.getCreditTradeInfo();
                if (!CollectionUtils.isEmpty(creditTradeInfo)) {
                    for (AccountTradeInfo accountTradeInfo : creditTradeInfo) {
                        map.put(accountTradeInfo.getAccountNo(), accountTradeInfo);
                    }
                }
                accountNos.addAll(map.keySet());
                break;
            default:
        }
        if (CollectionUtils.isEmpty(accountNos)) {
            log.debug("[exeContract]accountNos is empty");
            return;
        }
        for (String accountNo : accountNos) {
            List<AccountContractBinding> bindingList = null;
            bindingList = accountContractBindingSnapshotAgent.getListByAccount(accountNo);
            if (CollectionUtils.isEmpty(bindingList)) {
                continue;
            }
            //execute contracts
            for (AccountContractBinding binding : bindingList) {
                StandardExecuteContextData standardExecuteContextData = new StandardExecuteContextData();
                standardExecuteContextData.setAction(actionData);
                //execute
                standardSmartContract.execute(binding, standardExecuteContextData);
            }
        }
    }
}
