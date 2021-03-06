package com.higgschain.trust.rs.core.dao.rocks;

import com.higgschain.trust.common.dao.RocksBaseDao;
import com.higgschain.trust.rs.common.enums.RsCoreErrorEnum;
import com.higgschain.trust.rs.common.exception.RsCoreException;
import com.higgschain.trust.rs.core.dao.po.BizTypePO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * The type Biz type rocks dao.
 *
 * @author tangfashuang
 * @desc key : policyId, value: bizType
 */
@Slf4j
@Service
public class BizTypeRocksDao extends RocksBaseDao<BizTypePO>{
    @Override protected String getColumnFamilyName() {
        return "bizType";
    }

    /**
     * Add.
     *
     * @param bizTypePO the biz type po
     */
    public void add(BizTypePO bizTypePO) {
        String key = bizTypePO.getPolicyId();
        if (keyMayExist(key) && null != get(key)) {
            throw new RsCoreException(RsCoreErrorEnum.RS_CORE_ROCKS_KEY_ALREADY_EXIST);
        }

        bizTypePO.setCreateTime(new Date());
        put(key, bizTypePO);
    }

    /**
     * Update.
     *
     * @param policyId the policy id
     * @param bizType  the biz type
     */
    public void update(String policyId, String bizType) {
        BizTypePO bizTypePO = get(policyId);
        if (null == bizTypePO) {
            throw new RsCoreException(RsCoreErrorEnum.RS_CORE_ROCKS_KEY_IS_NOT_EXIST);
        }

        bizTypePO.setBizType(bizType);
        put(policyId, bizTypePO);
    }
}
