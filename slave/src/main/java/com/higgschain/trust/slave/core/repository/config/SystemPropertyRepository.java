package com.higgschain.trust.slave.core.repository.config;

import com.higgschain.trust.common.utils.BeanConvertor;
import com.higgschain.trust.slave.common.config.InitConfig;
import com.higgschain.trust.slave.dao.mysql.config.SystemPropertyDao;
import com.higgschain.trust.slave.dao.po.config.SystemPropertyPO;
import com.higgschain.trust.slave.dao.rocks.config.SystemPropertyRocksDao;
import com.higgschain.trust.slave.model.bo.config.SystemProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * System  Property  repository
 *
 * @author lingchao
 * @create 2018年06月27日15 :58
 */
@Service
@Slf4j
public class SystemPropertyRepository {
    @Autowired
    private SystemPropertyDao systemPropertyDao;

    @Autowired
    private SystemPropertyRocksDao systemPropertyRocksDao;

    @Autowired
    private InitConfig initConfig;

    /**
     * query system property by key
     *
     * @param key the key
     * @return system property
     */
    public SystemProperty queryByKey(String key) {
        SystemPropertyPO systemPropertyPO;
        if (initConfig.isUseMySQL()) {
            systemPropertyPO = systemPropertyDao.queryByKey(key);
        } else {
            systemPropertyPO = systemPropertyRocksDao.get(key);
        }
        return BeanConvertor.convertBean(systemPropertyPO, SystemProperty.class);
    }

    /**
     * add property into db
     *
     * @param key   the key
     * @param value the value
     * @param desc  the desc
     * @return
     */
    public void add(String key, String value, String desc){
        SystemPropertyPO systemPropertyPO = new SystemPropertyPO();
        systemPropertyPO.setKey(key);
        systemPropertyPO.setValue(value);
        systemPropertyPO.setDesc(desc);

        if (initConfig.isUseMySQL()) {
            systemPropertyDao.add(systemPropertyPO);
        } else {
            systemPropertyPO.setCreateTime(new Date());
            systemPropertyRocksDao.put(key, systemPropertyPO);
        }
    }

    /**
     * add property into db
     *
     * @param key   the key
     * @param value the value
     * @param desc  the desc
     * @return int
     */
    public int update(String key, String value, String desc){
        if (initConfig.isUseMySQL()) {
            return systemPropertyDao.update(key, value , desc);
        } else {
            SystemPropertyPO po = systemPropertyRocksDao.get(key);
            if (null != po) {
                po.setValue(value);
                po.setDesc(desc);
                po.setUpdateTime(new Date());
                systemPropertyRocksDao.put(key, po);
                return 1;
            }
            return 0;
        }
    }

    /**
     * Save with transaction.
     *
     * @param key   the key
     * @param value the value
     * @param desc  the desc
     */
    public void saveWithTransaction(String key, String value, String desc) {
        systemPropertyRocksDao.saveWithTransaction(key, value, desc);
    }
}
