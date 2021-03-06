package com.higgschain.trust.slave.core.service.action.ca;

import com.higgschain.trust.common.utils.Profiler;
import com.higgschain.trust.slave.api.enums.ActionTypeEnum;
import com.higgschain.trust.slave.common.enums.SlaveErrorEnum;
import com.higgschain.trust.slave.common.exception.SlaveException;
import com.higgschain.trust.slave.core.service.action.ActionHandler;
import com.higgschain.trust.slave.core.service.datahandler.ca.CaSnapshotHandler;
import com.higgschain.trust.slave.model.bo.action.Action;
import com.higgschain.trust.slave.model.bo.ca.Ca;
import com.higgschain.trust.slave.model.bo.ca.CaAction;
import com.higgschain.trust.slave.model.bo.context.ActionData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The type Ca update handler.
 *
 * @author WangQuanzhou
 * @desc update ca handler
 * @date 2018 /6/6 10:25
 */
@Slf4j @Component public class CaUpdateHandler implements ActionHandler {

    /**
     * The Ca snapshot handler.
     */
    @Autowired
    CaSnapshotHandler caSnapshotHandler;
    /**
     * The Ca helper.
     */
    @Autowired CaHelper caHelper;

    @Override public void verifyParams(Action action) throws SlaveException {
        CaAction caAction = (CaAction)action;
        caHelper.verifyParams(caAction);
    }

    /**
     * the storage for the action
     *
     * @param actionData
     */
    @Override public void process(ActionData actionData) {
        // convert action and validate it
        CaAction caAction = (CaAction)actionData.getCurrentAction();

        log.info("[CaUpdateHandler.process] start to process ca update action, user={}, pubKey={}, usage={}",
            caAction.getUser(), caAction.getPubKey(), caAction.getUsage());

        if (!caHelper.validate(caAction, ActionTypeEnum.CA_UPDATE)) {
            log.error("[CaUpdateHandler.process] actionData validate error, user={}, pubKey={}", caAction.getUser(),
                caAction.getPubKey());
            throw new SlaveException(SlaveErrorEnum.SLAVE_CA_VALIDATE_ERROR,
                "[CaUpdateHandler.process] actionData validate error");
        }

        Profiler.enter("[CaUpdateHandler.updateCa]");
        Ca ca = new Ca();
        BeanUtils.copyProperties(caAction, ca);
        caSnapshotHandler.updateCa(ca);

        Profiler.release();

        // TODO  添加refresh()方法属性集群配置信息
    }
}
