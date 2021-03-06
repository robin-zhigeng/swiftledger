package com.higgschain.trust.rs.core.dao;

import com.higgschain.trust.IntegrateBaseTest;
import com.higgschain.trust.rs.core.dao.po.CoreTransactionPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The type Core tx jdbc dao test.
 *
 * @author liuyu
 * @description
 * @date 2018 -08-01
 */
public class CoreTxJDBCDaoTest extends IntegrateBaseTest {
    /**
     * The Core tx jdbc dao.
     */
    @Autowired
    CoreTxJDBCDao coreTxJDBCDao;

    /**
     * Test insert.
     */
    @Test
    public void testInsert(){
        List<CoreTransactionPO> list = new ArrayList<>();
        for(int i=0;i<10;i++){
            CoreTransactionPO po = new CoreTransactionPO();
            po.setTxId("tx_id_" + i);
            po.setPolicyId("ppp-");
            po.setBizModel("{}");
            po.setSendTime(new Date());
            po.setLockTime(new Date());
         //   po.setExecuteResult("SUCCESS");
       //     po.setErrorMsg("aa");
         //   po.setErrorCode("000");
            po.setActionDatas("[]");
            po.setBlockHeight(1L + i);
            po.setCreateTime(new Date());
            po.setSender("sender-" + i);
            po.setVersion("v1.0.0");
            po.setSignDatas("[]");
            list.add(po);
        }
        coreTxJDBCDao.batchInsert(list);
    }

    /**
     * Test update.
     */
    @Test
    public void testUpdate(){
        List<CoreTransactionPO> list = new ArrayList<>();
        for(int i=0;i<10;i++){
            CoreTransactionPO po = new CoreTransactionPO();
            po.setTxId("tx_id_" + i);
            po.setPolicyId("ppp-");
            po.setBizModel("{}");
            po.setSendTime(new Date());
            po.setLockTime(new Date());
            po.setExecuteResult( i % 2 == 0 ? "SUCCESS" : "FAIL");
            po.setErrorMsg(i % 2 == 0 ? "bb" : "cc");
            po.setErrorCode(i % 2 == 0 ? "111" : "xxx");
            po.setActionDatas("[]");
            po.setBlockHeight(1L + i);
            po.setCreateTime(new Date());
            po.setSender("sender-" + i);
            po.setVersion("v1.0.0");
            po.setSignDatas("[]");
            list.add(po);
        }
        coreTxJDBCDao.batchUpdate(list,5L);
    }
}
