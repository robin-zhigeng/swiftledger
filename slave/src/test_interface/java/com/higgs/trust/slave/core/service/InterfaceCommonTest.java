package com.higgschain.trust.slave.core.service;

import com.alibaba.fastjson.JSON;
import com.higgschain.trust.slave.BaseTest;
import com.higgschain.trust.slave.JsonFileUtil;
import com.higgschain.trust.slave.api.enums.ActionTypeEnum;
import com.higgschain.trust.slave.api.enums.manage.InitPolicyEnum;
import com.higgschain.trust.slave.core.service.action.ActionHandler;
import com.higgschain.trust.slave.core.service.action.account.TestDataMaker;
import com.higgschain.trust.slave.core.service.snapshot.SnapshotService;
import com.higgschain.trust.slave.model.bo.Package;
import com.higgschain.trust.slave.model.bo.action.Action;
import com.higgschain.trust.slave.model.bo.context.PackContext;
import com.higgschain.trust.slave.model.enums.biz.PackageStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * The type Interface common test.
 *
 * @author liuyu
 * @description
 * @date 2018 -04-26
 */
@Slf4j public abstract class InterfaceCommonTest extends BaseTest {
    /**
     * 数据库连接定义
     */
    public static String DB_URL = "jdbc:mysql://localhost:3306/trust?user=root&password=root";
    /**
     * snapshot service
     */
    @Autowired SnapshotService snapshotService;

    /**
     * Before.
     */
    @BeforeMethod public void before() {
        snapshotService.startTransaction();
    }

    /**
     * After.
     */
    @AfterMethod public void after() {
        snapshotService.destroy();
    }

    /**
     * 测试数据根路径
     *
     * @return provider root path
     */
    protected abstract String getProviderRootPath();

    /**
     * 默认的测试数据源，路径：测试数据根路径+测试方法名
     *
     * @param method the method
     * @return object [ ] [ ]
     */
    @DataProvider public Object[][] defaultProvider(Method method) {
        String name = method.getName();
        String providerPath = getProviderRootPath() + name;
        log.info("[defaultProvider].path:{}", providerPath);
        String filePath = JsonFileUtil.findJsonFile(providerPath);
        HashMap<String, String>[][] arrMap = (HashMap<String, String>[][])JsonFileUtil.jsonFileToArry(filePath);
        return arrMap;
    }

    /**
     * 执行actionHandler, validate、persist都会执行
     *
     * @param param         the param
     * @param actionHandler the action handler
     * @param action        the action
     */
    protected void executeActionHandler(Map<?, ?> param,ActionHandler actionHandler,Action action){
        String assertData = getAssertData(param);
        try {
            actionHandler.validate(makePackContext(action, 1L));
            actionHandler.persist(makePackContext(action, 1L));
        }catch (Exception e){
            log.info("has error:{}",e.getMessage());
            assertEquals(e.getMessage(),assertData);
        }
    }

    /**
     * 获取 测试数据中的 body 对象实体
     *
     * @param <T>   the type parameter
     * @param param the param
     * @param clazz the clazz
     * @return body data
     */
    protected <T> T getBodyData(Map<?, ?> param, Class<T> clazz) {
        String body = String.valueOf(param.get("body"));
        if (StringUtils.isEmpty(body) || "null".equals(body)) {
            return null;
        }
        body = body.replaceAll("\"@type\":\"com.alibaba.fastjson.JSONObject\",","");
        return JSON.parseObject(body, clazz);
    }

    /**
     * 从body中获取action对象实体,同时设置actionType
     *
     * @param <T>            the type parameter
     * @param param          the param
     * @param clazz          the clazz
     * @param actionTypeEnum the action type enum
     * @return action
     */
    protected <T> T getAction(Map<?, ?> param, Class<T> clazz,ActionTypeEnum actionTypeEnum) {
        T data = getBodyData(param,clazz);
        if(data == null){
            return null;
        }
        Action action = (Action)data;
        action.setType(actionTypeEnum);
        action.setIndex(1);
        return (T)action;
    }

    /**
     * 断言
     *
     * @param param the param
     * @return assert data
     */
    protected String getAssertData(Map<?, ?> param) {
        return String.valueOf(param.get("assert"));
    }

    /**
     * package data 的 封装
     *
     * @param action      the action
     * @param blockHeight the block height
     * @return pack context
     * @throws Exception the exception
     */
    protected PackContext makePackContext(Action action, Long blockHeight) throws Exception {
        List<Action> actions = new ArrayList<>();
        actions.add(action);
        CoreTransaction coreTransaction = TestDataMaker.makeCoreTx(actions, 1, InitPolicyEnum.REGISTER);
        SignedTransaction transaction = TestDataMaker.makeSignedTx(coreTransaction);

        Package pack = new Package();
        pack.setHeight(blockHeight);

        List<SignedTransaction> signedTransactions = new ArrayList<>();
        signedTransactions.add(transaction);

        pack.setSignedTxList(signedTransactions);
        pack.setPackageTime(System.currentTimeMillis());
        pack.setStatus(PackageStatusEnum.INIT);

        BlockHeader header = new BlockHeader();
        header.setHeight(blockHeight);
        header.setBlockTime(System.currentTimeMillis());

        Block block = new Block();
        block.setBlockHeader(header);
        block.setSignedTxList(signedTransactions);

        PackContext packContext = new PackContext(pack, block);
        packContext.setCurrentAction(action);
        packContext.setCurrentTransaction(transaction);

        return packContext;
    }
}
