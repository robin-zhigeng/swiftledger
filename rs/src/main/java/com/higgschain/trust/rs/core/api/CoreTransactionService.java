package com.higgschain.trust.rs.core.api;

import com.higgschain.trust.rs.core.bo.CoreTxBO;
import com.higgschain.trust.rs.core.vo.RsCoreTxVO;
import com.higgschain.trust.common.vo.RespData;
import com.higgschain.trust.slave.model.bo.CoreTransaction;

import java.util.List;

/**
 * The interface Core transaction service.
 *
 * @author liuyu
 * @description transaction service of rs core
 * @date 2018 -05-12
 */
public interface CoreTransactionService {
    /**
     * submit transaction from custom rs
     *
     * @param coreTx the core tx
     */
    void submitTx(CoreTransaction coreTx);

    /**
     * 同步等待
     *
     * @param txId   the tx id
     * @param forEnd the for end
     * @return resp data
     */
    RespData syncWait(String txId, boolean forEnd);

    /**
     * process init data,sign and update tx status to wait,called by scheduler
     *
     * @param txId the tx id
     */
    void processInitTx(String txId);

    /**
     * process need_vote tx
     *
     * @param txId the tx id
     */
    void processNeedVoteTx(String txId);

    /**
     * submit to slave
     *
     * @param boList the bo list
     */
    void submitToSlave(List<CoreTxBO> boList);

    /**
     * query by txId
     *
     * @param txId the tx id
     * @return rs core tx vo
     */
    RsCoreTxVO queryCoreTx(String txId);
}
