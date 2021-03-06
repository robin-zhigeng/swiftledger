package com.higgschain.trust.slave.dao.rocks.contract;

import com.higgschain.trust.common.dao.RocksBaseDao;
import com.higgschain.trust.common.utils.ThreadLocalUtils;
import com.higgschain.trust.slave.common.enums.SlaveErrorEnum;
import com.higgschain.trust.slave.common.exception.SlaveException;
import com.higgschain.trust.slave.dao.po.contract.AccountContractBindingPO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.rocksdb.Transaction;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * The type Account contract binding rocks dao.
 *
 * @author tangfashuang
 */
@Service
@Slf4j
public class AccountContractBindingRocksDao extends RocksBaseDao<AccountContractBindingPO>{
    @Override protected String getColumnFamilyName() {
        return "accountContractBinding";
    }

    /**
     * Batch insert int.
     *
     * @param list the list
     * @return the int
     */
    public int batchInsert(Collection<AccountContractBindingPO> list) {
        if (CollectionUtils.isEmpty(list)) {
            return 0;
        }

        Transaction tx = ThreadLocalUtils.getRocksTx();
        if (null == tx) {
            log.error("[AccountContractBindingRocksDao.batchInsert] transaction is null");
            throw new SlaveException(SlaveErrorEnum.SLAVE_ROCKS_TRANSACTION_IS_NULL);
        }

        for (AccountContractBindingPO po : list) {
            txPut(tx, po.getHash(), po);
        }
        return list.size();
    }
}
