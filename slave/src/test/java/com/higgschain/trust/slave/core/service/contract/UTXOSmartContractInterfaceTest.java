package com.higgschain.trust.slave.core.service.contract;

import com.higgschain.trust.contract.ExecuteContextData;
import com.higgschain.trust.slave.BaseTest;
import com.higgschain.trust.slave.core.repository.contract.ContractRepository;
import com.higgschain.trust.slave.core.service.action.contract.ContractBaseTest;
import com.higgschain.trust.slave.core.service.snapshot.SnapshotService;
import com.higgschain.trust.slave.core.service.snapshot.agent.ContractSnapshotAgent;
import com.higgschain.trust.slave.model.bo.action.UTXOAction;
import com.higgschain.trust.slave.model.bo.contract.Contract;
import com.higgschain.trust.slave.model.bo.utxo.TxIn;
import com.higgschain.trust.tester.dbunit.DataBaseManager;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;

/**
 * The type Utxo smart contract interface test.
 */
public class UTXOSmartContractInterfaceTest extends BaseTest {

    @Autowired private UTXOSmartContractImpl utxoSmartContract;
    @Autowired private SnapshotService snapshotService;
    @Autowired private ContractSnapshotAgent contractSnapshotAgent;
    @Autowired private ContractRepository contractRepository;

    private String getCodeFromFile(String fileName) {
        String path = String.format("/java/com/higgs/trust/slave/core/service/contract/code/%s", fileName);
        String code = null;
        try {
            code = IOUtils.toString(this.getClass().getResource(path), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return code;
    }

    /**
     * Execute file boolean.
     *
     * @param codeFile the code file
     * @return the boolean
     */
    public boolean executeFile(String codeFile) {
        String address = createContract(codeFile);
        UTXOExecuteContextData contextData = new UTXOExecuteContextData();
        UTXOAction utxoAction = new UTXOAction();
        utxoAction.setInputList(new ArrayList<TxIn>(0));
        contextData.setAction(utxoAction);
        return utxoSmartContract.execute(address, contextData);
    }

    /**
     * Execute file with exception.
     *
     * @param codeFile                 the code file
     * @param expectedException        the expected exception
     * @param expectedExceptionMessage the expected exception message
     */
    public void executeFileWithException(String codeFile, Class expectedException, String expectedExceptionMessage) {
        String address = createContract(codeFile);
        UTXOExecuteContextData contextData = new UTXOExecuteContextData();
        try {
            utxoSmartContract.execute(address, contextData);
        } catch (Exception ex) {
            ex.printStackTrace();
            if (ex.getClass() == expectedException) {
                Assert.assertEquals(ex.getMessage(), expectedExceptionMessage);
                return;
            }
            Assert.fail();
        }
    }

    private String createContract(String filePath) {
        String code = getCodeFromFile(filePath);
        Contract contract = new Contract();
        contract.setBlockHeight(1L);
        contract.setTxId("000000000000000" + System.currentTimeMillis());
        contract.setActionIndex(0);
        contract.setAddress("00000" + System.currentTimeMillis() + System.currentTimeMillis());
        contract.setCode(code);
        contract.setLanguage("javascript");
        contract.setVersion("0.1");
        contract.setCreateTime(new Date());

//        contractRepository.deploy(contract);
        return contract.getAddress();
    }

    /**
     * Clear db.
     */
    @AfterClass
    public void clearDb() {
        DataBaseManager dataBaseManager = new DataBaseManager();
        Connection conn = dataBaseManager.getMysqlConnection(ContractBaseTest.getDbConnectString());
        dataBaseManager.executeDelete("TRUNCATE TABLE contract;TRUNCATE TABLE contract_state", conn);
    }

    /**
     * Test execute code empty.
     */
    @Test
    public void testExecute_code_empty() {
        ExecuteContextData contextData = new UTXOExecuteContextData();
        try {
            utxoSmartContract.execute(null, contextData);
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals(ex.getMessage(), "argument code is empty");
        }
    }

    /**
     * Test execute context data null.
     */
    @Test
    public void testExecute_contextData_null() {
        ExecuteContextData contextData = new UTXOExecuteContextData();
        try {
            utxoSmartContract.execute("function verify() {}", null);
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals(ex.getMessage(), "contextData is null");
        }
    }

    /**
     * Test execute process type null.
     */
    @Test
    public void testExecute_processType_null() {
        ExecuteContextData contextData = new UTXOExecuteContextData();
        try {
            utxoSmartContract.execute("function verify() {}", contextData);
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals(ex.getMessage(), "processType is null");
        }
    }

    /**
     * Test execute validate.
     */
    @Test
    public void testExecute_Validate() {
        snapshotService.startTransaction();
        boolean result = executeFile("utxo_normal_return_true.js");
        Assert.assertTrue(result);

        result = executeFile("utxo_normal_retrun_false.js");
        Assert.assertFalse(result);

        executeFileWithException("utxo_exception_return_object.js",  ClassCastException.class, "jdk.nashorn.api.scripting.ScriptObjectMirror cannot be cast to java.lang.Boolean");
        executeFileWithException("utxo_exception_return_number.js", ClassCastException.class, "java.lang.Integer cannot be cast to java.lang.Boolean");
        snapshotService.commit();
    }

    /**
     * Test execute persist.
     */
    @Test
    public void testExecute_Persist() {
        boolean result = executeFile("utxo_normal_return_true.js");
        Assert.assertTrue(result);

        result = executeFile("utxo_normal_retrun_false.js");
        Assert.assertFalse(result);

        executeFileWithException("utxo_exception_return_object.js", ClassCastException.class, "jdk.nashorn.api.scripting.ScriptObjectMirror cannot be cast to java.lang.Boolean");
        executeFileWithException("utxo_exception_return_number.js", ClassCastException.class, "java.lang.Integer cannot be cast to java.lang.Boolean");
    }
}