package com.higgschain.trust.presstest.service;

import com.alibaba.fastjson.JSONObject;
import com.higgschain.trust.presstest.AppConst;
import com.higgschain.trust.presstest.vo.StoreVO;
import com.higgschain.trust.rs.common.config.RsConfig;
import com.higgschain.trust.rs.core.api.RsCoreFacade;
import com.higgschain.trust.slave.api.enums.VersionEnum;
import com.higgschain.trust.common.vo.RespData;
import com.higgschain.trust.slave.model.bo.CoreTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.testng.collections.Lists;

/**
 * The type Store service.
 *
 * @author liuyu
 * @description
 * @date 2018 -09-11
 */
@Service @Slf4j public class StoreService {
    /**
     * The Rs core facade.
     */
    @Autowired
    RsCoreFacade rsCoreFacade;
    /**
     * The Rs config.
     */
    @Autowired
    RsConfig rsConfig;

    /**
     * 存正交易
     *
     * @param vo the vo
     * @return the resp data
     */
    public RespData store(StoreVO vo) {
        CoreTransaction coreTransaction = new CoreTransaction();
        coreTransaction.setTxId(vo.getReqNo());
        coreTransaction.setPolicyId(AppConst.STORE);
        JSONObject bizData = new JSONObject();
        bizData.put("biz",vo);
        coreTransaction.setBizModel(bizData);
        coreTransaction.setActionList(Lists.newArrayList());
        coreTransaction.setSender(rsConfig.getRsName());
        coreTransaction.setVersion(VersionEnum.V1.getCode());

        rsCoreFacade.processTx(coreTransaction);
        return rsCoreFacade.syncWait(vo.getReqNo(), true);
    }
}
