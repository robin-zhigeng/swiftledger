package com.higgschain.trust.rs.core.service;

import com.higgschain.trust.IntegrateBaseTest;
import com.higgschain.trust.rs.core.api.SignService;
import com.higgschain.trust.slave.model.bo.CoreTransaction;
import com.higgschain.trust.slave.model.bo.SignInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

/**
 * The type Sign service test.
 *
 * @author liuyu
 * @description
 * @date 2018 -05-15
 */
public class SignServiceTest extends IntegrateBaseTest {
    /**
     * The Sign service.
     */
    @Autowired SignService signService;

    /**
     * Test sign.
     */
    @Test
    public void testSign(){
        CoreTransaction coreTx = new CoreTransaction();
        SignInfo signInfo = signService.signTx(coreTx);
        System.out.println("---->:" + signInfo);
    }
}
