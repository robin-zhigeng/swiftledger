package com.higgschain.trust.slave.core.service.ca;

import com.higgschain.trust.consensus.config.NodeState;
import com.higgschain.trust.consensus.util.CaKeyLoader;
import com.higgschain.trust.slave.core.repository.ca.CaRepository;
import com.higgschain.trust.slave.core.repository.config.ConfigRepository;
import com.higgschain.trust.slave.model.bo.config.Config;
import com.higgschain.trust.slave.model.enums.UsageEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The type Ca key loader.
 */
@Component @Slf4j public class CaKeyLoaderImpl implements CaKeyLoader {

    @Autowired private CaRepository caRepository;
    @Autowired private ConfigRepository configRepository;
    @Autowired private NodeState nodeState;

    /**
     * @param
     * @return
     * @desc acquire pubKeyForConsensus by nodeName
     */
    @Override public String loadPublicKey(String nodeName) throws Exception {
        return caRepository.getCaForConsensus(nodeName).getPubKey();
    }

    /**
     * @param
     * @return
     * @desc acquire current node priKey
     */
    @Override public String loadPrivateKey() throws Exception {
        List<Config> list = configRepository.getConfig(new Config(nodeState.getNodeName()));
        for (Config config : list) {
            if (config.getUsage().equals(UsageEnum.CONSENSUS.getCode())) {
                return config.getPriKey();
            }
        }
        log.warn("acquire priKey error, node name = {}", nodeState.getNodeName());
        return null;
    }
}
