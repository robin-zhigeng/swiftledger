package com.higgschain.trust.slave.dao.mysql.dataIdentity;

import com.higgschain.trust.common.mybatis.BaseDao;
import com.higgschain.trust.slave.dao.po.DataIdentityPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author liuyu
 * @description data identity DAO
 * @date 2018-03-27
 */
@Mapper public interface DataIdentityDao extends BaseDao<DataIdentityPO> {
    /**
     * query by identity
     *
     * @param identity
     * @return
     */
    DataIdentityPO queryByIdentity(@Param("identity") String identity);

    /**
     * batch insert
     *
     * @param dataIdentityList
     * @return
     */
    int batchInsert(List<DataIdentityPO> dataIdentityList);
}