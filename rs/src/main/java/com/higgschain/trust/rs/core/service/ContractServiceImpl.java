package com.higgschain.trust.rs.core.service;

import com.higgschain.trust.common.utils.BeanConvertor;
import com.higgschain.trust.consensus.config.NodeState;
import com.higgschain.trust.evmcontract.crypto.ECKey;
import com.higgschain.trust.rs.common.enums.RsCoreErrorEnum;
import com.higgschain.trust.rs.common.exception.RsCoreException;
import com.higgschain.trust.rs.common.utils.CoreTransactionConvertor;
import com.higgschain.trust.rs.core.api.ContractService;
import com.higgschain.trust.rs.core.api.CoreTransactionService;
import com.higgschain.trust.rs.core.api.enums.CoreTxResultEnum;
import com.higgschain.trust.rs.core.bo.ContractCreateV2Request;
import com.higgschain.trust.rs.core.bo.ContractMigrationRequest;
import com.higgschain.trust.rs.core.bo.ContractQueryRequest;
import com.higgschain.trust.rs.core.vo.RsCoreTxVO;
import com.higgschain.trust.slave.api.enums.ActionTypeEnum;
import com.higgschain.trust.slave.api.enums.TxTypeEnum;
import com.higgschain.trust.slave.api.enums.VersionEnum;
import com.higgschain.trust.slave.api.enums.manage.InitPolicyEnum;
import com.higgschain.trust.slave.api.vo.ContractVO;
import com.higgschain.trust.slave.api.vo.PageVO;
import com.higgschain.trust.slave.api.vo.QueryContractVO;
import com.higgschain.trust.common.vo.RespData;
import com.higgschain.trust.slave.core.repository.contract.ContractRepository;
import com.higgschain.trust.slave.core.service.contract.StandardSmartContract;
import com.higgschain.trust.slave.model.bo.CoreTransaction;
import com.higgschain.trust.slave.model.bo.action.Action;
import com.higgschain.trust.slave.model.bo.contract.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * The type Contract service.
 *
 * @author duhongming
 * @date 2018 /5/14
 */
@Service @Slf4j public class ContractServiceImpl implements ContractService {

    @Autowired private NodeState nodeState;
    @Autowired private CoreTransactionService coreTransactionService;
    @Autowired private ContractRepository contractRepository;
    @Autowired private StandardSmartContract smartContract;
    @Autowired private CoreTransactionConvertor convertor;

    private CoreTransaction buildCoreTx(String txId, String code, Object... initArgs) {
        List<Action> actionList = new ArrayList<>(1);
        actionList.add(buildAction(code, initArgs));

        CoreTransaction coreTx = new CoreTransaction();
        coreTx.setTxId(txId);
        coreTx.setActionList(actionList);
        coreTx.setVersion(VersionEnum.V1.getCode());
        coreTx.setSender(nodeState.getNodeName());
        coreTx.setSendTime(new Date());
        coreTx.setPolicyId(InitPolicyEnum.CONTRACT_ISSUE.getPolicyId());
        coreTx.setTxType(TxTypeEnum.CONTRACT.getCode());
        return coreTx;
    }

    private CoreTransaction buildCoreTx(String txId, Action action) {
        List<Action> actionList = new ArrayList<>(1);
        actionList.add(action);

        CoreTransaction coreTx = new CoreTransaction();
        coreTx.setTxId(txId);
        coreTx.setActionList(actionList);
        coreTx.setVersion(VersionEnum.V1.getCode());
        coreTx.setSender(nodeState.getNodeName());
        coreTx.setSendTime(new Date());
        coreTx.setPolicyId(InitPolicyEnum.CONTRACT_INVOKE.getPolicyId());
        coreTx.setTxType(TxTypeEnum.CONTRACT.getCode());
        return coreTx;
    }

    private CoreTransaction buildCreateContractCoreTx(ContractCreateV2Request request) {
        String hexCode =
            convertor.buildContractCode(request.getSourceCode(), request.getContractor(), request.getInitArgs());
        ContractCreationV2Action createAction =
            convertor.buildContractCreationV2Action(request.getFromAddr(), request.getContractAddress(), hexCode, 0);
        return convertor.buildCoreTransaction(request.getTxId(), null, Collections.singletonList(createAction),
            InitPolicyEnum.CONTRACT_ISSUE.getPolicyId(), TxTypeEnum.CONTRACT.getCode());
    }

    private ContractCreationAction buildAction(String code, Object... initArgs) {
        ContractCreationAction creationAction = new ContractCreationAction();
        creationAction.setCode(code);
        creationAction.setType(ActionTypeEnum.REGISTER_CONTRACT);
        creationAction.setIndex(0);
        creationAction.setLanguage("javascript");
        creationAction.setInitArgs(initArgs);
        return creationAction;
    }

    private ContractInvokeAction buildInvokeAction(String address, Object... args) {
        ContractInvokeAction invokeAction = new ContractInvokeAction();
        invokeAction.setAddress(address);
        invokeAction.setArgs(args);
        invokeAction.setIndex(0);
        invokeAction.setType(ActionTypeEnum.TRIGGER_CONTRACT);
        return invokeAction;
    }

