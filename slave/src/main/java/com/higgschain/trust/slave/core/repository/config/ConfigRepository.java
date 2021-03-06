package com.higgschain.trust.slave.core.repository.config;

import com.higgschain.trust.slave.common.config.InitConfig;
import com.higgschain.trust.slave.common.enums.SlaveErrorEnum;
import com.higgschain.trust.slave.common.exception.SlaveException;
import com.higgschain.trust.slave.dao.mysql.config.ConfigDao;
import com.higgschain.trust.slave.dao.mysql.config.ConfigJDBCDao;
import com.higgschain.trust.slave.dao.po.config.ConfigPO;
import com.higgschain.trust.slave.dao.rocks.config.ConfigRocksDao;
import com.higgschain.trust.slave.model.bo.config.Config;
import com.higgschain.trust.slave.model.enums.UsageEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.util.LinkedList;
import java.util.List;

/**
 * The type Config repository.
 *
 * @author WangQuanzhou
 * @desc TODO
 * @date 2018 /6/5 16:11
 */
@Repository @Slf4j public class ConfigRepository {

    @Autowired private ConfigDao configDao;
    @Autowired private ConfigRocksDao configRocksDao;
    @Autowired private InitConfig initConfig;
    @Autowired private ConfigJDBCDao configJDBCDao;

    /**
     * Insert config.
     *
     * @param config the config
     * @return
     * @desc insert config into db
     */
    public void insertConfig(Config config) {
        ConfigPO configPO = new ConfigPO();
        BeanUtils.copyProperties(config, configPO);
        if (initConfig.isUseMySQL()) {
            configDao.insertConfig(configPO);
        } else {
            configRocksDao.save(configPO);
        }
    }

    /**
     * Update config.
     *
     * @param config the config
     * @return
     * @desc update config information
     */
    public void updateConfig(Config config) {
        ConfigPO configPO = new ConfigPO();
        BeanUtils.copyProperties(config, configPO);
        if (initConfig.isUseMySQL()) {
            configDao.updateConfig(configPO);
        } else {
            configRocksDao.update(configPO);
        }
    }

    /**
     * Gets config.
     *
     * @param config the config
     * @return List<ConfigPO> config
     * @desc get config information by nodeName and usage(if needed)
     */
    public List<Config> getConfig(Config config) {
        ConfigPO configPO = new ConfigPO();
        BeanUtils.copyProperties(config, configPO);

        List<ConfigPO> list;
        if (initConfig.isUseMySQL()) {
            list = configDao.getConfig(configPO);
        } else {
            list = configRocksDao.getConfig(config.getNodeName(), config.getUsage());
        }

        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        List<Config> configList = new LinkedList<>();
        for (ConfigPO configPO1 : list) {
            Config config1 = new Config();
            BeanUtils.copyProperties(configPO1, config1);
            configList.add(config1);
        }
        return configList;
    }

    /**
     * Gets biz config.
     *
     * @param user the user
     * @return the biz config
     */
    public Config getBizConfig(String user) {
        return getConfig(user, UsageEnum.BIZ);
    }

    /**
     * get config by nodeName and usage
     *
     * @param user      the user
     * @param usageEnum the usage enum
     * @return config
     */
    public Config getConfig(String user,UsageEnum usageEnum) {
        ConfigPO configPO = new ConfigPO();
        configPO.setNodeName(user);
        configPO.setUsage(usageEnum.getCode());
        Config config = new Config();
        if (initConfig.isUseMySQL()) {
            List<ConfigPO> list = configDao.getConfig(configPO);
            if (CollectionUtils.isEmpty(list)) {
                return null;
            }
            if (list.size() > 1) {
                throw new SlaveException(SlaveErrorEnum.SLAVE_CA_UPDATE_ERROR, "more than one pair of pub/priKey");
            }
            BeanUtils.copyProperties(list.get(0), config);
        } else {
            BeanUtils.copyProperties(configRocksDao.get(user + "_" + UsageEnum.BIZ.getCode()), config);
        }
        return config;
    }

    /**
     * batch insert
     *
     * @param configPOList the config po list
     * @return boolean
     */
    public boolean batchInsert(List<ConfigPO> configPOList) {
        int affectRows;
        if (initConfig.isUseMySQL()) {
            try {
                affectRows = configDao.batchInsert(configPOList);
            } catch (DuplicateKeyException e) {
                log.error("batch insert config fail, because there is DuplicateKeyException for configPOList:", configPOList);
                throw new SlaveException(SlaveErrorEnum.SLAVE_IDEMPOTENT);
            }
        } else {
            affectRows = configRocksDao.batchInsert(configPOList);
        }
        return affectRows == configPOList.size();
    }

    /**
     * batch update
     *
     * @param configPOList the config po list
     * @return boolean
     */
    public boolean batchUpdate(List<ConfigPO> configPOList) {
        if (initConfig.isUseMySQL()) {
            return configPOList.size() == configDao.batchUpdate(configPOList);
        }
        return configPOList.size() == configRocksDao.batchInsert(configPOList);
    }

    /**
     * Batch cancel.
     *
     * @param nodes the nodes
     */
    public void batchCancel(List<String> nodes) {
        if (initConfig.isUseMySQL()) {
            configJDBCDao.batchCancel(nodes);
        } else {
            configRocksDao.batchCancel(nodes);
        }
    }

    /**
     * Batch enable.
     *
     * @param nodes the nodes
     */
    public void batchEnable(List<String> nodes) {
        if (initConfig.isUseMySQL()) {
            configJDBCDao.batchEnable(nodes);
        } else {
            configRocksDao.batchEnable(nodes);
        }
    }
}
