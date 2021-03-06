package com.higgschain.trust.slave.dao.rocks.pack;

import com.higgschain.trust.common.dao.RocksBaseDao;
import com.higgschain.trust.common.utils.ThreadLocalUtils;
import com.higgschain.trust.slave.common.enums.SlaveErrorEnum;
import com.higgschain.trust.slave.common.exception.SlaveException;
import com.higgschain.trust.slave.dao.po.pack.PackagePO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.rocksdb.Transaction;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * The type Pack rocks dao.
 *
 * @author tangfashuang
 * @desc key : height, value: packagePO
 */
@Service
@Slf4j
public class PackRocksDao extends RocksBaseDao<PackagePO> {
    @Override protected String getColumnFamilyName() {
        return "package";
    }

    /**
     * Save.
     *
     * @param po the po
     */
    public void save(PackagePO po) {

        Transaction tx = ThreadLocalUtils.getRocksTx();
        if (null == tx) {
            log.error("[PackRocksDao.save] transaction is null");
            throw new SlaveException(SlaveErrorEnum.SLAVE_ROCKS_TRANSACTION_IS_NULL);
        }

        String height = String.valueOf(po.getHeight());
        if (keyMayExist(height) && null != get(height)) {
            log.error("[PackRocksDao.save] package is already exist. height={}", height);
            throw new SlaveException(SlaveErrorEnum.SLAVE_ROCKS_KEY_ALREADY_EXIST);
        }
        po.setCreateTime(new Date());

        txPut(tx, height, po);
    }

    /**
     * Update status.
     *
     * @param height the height
     * @param from   the from
     * @param to     the to
     */
    public void updateStatus(Long height, String from, String to) {
        Transaction tx = ThreadLocalUtils.getRocksTx();
        if (null == tx) {
            log.error("[PackRocksDao.updateStatus] transaction is null");
            throw new SlaveException(SlaveErrorEnum.SLAVE_ROCKS_TRANSACTION_IS_NULL);
        }

        String heightStr = String.valueOf(height);
        PackagePO po = get(heightStr);
        if (null == po) {
            log.error("[PackageRocksDao.updateStatus] package is not exist, height={}", height);
            throw new SlaveException(SlaveErrorEnum.SLAVE_ROCKS_KEY_IS_NOT_EXIST);
        }

        if (!StringUtils.equals(po.getStatus(), from)) {
            log.error("[PackRocksDao.updateStatus] status is not equals, po.status = {}, from = {}", po.getStatus(), from);
            throw new SlaveException(SlaveErrorEnum.SLAVE_PACKAGE_UPDATE_STATUS_ERROR);
        }

        po.setUpdateTime(new Date());
        po.setStatus(to);

        txPut(tx, heightStr, po);
    }

    /**
     * Query height list by height list.
     *
     * @param packHeights the pack heights
     * @return the list
     */
    public List<Long> queryHeightListByHeight(List<String> packHeights) {
        List<Long> heights = new ArrayList<>();
        Map<String, PackagePO> resultMap = multiGet(packHeights);
        if (!MapUtils.isEmpty(resultMap)) {
            for (String key : resultMap.keySet()) {
                if (!StringUtils.isEmpty(key)) {
                    heights.add(Long.parseLong(key));
                }
            }
        }

        //sort height asc
        Collections.sort(heights);
        return heights;
    }

    /**
     * Batch delete.
     *
     * @param h the h
     */
    public void batchDelete(Long h) {
        Transaction tx = ThreadLocalUtils.getRocksTx();
        if (null == tx) {
            log.error("[PackStatusRocksDao.batchDelete] transaction is null");
            throw new SlaveException(SlaveErrorEnum.SLAVE_ROCKS_TRANSACTION_IS_NULL);
        }

        txDelete(tx, String.valueOf(h));
    }
}
