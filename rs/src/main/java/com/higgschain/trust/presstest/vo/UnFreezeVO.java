package com.higgschain.trust.presstest.vo;

import com.higgschain.trust.rs.core.vo.BaseVO;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * The type Un freeze vo.
 *
 * @author liuyu
 * @description
 * @date 2018 -08-31
 */
@Getter
@Setter
public class UnFreezeVO extends BaseVO{
    /**
     * 请求号
     */
    private String reqNo;
    /**
     * 账号
     */
    private String accountNo;
    /**
     * 冻结流水号
     */
    private String bizFlowNo;
    /**
     * 解冻金额
     */
    private BigDecimal amount;
}
