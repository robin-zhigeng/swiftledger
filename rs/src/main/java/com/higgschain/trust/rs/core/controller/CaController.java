package com.higgschain.trust.rs.core.controller;

import com.higgschain.trust.rs.core.api.CaService;
import com.higgschain.trust.slave.api.vo.CaVO;
import com.higgschain.trust.common.vo.RespData;
import com.higgschain.trust.slave.model.bo.ca.Ca;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The type Ca controller.
 *
 * @author WangQuanzhou
 * @date 2018 /6/5 17:37
 */
@RestController
@Slf4j
public class CaController {

    @Autowired
    private CaService caService;

    /**
     * auth ca transaction
     *
     * @param list the list
     * @return resp data
     */
    @RequestMapping(value = "/ca/auth")
    RespData<String> caAuth(@RequestBody List<CaVO> list) {
        return caService.authCaTx(list);
    }

    /**
     * update ca transaction
     *
     * @param caVO the ca vo
     * @return resp data
     */
    @RequestMapping(value = "/ca/update")
    RespData<String> caUpdate(@RequestBody CaVO caVO) {
        return caService.updateCaTx(caVO);
    }

    /**
     * cancel ca transaction
     *
     * @param caVO the ca vo
     * @return resp data
     */
    @RequestMapping(value = "/ca/cancel")
    RespData<String> caCancel(@RequestBody CaVO caVO) {
        return caService.cancelCaTx(caVO);
    }

    /**
     * acquire ca transaction
     *
     * @param user the user
     * @return resp data
     */
    @RequestMapping(value = "/ca/get")
    RespData<Ca> acquireCA(@RequestParam("user") String user) {
        return caService.acquireCA(user);
    }

}
