package com.higgschain.trust.slave.common.json;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.higgschain.trust.consensus.p2pvalid.core.ResponseCommand;
import com.higgschain.trust.slave.core.service.consensus.view.ValidClusterViewCmd;
import com.higgschain.trust.slave.model.bo.consensus.ValidBlockHeaderCmd;
import com.higgschain.trust.slave.model.bo.consensus.ValidClusterHeightCmd;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * The type Response command json deserializer.
 *
 * @author duhongming
 * @date 2018 /8/3
 */
@Slf4j
public class ResponseCommandJsonDeserializer implements ObjectDeserializer {

    /**
     * The Type map.
     */
    static Map<String, Type> typeMap = new HashMap<>();

    static {
        typeMap.put(ValidBlockHeaderCmd.class.getSimpleName(), ValidBlockHeaderCmd.class);
        typeMap.put(ValidClusterHeightCmd.class.getSimpleName(), ValidClusterHeightCmd.class);
        typeMap.put(ValidClusterViewCmd.class.getSimpleName(), ValidClusterViewCmd.class);
        try {
            Class clazz = Class.forName("com.higgschain.trust.config.master.command.ChangeMasterVerifyResponseCmd1");
            typeMap.put(clazz.getSimpleName(), clazz);
        } catch (ClassNotFoundException e) {
        }

    }

    @Override
    public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        Object obj = parser.parse();
        if (obj == null) {
            return null;
        }
        if(obj instanceof ResponseCommand) {
            return (T) obj;
        }
        JSONObject jsonObject = (JSONObject) obj;
        String cmdName = jsonObject.getString("cmdName");
        if (StringUtils.isEmpty(cmdName)) {
            log.error("cmdName is empty, {}", ((JSONObject) obj).toJSONString());
            return null;
        }

        jsonObject.remove("@type");
        Type realType = typeMap.get(cmdName);
        if (realType == null) {
            log.error("cmdName is invalid: {}", cmdName);
        }
        return (T) JSON.parseObject(jsonObject.toJSONString(), realType);
    }

    @Override
    public int getFastMatchToken() {
        return 0;
    }
}
