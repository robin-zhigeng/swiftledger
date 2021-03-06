package com.higgschain.trust.slave.core.repository.config;

import com.higgschain.trust.slave.common.config.InitConfig;
import com.higgschain.trust.slave.common.enums.SlaveErrorEnum;
import com.higgschain.trust.slave.common.exception.SlaveException;
import com.higgschain.trust.slave.dao.mysql.config.ClusterNodeDao;
import com.higgschain.trust.slave.dao.po.config.ClusterNodePO;
import com.higgschain.trust.slave.dao.rocks.config.ClusterNodeRocksDao;
import com.higgschain.trust.slave.model.bo.config.ClusterNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.util.LinkedList;
import java.util.List;

/**
 * The type Cluster node repository.
 *
 * @author WangQuanzhou
 * @desc TODO
 * @date 2018 /6/5 16:11
 */
@Repository @Slf4j public class ClusterNodeRepository {

    @Autowired private ClusterNodeDao clusterNodeDao;
    @Autowired private ClusterNodeRocksDao clusterNodeRocksDao;
    @Autowired private InitConfig initConfig;

    /**
     * Insert cluster node.
     *
     * @param clusterNode the cluster node
     * @return
     * @desc insert clusterNode information into db
     */
    public void insertClusterNode(ClusterNode clusterNode) {
        ClusterNodePO clusterNodePO = new ClusterNodePO();
        BeanUtils.copyProperties(clusterNode, clusterNodePO);

        if (initConfig.isUseMySQL()) {
            clusterNodeDao.insertClusterNode(clusterNodePO);
        } else {
            clusterNodeRocksDao.saveWithTransaction(clusterNodePO);
        }
    }

    /**
     * Gets cluster node.
     *
     * @param nodeName the node name
     * @return ClusterConfigPO cluster node
     * @desc get clusterNode by node name
     */
    public ClusterNode getClusterNode(String nodeName) {
        ClusterNodePO clusterNodePO;
        if (initConfig.isUseMySQL()) {
            clusterNodePO = clusterNodeDao.getClusterNode(nodeName);
        } else {
            clusterNodePO = clusterNodeRocksDao.get(nodeName);
        }
        if (null == clusterNodePO) {
            return null;
        }
        ClusterNode clusterNode = new ClusterNode();
        BeanUtils.copyProperties(clusterNodePO, clusterNode);
        return clusterNode;
    }

    /**
     * batch insert
     *
     * @param clusterNodePOList the cluster node po list
     * @return boolean
     */
    public boolean batchInsert(List<ClusterNodePO> clusterNodePOList) {
        int affectRows;
        if (initConfig.isUseMySQL()) {
            try {
                affectRows = clusterNodeDao.batchInsert(clusterNodePOList);
            } catch (DuplicateKeyException e) {
                log.error("batch insert clusterNode fail, because there is DuplicateKeyException for clusterNodePOList:",
                    clusterNodePOList);
                throw new SlaveException(SlaveErrorEnum.SLAVE_IDEMPOTENT);
            }
        } else {
            affectRows = clusterNodeRocksDao.batchInsert(clusterNodePOList);
        }
        return affectRows == clusterNodePOList.size();
    }

    /**
     * batch update
     *
     * @param clusterNodePOList the cluster node po list
     * @return boolean
     */
    public boolean batchUpdate(List<ClusterNodePO> clusterNodePOList) {
        if (initConfig.isUseMySQL()) {
            return clusterNodePOList.size() == clusterNodeDao.batchUpdate(clusterNodePOList);
        }
        return clusterNodePOList.size() == clusterNodeRocksDao.batchInsert(clusterNodePOList);
    }

    /**
     * Gets all cluster nodes.
     *
     * @param
     * @return all cluster nodes
     * @desc acquire all cluster nodes
     */
    public List<ClusterNode> getAllClusterNodes() {
        List<ClusterNodePO> list;
        if (initConfig.isUseMySQL()) {
            list = clusterNodeDao.getAllClusterNodes();
        } else {
            list = clusterNodeRocksDao.queryAll();
        }

        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        List<ClusterNode> nodeList = new LinkedList<>();
        for (ClusterNodePO clusterNodePO : list) {
            ClusterNode clusterNode = new ClusterNode();
            BeanUtils.copyProperties(clusterNodePO, clusterNode);
            nodeList.add(clusterNode);
        }
        return nodeList;
    }
}
