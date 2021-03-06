package com.higgschain.trust.rs.core.dao.rocks;

import com.higgschain.trust.common.constant.Constant;
import com.higgschain.trust.common.dao.RocksBaseDao;
import com.higgschain.trust.common.utils.ThreadLocalUtils;
import com.higgschain.trust.rs.common.enums.RsCoreErrorEnum;
import com.higgschain.trust.rs.common.exception.RsCoreException;
import com.higgschain.trust.rs.core.api.enums.CoreTxStatusEnum;
import com.higgschain.trust.rs.core.dao.po.CoreTransactionProcessPO;
import com.higgschain.trust.rs.core.vo.RsCoreTxVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.rocksdb.Transaction;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * The type Core tx process rocks dao.
 *
 * @author tangfashuang
 * @desc key : status_index + "_" + txId, value: CoreTransactionProcessPO
 */
@Service
@Slf4j
public class CoreTxProcessRocksDao extends RocksBaseDao<CoreTransactionProcessPO>{
    @Override protected String getColumnFamilyName() {
        return "coreTransactionProcess";
    }

    /**
     * Save with transaction.
     *
     * @param po    the po
     * @param index the index
     */
    public void saveWithTransaction(CoreTransactionProcessPO po, String index) {
        String key = index + Constant.SPLIT_SLASH + po.getTxId();
        if (keyMayExist(key) && null != get(key)) {
            log.error("[CoreTxProcessRocksDao.save] core transaction process is exist, key={}", key);
            throw new RsCoreException(RsCoreErrorEnum.RS_CORE_ROCKS_KEY_ALREADY_EXIST);
        }

        po.setCreateTime(new Date());

        Transaction tx = ThreadLocalUtils.getRocksTx();
        if (null == tx) {
            log.error("[CoreTxProcessRocksDao.saveWithTransaction] transaction is null");
            throw new RsCoreException(RsCoreErrorEnum.RS_CORE_ROCKS_TRANSACTION_IS_NULL);
        }

        txPut(tx, key, po);
    }

    /**
     * Update status.
     *
     * @param txId the tx id
     * @param from the from
     * @param to   the to
     */
    public void updateStatus(String txId, CoreTxStatusEnum from, CoreTxStatusEnum to) {

        Transaction tx = ThreadLocalUtils.getRocksTx();
        if (null == tx) {
            log.error("[CoreTxProcessRocksDao.updateStatus] transaction is null");
            throw new RsCoreException(RsCoreErrorEnum.RS_CORE_ROCKS_TRANSACTION_IS_NULL);
        }

        String key = from.getIndex() + Constant.SPLIT_SLASH + txId;
        //first query
        CoreTransactionProcessPO po = get(key);
        if (null == po) {
            log.error("[CoreTxProcessRocksDao.updateStatus] core transaction process is not exist, key={}", key);
            throw new RsCoreException(RsCoreErrorEnum.RS_CORE_ROCKS_KEY_IS_NOT_EXIST);
        }

        po.setUpdateTime(new Date());
        //set state by 'to'
        po.setStatus(to.getCode());
        String newKey = to.getIndex() + Constant.SPLIT_SLASH + txId;
        //second delete
        txDelete(tx, key);
        //last update(put)
        txPut(tx, newKey, po);
    }

    /**
     * Batch insert.
     *
     * @param poList the po list
     * @param index  the index
     */
    public void batchInsert(List<CoreTransactionProcessPO> poList, String index) {
        if (CollectionUtils.isEmpty(poList)) {
            return;
        }

        Transaction tx = ThreadLocalUtils.getRocksTx();
        if (null == tx) {
            log.error("[CoreTxProcessRocksDao.batchInsert] transaction is null");
            throw new RsCoreException(RsCoreErrorEnum.RS_CORE_ROCKS_TRANSACTION_IS_NULL);
        }

        for (CoreTransactionProcessPO po : poList) {
            String key = index + Constant.SPLIT_SLASH + po.getTxId();
            po.setCreateTime(new Date());
            txPut(tx, key, po);
        }
    }

    /**
     * Batch delete.
     *
     * @param poList     the po list
     * @param statusEnum the status enum
     */
    public void batchDelete(List<RsCoreTxVO> poList, CoreTxStatusEnum statusEnum) {
        if (CollectionUtils.isEmpty(poList)){
            return;
        }

        Transaction tx = ThreadLocalUtils.getRocksTx();
        if (null == tx) {
            log.error("[CoreTxProcessRocksDao.batchUpdate] transaction is null");
            throw new RsCoreException(RsCoreErrorEnum.RS_CORE_ROCKS_TRANSACTION_IS_NULL);
        }

        for (RsCoreTxVO vo : poList) {
            String key = statusEnum.getIndex() + Constant.SPLIT_SLASH + vo.getTxId();
            txDelete(tx, key);
        }
    }

    /**
     * Query by tx id and status core tx status enum.
     *
     * @param txId  the tx id
     * @param index the index
     * @return the core tx status enum
     */
    public CoreTxStatusEnum queryByTxIdAndStatus(String txId, String index) {
        if (!StringUtils.isEmpty(index)) {
            String key = index + Constant.SPLIT_SLASH + txId;
            if (keyMayExist(key) && null != get(key)) {
                return CoreTxStatusEnum.formIndex(index);
            }
        }

        List<String> indexList = CoreTxStatusEnum.getIndexList(null);
        for (String i : indexList) {
            String key = i + Constant.SPLIT_SLASH + txId;
            if (keyMayExist(key) && null != get(key)) {
                return CoreTxStatusEnum.formIndex(i);
            }
        }
        return null;
    }
}
