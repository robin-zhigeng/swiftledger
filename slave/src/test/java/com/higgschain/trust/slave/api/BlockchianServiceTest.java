package com.higgschain.trust.slave.api;

import com.higgschain.trust.slave.BaseTest;
import com.higgschain.trust.slave.model.bo.utxo.TxIn;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.util.List;

/**
 * Created by liuyu on 18/1/2.
 */
public class BlockchianServiceTest extends BaseTest {
    @Autowired
    private BlockChainService blockChainService;

    /**
     * Test save.
     */
    @Test
    public void testSave() {
    }

    /**
     * Test remove.
     */
    @Test
    public void testRemove() {
    }

    /**
     * Test query system property by key.
     */
    @Test
    public void testQuerySystemPropertyByKey() {
        System.out.println("CHAIN_OWNER:" + blockChainService.querySystemPropertyByKey("CHAIN_OWNER"));
        System.out.println("UTXO_CONTRACT_ADDRESS:" + blockChainService.querySystemPropertyByKey("UTXO_CONTRACT_ADDRESS"));
    }

    /**
     * Test query utxo list.
     */
    @Test
    public void testQueryUTXOList() {
        List<TxIn> inputList = Lists.newArrayList();
        TxIn  txIn = new TxIn();
        txIn.setTxId("123123");
        txIn.setActionIndex(1);
        txIn.setIndex(0);
        inputList.add(txIn);

        System.out.println("queryUTXOList:" + blockChainService.queryUTXOList(inputList));

        System.out.println("getUTXOActionType:" + blockChainService.getUTXOActionType("NORMAL"));
    }
}
