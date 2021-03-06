package com.higgschain.trust.slave._interface.accounting;

import com.higgschain.trust.slave._interface.InterfaceCommonTest;
import com.higgschain.trust.slave.api.enums.ActionTypeEnum;
import com.higgschain.trust.slave.core.service.action.account.AccountOperationHandler;
import com.higgschain.trust.slave.model.bo.account.AccountOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * The type Operation account test.
 *
 * @author liuyu
 * @description
 * @date 2018 -04-26
 */
@Slf4j public class OperationAccountTest extends InterfaceCommonTest {
    private static String PROVIDER_ROOT_PATH = "java/com/higgs/trust/slave/core/service/accounting/operationAccount/";

    /**
     * The Account operation handler.
     */
    @Autowired AccountOperationHandler accountOperationHandler;

    @Override protected String getProviderRootPath() {
        return PROVIDER_ROOT_PATH;
    }

    /**
     * Param validate.
     *
     * @param param the param
     */
    @Test(dataProvider = "defaultProvider", priority = 1) public void paramValidate(Map<?, ?> param){
        log.info("[paramValidate]{}", param.get("comment"));
        AccountOperation action = getAction(param,AccountOperation.class,ActionTypeEnum.ACCOUNTING);
        executeActionHandler(param,accountOperationHandler,action);
    }

    /**
     * Test regular.
     *
     * @param param the param
     */
    @Test(dataProvider = "defaultProvider", priority = 2) public void testRegular(Map<?, ?> param){
        log.info("[testRegular]{}", param.get("comment"));
        executeBeforeSql(param);

        AccountOperation action = getAction(param,AccountOperation.class,ActionTypeEnum.ACCOUNTING);
        executeActionHandler(param,accountOperationHandler,action);

        checkResults(param);

        executeAfterSql(param);
    }

    /**
     * Test exception.
     *
     * @param param the param
     */
    @Test(dataProvider = "defaultProvider", priority = 3) public void testException(Map<?, ?> param){
        log.info("[testException]{}", param.get("comment"));
        executeBeforeSql(param);

        AccountOperation action = getAction(param,AccountOperation.class,ActionTypeEnum.ACCOUNTING);
        executeActionHandler(param,accountOperationHandler,action);

        executeAfterSql(param);
    }

}
