package com.higgschain.trust.slave.core.repository.contract;

import com.higgschain.trust.common.utils.BeanConvertor;
import com.higgschain.trust.slave.common.config.InitConfig;
import com.higgschain.trust.slave.dao.mysql.contract.AccountContractBindingDao;
import com.higgschain.trust.slave.dao.po.contract.AccountContractBindingPO;
import com.higgschain.trust.slave.dao.rocks.contract.AccountContractBindingRocksDao;
import com.higgschain.trust.slave.model.bo.contract.AccountContractBinding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * the repository of AccountContractBinding
 *
 * @author duhongming
 * @date 2018 -04-19 //TODO 此表可删除，暂时不考虑rocksdb的实现
 */
@Repository @Slf4j public class AccountContractBindingRepository {

    @Autowired private AccountContractBindingDao dao;

    @Autowired private AccountContractBindingRocksDao rocksDao;

    @Autowired private InitConfig initConfig;

    /**
     * Batch insert boolean.
     *
     * @param list the list
     * @return the boolean
     */
    public boolean batchInsert(List<AccountContractBindingPO> list) {
        int result;
        if (initConfig.isUseMySQL()) {
            result = dao.batchInsert(list);
        } else {
            result = rocksDao.batchInsert(list);
            //TODO account_no_bind_hash不再使用
        }
        return result == list.size();
    }

    /**
     * Query list by account no list.
     *
     * @param accountNo the account no
     * @return the list
     */
    public List<AccountContractBinding> queryListByAccountNo(String accountNo) {
        List<AccountContractBindingPO> list = dao.queryListByAccountNo(accountNo);
        List<AccountContractBinding> bindings = BeanConvertor.convertList(list, AccountContractBinding.class);
        return bindings;
    }

    /**
     * Query by hash account contract binding.
     *
     * @param hash the hash
     * @return the account contract binding
     */
    public AccountContractBinding queryByHash(String hash) {
        AccountContractBindingPO po = dao.queryByHash(hash);
        return BeanConvertor.convertBean(po, AccountContractBinding.class);
    }
}
