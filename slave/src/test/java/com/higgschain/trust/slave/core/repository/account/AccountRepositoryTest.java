package com.higgschain.trust.slave.core.repository.account;

import com.higgschain.trust.common.dao.RocksUtils;
import com.higgschain.trust.common.utils.ThreadLocalUtils;
import com.higgschain.trust.slave.BaseTest;
import com.higgschain.trust.slave.common.config.InitConfig;
import com.higgschain.trust.slave.model.bo.account.AccountDcRecord;
import com.higgschain.trust.slave.model.bo.account.AccountDetail;
import org.rocksdb.Transaction;
import org.rocksdb.WriteOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * The type Account repository test.
 */
public class AccountRepositoryTest extends BaseTest {
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private InitConfig initConfig;

    /**
     * Test batch insert.
     *
     * @throws Exception the exception
     */
    @Test public void testBatchInsert() throws Exception {

    }

    /**
     * Test batch update.
     *
     * @throws Exception the exception
     */
    @Test public void testBatchUpdate() throws Exception {
    }

    /**
     * Test batch insert account detail.
     *
     * @throws Exception the exception
     */
    @Test public void testBatchInsertAccountDetail() throws Exception {
        List<AccountDetail> accountDetails = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            AccountDetail accountDetail = new AccountDetail();
            accountDetail.setAccountNo("test-account-no-" + System.currentTimeMillis());
            accountDetail.setAfterAmount(new BigDecimal("1.0"));
            accountDetail.setAmount(new BigDecimal("2.0"));
            accountDetail.setBeforeAmount(new BigDecimal("1.0"));
            accountDetail.setBizFlowNo("test-flow-no-" + System.currentTimeMillis());
            accountDetail.setBlockHeight(120L);
            accountDetail.setChangeDirection("credit");
            accountDetail.setDetailNo(1L);
            accountDetail.setRemark("test");
            accountDetails.add(accountDetail);
        }
        Transaction tx = RocksUtils.beginTransaction(new WriteOptions());
        ThreadLocalUtils.putRocksTx(tx);
        accountRepository.batchInsertAccountDetail(accountDetails);
        RocksUtils.txCommit(tx);
        ThreadLocalUtils.clearRocksTx();
    }

    /**
     * Test batch insert dc records.
     *
     * @throws Exception the exception
     */
    @Test public void testBatchInsertDcRecords() throws Exception {
        List<AccountDcRecord> accountDcRecords = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            AccountDcRecord accountDcRecord = new AccountDcRecord();
            accountDcRecord.setAccountNo("test-account-no-" + System.currentTimeMillis());
            accountDcRecord.setAmount(new BigDecimal("1"));
            accountDcRecord.setBizFlowNo("test-flow-no-" + System.currentTimeMillis());
            accountDcRecord.setDcFlag("flag");
        }

        Transaction tx = RocksUtils.beginTransaction(new WriteOptions());
        ThreadLocalUtils.putRocksTx(tx);
        accountRepository.batchInsertDcRecords(accountDcRecords);
        RocksUtils.txCommit(tx);
        ThreadLocalUtils.clearRocksTx();
    }
}