    private ContractInvokeV2Action buildInvokeV2Action(String from, String to, BigDecimal value, String methodSignature,
        Object... args) {
        ContractInvokeV2Action invokeV2Action = new ContractInvokeV2Action();
        invokeV2Action.setFrom(from);
        invokeV2Action.setTo(to);
        invokeV2Action.setArgs(args);
        invokeV2Action.setIndex(0);
        invokeV2Action.setType(ActionTypeEnum.CONTRACT_INVOKED);
        invokeV2Action.setValue(value);
        invokeV2Action.setMethodSignature(methodSignature);
        return invokeV2Action;
    }

    private ContractStateMigrationAction buildMigrationAction(ContractMigrationRequest migrationRequest) {
        ContractStateMigrationAction action = new ContractStateMigrationAction();
        action.setFormInstanceAddress(migrationRequest.getFromAddress());
        action.setToInstanceAddress(migrationRequest.getToAddress());
        action.setIndex(0);
        action.setType(ActionTypeEnum.TRIGGER_CONTRACT);
        return action;
    }

    @Override public RespData<?> deploy(String txId, String code, Object... initArgs) {
        CoreTransaction coreTx = buildCoreTx(txId, code, initArgs);
        RespData respData = submit(coreTx);
        if (null != respData) {
            return respData;
        }
        respData = coreTransactionService.syncWait(txId, true);
        return respData;
    }

    @Override public RespData<?> deployV2(ContractCreateV2Request request) {
        String txId = request.getTxId();
        if (StringUtils.isBlank(request.getContractAddress())) {
            String address = Hex.toHexString(new ECKey().getAddress());
            request.setContractAddress(address);
        }
        CoreTransaction coreTx = buildCreateContractCoreTx(request);
        RespData respData = submit(coreTx);
        if (null != respData) {
            return respData;
        }
        respData = coreTransactionService.syncWait(txId, true);
        return respData;
    }

    @Override public PageVO<ContractVO> queryList(Long height, String txId, Integer pageIndex, Integer pageSize) {
        PageVO<ContractVO> result = new PageVO();
        result.setTotal(contractRepository.queryCount(height, txId));
        List<Contract> list =
            contractRepository.query(height, txId, pageIndex, pageSize);
        result.setData(BeanConvertor.convertList(list, ContractVO.class));
        return result;
    }

    @Override public List<ContractVO> queryContractsByPage(QueryContractVO req) {
        if (null == req) {
            return null;
        }
        //less than minimum，use default value
        Integer minNo = 0;
        if (null == req.getPageNo() || req.getPageNo().compareTo(minNo) <= 0) {
            req.setPageNo(1);
        }
        //over the maximum，use default value
        Integer maxSize = 100;
        if (null == req.getPageSize() || req.getPageSize().compareTo(maxSize) == 1) {
            req.setPageSize(20);
        }
        List<Contract> list =
            contractRepository.query(req.getHeight(), req.getTxId(), req.getPageNo(), req.getPageSize());
        List<ContractVO> result = BeanConvertor.convertList(list, ContractVO.class);
        return result;
    }

    @Override public RespData invoke(String txId, String address, Object... args) {
        ContractInvokeAction action = buildInvokeAction(address, args);
        CoreTransaction coreTx = buildCoreTx(txId, action);
        RespData respData = submit(coreTx);
        if (null != respData) {
            return respData;
        }
        respData = coreTransactionService.syncWait(txId, true);
        return respData;
    }

    /**
     * invoke V2 contract
     *
     * @param txId
     * @param from
     * @param to
     * @param value
     * @param methodSignature
     * @param args
     * @return
     */
    @Override public RespData invokeV2(String txId, String from, String to, BigDecimal value, String methodSignature,
        Object... args) {
        ContractInvokeV2Action action = buildInvokeV2Action(from, to, value, methodSignature, args);
        CoreTransaction coreTx = buildCoreTx(txId, action);
        RespData respData = submit(coreTx);
        if (null != respData) {
            return respData;
        }
        respData = coreTransactionService.syncWait(txId, true);
        return respData;
    }

    @Override public RespData migration(ContractMigrationRequest migrationRequest) {
        ContractStateMigrationAction action = buildMigrationAction(migrationRequest);
        CoreTransaction coreTx = buildCoreTx(migrationRequest.getTxId(), action);
        RespData respData = submit(coreTx);
        if (null != respData) {
            return respData;
        }
        respData = coreTransactionService.syncWait(migrationRequest.getTxId(), true);
        return respData;
    }

    @Override public Object query(ContractQueryRequest request) {
        return smartContract.executeQuery(request.getAddress(), request.getMethodName(), request.getBizArgs());
    }

    @Override public ContractVO queryByTxId(String txId, int actionIndex) {
        return contractRepository.queryByTxId(txId, actionIndex);
    }

    /**
     * commin submit to slave
     *
     * @param coreTransaction
     * @return
     */
    private RespData<?> submit(CoreTransaction coreTransaction) {
        try {
            coreTransactionService.submitTx(coreTransaction);
        } catch (RsCoreException e) {
            if (e.getCode() == RsCoreErrorEnum.RS_CORE_IDEMPOTENT) {
                RsCoreTxVO rsCoreTxVO = coreTransactionService.queryCoreTx(coreTransaction.getTxId());
                if (rsCoreTxVO.getExecuteResult() != CoreTxResultEnum.SUCCESS) {
                    return RespData
                        .error(rsCoreTxVO.getErrorCode(), rsCoreTxVO.getErrorMsg(), coreTransaction.getTxId());
                }
                return RespData.success(coreTransaction.getTxId());
            }
            log.error("Submit to RS error", e);
        }
        return null;
    }

}
