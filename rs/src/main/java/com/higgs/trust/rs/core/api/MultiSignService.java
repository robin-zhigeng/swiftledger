package com.higgs.trust.rs.core.api;

import com.higgs.trust.rs.common.exception.RsCoreException;
import com.higgs.trust.rs.core.vo.MultiSignHashVO;
import com.higgs.trust.rs.core.vo.MultiSignRuleVO;
import com.higgs.trust.rs.core.vo.MultiSignTxVO;
import com.higgs.trust.slave.api.vo.RespData;

/**
 * @author liuyu
 * @description
 * @date 2019-03-20
 */
public interface MultiSignService {

    /**
     * create a address
     *
     * @param rule
     *          validation rules for the address
     * @return
     */
    RespData<String> createAddress(MultiSignRuleVO rule) throws RsCoreException;

    /**
     * get sign-hash by tx data
     *
     * @param rule
     * @return
     * @throws RsCoreException
     */
    RespData<String> getSignHashValue(MultiSignHashVO rule) throws RsCoreException;

    /**
     *
     * @param rule
     * @return
     * @throws RsCoreException
     */
    RespData<Boolean> transfer(MultiSignTxVO rule) throws RsCoreException;
}
