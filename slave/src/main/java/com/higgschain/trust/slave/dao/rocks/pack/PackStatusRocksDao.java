package com.higgschain.trust.slave.dao.rocks.pack;

import com.higgschain.trust.common.constant.Constant;
import com.higgschain.trust.common.dao.RocksBaseDao;
import com.higgschain.trust.common.utils.ThreadLocalUtils;
import com.higgschain.trust.slave.common.enums.SlaveErrorEnum;
import com.higgschain.trust.slave.common.exception.SlaveException;
import com.higgschain.trust.slave.model.enums.biz.PackageStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.rocksdb.Transaction;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.List;

/**
 * The type Pack status rocks dao.
 *
 * @author tangfashuang
 * @desc key : status_height, value: height
 */
@Service
@Slf4j
public class PackStatusRocksDao extends RocksBaseDao<Long> {
    private static final int LOAD_LIMIT = 30;

    @Override protected String getColumnFamilyName() {
        return "packageStatus";
    }

    /**
     * Gets max height by status.
     *
     * @param status the status
     * @return the max height by status
     */
    public Long getMaxHeightByStatus(String status) {

        List<String> indexList = PackageStatusEnum.getIndexs(status);
        for (String index : indexList) {
            if (!StringUtils.isEmpty(queryFirstKey(index))) {
                return queryForPrev(index);
            }
        }
        return queryLastValueWithPrefix(status);
    }

    /**
     * Gets min height by status.
     *
     * @param status the status
     * @return the min height by status
     */
    public Long getMinHeightByStatus(String status) {
        return queryFirstValueByPrefix(status);
    }

    /**
     * Save.
     *
     * @param height the height
     * @param status the status
     */
    public void save(Long height, String status) {

        DecimalFormat df = new DecimalFormat(Constant.PACK_STATUS_HEIGHT_FORMAT);
        String key = status + Constant.SPLIT_SLASH + df.format(height);
        if (keyMayExist(key) && null != get(key)) {
            log.error("[PackStatusRocksDao.save] height and status is exist, key={}", key);
            throw new SlaveException(SlaveErrorEnum.SLAVE_ROCKS_KEY_ALREADY_EXIST);
        }

        Transaction tx = ThreadLocalUtils.getRocksTx();
        if (null == tx) {
            log.error("[PackStatusRocksDao.save] transaction is null");
            throw new SlaveException(SlaveErrorEnum.SLAVE_ROCKS_TRANSACTION_IS_NULL);
        }

        txPut(tx, key, height);
    }

    /**
     * Batch delete.
     *
     * @param height the height
     * @param status the status
     */
    public void batchDelete(Long height, String status) {
        Transaction tx = ThreadLocalUtils.getRocksTx();
        if (null == tx) {
            log.error("[PackStatusRocksDao.batchDelete] transaction is null");
            throw new SlaveException(SlaveErrorEnum.SLAVE_ROCKS_TRANSACTION_IS_NULL);
        }

        DecimalFormat df = new DecimalFormat(Constant.PACK_STATUS_HEIGHT_FORMAT);
        String key = status + Constant.SPLIT_SLASH + df.format(height);

        txDelete(tx, key);
    }

    /**
     * Update.
     *
     * @param height the height
     * @param from   the from
     * @param to     the to
     */
    public void update(Long height, String from , String to) {
        Transaction tx = ThreadLocalUtils.getRocksTx();
        if (null == tx) {
            log.error("[PackStatusRocksDao.update] transaction is null");
            throw new SlaveException(SlaveErrorEnum.SLAVE_ROCKS_TRANSACTION_IS_NULL);
        }

        DecimalFormat df = new DecimalFormat(Constant.PACK_STATUS_HEIGHT_FORMAT);
        String key = from + Constant.SPLIT_SLASH + df.format(height);
        if (!keyMayExist(key) && null != get(key)) {
            log.error("[PackStatusRocksDao.update] height and status is not exist, key={}", key);
            throw new SlaveException(SlaveErrorEnum.SLAVE_ROCKS_KEY_IS_NOT_EXIST);
        }

        //delete
        txDelete(tx, key);

        //put
        String newKey = to + Constant.SPLIT_SLASH + df.format(height);
        txPut(tx, newKey, height);
    }

    /**
     * Gets status by height.
     *
     * @param height the height
     * @return the status by height
     */
    public String getStatusByHeight(Long height) {
        List<String> indexList = PackageStatusEnum.getIndexs(null);
        DecimalFormat df = new DecimalFormat(Constant.PACK_STATUS_HEIGHT_FORMAT);

        for (String index : indexList) {
            String key = index + Constant.SPLIT_SLASH + df.format(height);
            if (keyMayExist(key) && null != get(key)) {
                return PackageStatusEnum.getByIndex(index).getCode();
            }
        }
        return null;
    }

    /**
     * Query height list by height list.
     *
     * @param height the height
     * @return the list
     */
    public List<Long> queryHeightListByHeight(Long height) {
        DecimalFormat df = new DecimalFormat(Constant.PACK_STATUS_HEIGHT_FORMAT);
        String position = PackageStatusEnum.RECEIVED.getIndex() + Constant.SPLIT_SLASH + df.format(height);
        return queryByPrefix(PackageStatusEnum.RECEIVED.getIndex(), LOAD_LIMIT, position);
    }

    /**
     * Query by status and less than height list.
     *
     * @param index  the index
     * @param height the height
     * @return the list
     */
    public List<Long> queryByStatusAndLessThanHeight(String index, Long height) {
        DecimalFormat df = new DecimalFormat(Constant.PACK_STATUS_HEIGHT_FORMAT);
        String position = index + Constant.SPLIT_SLASH + df.format(height);
        return queryLessThanByPrefixAndPosition(index, position);
    }

    /**
     * Get block heights by status list.
     *
     * @param status the status
     * @return the list
     */
    public List<Long> getBlockHeightsByStatus(String status){
        return queryByPrefix(status);
    }

}
