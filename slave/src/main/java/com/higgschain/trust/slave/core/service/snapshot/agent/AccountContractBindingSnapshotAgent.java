package com.higgschain.trust.slave.core.service.snapshot.agent;

import com.higgschain.trust.common.utils.BeanConvertor;
import com.higgschain.trust.slave.api.enums.SnapshotBizKeyEnum;
import com.higgschain.trust.slave.core.repository.contract.AccountContractBindingRepository;
import com.higgschain.trust.slave.core.service.snapshot.CacheLoader;
import com.higgschain.trust.slave.core.service.snapshot.SnapshotService;
import com.higgschain.trust.slave.dao.po.contract.AccountContractBindingPO;
import com.higgschain.trust.slave.model.bo.BaseBO;
import com.higgschain.trust.slave.model.bo.contract.AccountContractBinding;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.List;

/**
 * the snapshot agent of AccountContractBinding
 *
 * @author duhongming
 * @date 2018 -04-19
 */
@Slf4j
@Component
public class AccountContractBindingSnapshotAgent implements CacheLoader {

    /**
     * The Snapshot.
     */
    @Autowired
    SnapshotService snapshot;
    /**
     * The Repository.
     */
    @Autowired
    AccountContractBindingRepository repository;

    /**
     * check exist binging relationship
     *
     * @param hash the hash
     * @return binding
     */
    public AccountContractBinding getBinding(String hash) {
        AccountContractBinding binding = (AccountContractBinding) snapshot.get(SnapshotBizKeyEnum.ACCOUNT_CONTRACT_BIND, new BindingItemCacheKey(hash));
        return binding;
    }

    /**
     * Put binding.
     *
     * @param binding the binding
     */
    public void putBinding(AccountContractBinding binding) {
         snapshot.insert(SnapshotBizKeyEnum.ACCOUNT_CONTRACT_BIND, new BindingItemCacheKey(binding.getHash()), binding);
    }

    /**
     * Gets list by account.
     *
     * @param accountNo the account no
     * @return the list by account
     */
    public List<AccountContractBinding> getListByAccount(String accountNo) {
        return (List<AccountContractBinding>) snapshot.get(SnapshotBizKeyEnum.ACCOUNT_CONTRACT_BIND, new AccountContractBindingCacheKey(accountNo));
    }

    /**
     * Put.
     *
     * @param accountNo the account no
     * @param bindings  the bindings
     */
    public void put(String accountNo, List<AccountContractBinding> bindings) {
          snapshot.insert(SnapshotBizKeyEnum.ACCOUNT_CONTRACT_BIND, new AccountContractBindingCacheKey(accountNo), bindings);
    }

    /**
     * Put.
     *
     * @param binding the binding
     */
    public void put(AccountContractBinding binding) {
        this.putBinding(binding);
    }

    @Override
    public Object query(Object object) {
        if (object instanceof BindingItemCacheKey) {
            return repository.queryByHash(((BindingItemCacheKey) object).getBindHash());
        }

        if (object instanceof AccountContractBindingCacheKey) {
            String accountNo = ((AccountContractBindingCacheKey) object).getAccountNo();
            List<AccountContractBinding> bindings = repository.queryListByAccountNo(accountNo);
            return bindings;
        }

        log.error("unknow CacheKey object: {}", object.getClass().getName());
        return null;
    }


    /**
     * the method to batchInsert data into db
     *
     * @param insertList
     * @return
     */
    @Override
    public boolean batchInsert(List<Pair<Object, Object>> insertList) {
        List<AccountContractBindingPO> list = new ArrayList<>(insertList.size());
        insertList.forEach(pair -> {
            AccountContractBinding binding = (AccountContractBinding) pair.getRight();
            list.add(BeanConvertor.convertBean(binding, AccountContractBindingPO.class));
        });
        return repository.batchInsert(list);
    }

    /**
     * the method to batchUpdate data into db
     *
     * @param updateList
     * @return
     */
    @Override
    public boolean batchUpdate(List<Pair<Object, Object>> updateList) {
        throw new NotImplementedException();
    }

    /**
     * The type Account contract binding cache key.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountContractBindingCacheKey extends BaseBO {
        private String accountNo;
    }

    /**
     * The type Binding item cache key.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BindingItemCacheKey extends BaseBO {
        private String bindHash;
    }
}
