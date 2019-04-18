package com.higgschain.trust.slave.core.service.action.contract;

import com.higgschain.trust.common.utils.Profiler;
import com.higgschain.trust.evmcontract.enums.ExtendsParamTypeEnum;
import com.higgschain.trust.evmcontract.facade.*;
import com.higgschain.trust.evmcontract.facade.compile.ContractInvocation;
import com.higgschain.trust.slave.common.enums.SlaveErrorEnum;
import com.higgschain.trust.slave.common.exception.SlaveException;
import com.higgschain.trust.slave.core.Blockchain;
import com.higgschain.trust.slave.core.service.action.ActionHandler;
import com.higgschain.trust.slave.model.bo.action.Action;
import com.higgschain.trust.slave.model.bo.context.ActionData;
import com.higgschain.trust.slave.model.bo.contract.ContractInvokeV2Action;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * The type Contract invoke v 2 handler.
 *
 * @author kongyu
 * @date 2018 /11/30
 */
@Slf4j
@Component
public class ContractInvokeV2Handler implements ActionHandler {

    @Autowired
    private Blockchain blockchain;

    private void processCustomerContractInvocation(ActionData actionData) {
        if (!(actionData.getCurrentAction() instanceof ContractInvokeV2Action)) {
            throw new IllegalArgumentException("action need a type of ContractInvokeV2Action");
        }
        ContractInvokeV2Action invokeAction = (ContractInvokeV2Action) actionData.getCurrentAction();
        this.verifyParams(invokeAction);

        Long blockHeight = actionData.getCurrentBlock().getBlockHeader().getHeight();
        String parentBlockHash = blockchain.getLastBlockHeader().getBlockHash();

        String txId = actionData.getCurrentTransaction().getCoreTx().getTxId();
        log.info("process contract transaction txId:{}", txId);
        byte[] senderAddress = Hex.decode(invokeAction.getFrom());
        byte[] receiverAddress = Hex.decode(invokeAction.getTo());
        long timestamp = actionData.getCurrentBlock().getBlockHeader().getBlockTime() / 1000;
        byte[] value = new BigInteger("0").toByteArray();

        ContractInvocation contractInvocation = new ContractInvocation();
        byte[] invokeFuncData = contractInvocation.getBytecodeForInvokeContract(invokeAction.getMethodSignature(), invokeAction.getArgs());

        byte[] minerAddress = Hex.decode("095e7baea6a6c7c4c2dfeb977efac326af552d87");
        Map<String,Object> extendsParamMap  = new HashMap(){{
            put(ExtendsParamTypeEnum.POLICY_ID.getCode(), actionData.getCurrentTransaction().getCoreTx().getPolicyId());
        }};

        ContractExecutionContext contractExecutionContext = buildContractExecutionContext(ContractTypeEnum.CUSTOMER_CONTRACT_INVOCATION,
                txId.getBytes(),
                null,
                senderAddress,
                receiverAddress,
                value,
                invokeFuncData,
                Hex.decode(parentBlockHash),
                minerAddress,
                timestamp,
                blockHeight,
                extendsParamMap);

        ContractExecutorFactory executorFactory = new ContractExecutorFactory();

        Executor<ContractExecutionResult> executor = executorFactory.createExecutor(contractExecutionContext);
        ContractExecutionResult result = executor.execute();
        result.setMethod(invokeAction.getMethodSignature());
        ContractExecutionResult.setCurrentResult(result);
    }

    private ContractExecutionContext buildContractExecutionContext(
            ContractTypeEnum contractType, byte[] transactionHash, byte[] nonce, byte[] senderAddress,
            byte[] receiverAddress, byte[] value, byte[] data, byte[] parentHash, byte[] minerAddress,
            long timestamp, long number, Map<String,Object> extendsParamMap) {
        return new ContractExecutionContext(contractType, transactionHash, nonce, senderAddress, receiverAddress,
                value, data, parentHash, minerAddress, timestamp, number, blockchain.getBlockStore(),
                blockchain.getRepositorySnapshot(), extendsParamMap);
    }

    @Override
    public void verifyParams(Action action) throws SlaveException {
        ContractInvokeV2Action invokeAction = (ContractInvokeV2Action) action;
        if (StringUtils.isEmpty(invokeAction.getFrom())){
            log.error("invokeContract validate: from address is empty");
            throw new SlaveException(SlaveErrorEnum.SLAVE_PARAM_VALIDATE_ERROR);
        }
        if (StringUtils.isEmpty(invokeAction.getTo())) {
            log.error("invokeContract validate: to address is empty");
            throw new SlaveException(SlaveErrorEnum.SLAVE_PARAM_VALIDATE_ERROR);
        }
        if (invokeAction.getTo().length() > 64) {
            log.error("invokeContract validate: address is too long");
            throw new SlaveException(SlaveErrorEnum.SLAVE_PARAM_VALIDATE_ERROR);
        }

        if (StringUtils.isEmpty(invokeAction.getMethodSignature())) {
            log.error("invokeContract validate: method is empty");
            throw new SlaveException(SlaveErrorEnum.SLAVE_PARAM_VALIDATE_ERROR);
        }
    }

    @Override
    public void process(ActionData actionData) {
        log.debug("contract invoke start");
        Profiler.enter("contract invoke");
        try {
            processCustomerContractInvocation(actionData);
        } finally {
            Profiler.release();
        }
    }
}
