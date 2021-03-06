package com.higgschain.trust.slave.core.service.datahandler.manage;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.higgschain.trust.slave.api.vo.SystemPropertyVO;
import com.higgschain.trust.slave.core.repository.config.SystemPropertyRepository;
import com.higgschain.trust.slave.model.bo.config.SystemProperty;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * SystemProperty snapshot handler
 *
 * @author lingchao
 * @create 2018年06月29日10 :51
 */
@Slf4j
@Service
public class SystemPropertyHandler {
    @Autowired
    private SystemPropertyRepository systemPropertyRepository;

    /**
     * systemProperty loading cache
     */
    LoadingCache<String, SystemProperty> systemPropertyCache = CacheBuilder.newBuilder().initialCapacity(10).maximumSize(500).refreshAfterWrite(10, TimeUnit.MINUTES).build(new CacheLoader<String, SystemProperty>() {
        @Override
        public SystemProperty load(String key) throws Exception {
            log.info("get system property of key :{} from db", key);
            return systemPropertyRepository.queryByKey(key);
        }
    });

    /**
     * query System Property by key
     *
     * @param key the key
     * @return system property vo
     */
    public SystemPropertyVO querySystemPropertyByKey(String key) {
        if (StringUtils.isBlank(key)) {
            return null;
        }
        SystemProperty systemProperty = null;
        try {
            systemProperty = systemPropertyCache.get(key);
        } catch (Throwable e) {
            log.info("Get no data from systemPropertyCache by key:{}", key);
        }
        SystemPropertyVO systemPropertyVO = null;
        if (null != systemProperty) {
            systemPropertyVO = new SystemPropertyVO();
            BeanUtils.copyProperties(systemProperty, systemPropertyVO);
        }
        return systemPropertyVO;
    }

    /**
     * query System Property by key  for command
     *
     * @param key the key
     * @return string
     */
    public String get(String key) {
        if (StringUtils.isBlank(key)) {
            return "Key can not be null!";
        }
        SystemPropertyVO systemPropertyVO = querySystemPropertyByKey(key);
        if (null == systemPropertyVO) {
            return "There is no system property value for key =" + key;
        }
        return "key = " + key + " value = " + systemPropertyVO.getValue();
    }

    /**
     * set system property ,it may be add or update
     *
     * @param key   the key
     * @param value the value
     * @param desc  the desc
     * @return string
     */
    public String set(String key, String value, String desc) {

        //check arguments
        if (StringUtils.isBlank(key) || StringUtils.isBlank(value)) {
            log.error("Key and value can not be null. key = {},value = {}", key, value);
            return "Set property into system failed! Key or value can not be null. key = " + key + " ,value = " + value;
        }

        //set data into DB
        try {
            //check data whether in db
            SystemPropertyVO systemPropertyVO = querySystemPropertyByKey(key);
            if (null != systemPropertyVO) {
                //update property
                systemPropertyRepository.update(key, value, desc);
                //invalidate cache value
                systemPropertyCache.invalidate(key);
                return "Set property into system success! key= " + key + "  value = " + value;
            }
            systemPropertyRepository.add(key, value, desc);
        } catch (Throwable e) {
            log.error("Set property into system failed!", e);
            return "Set property into system failed!" + e;
        }
        return "Set property into system success! key= " + key + "  value = " + value;
    }

}
