package com.higgschain.trust.rs.core.controller.explorer.vo;

import com.higgschain.trust.rs.common.BaseBO;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * The type Query tx vo.
 *
 * @author liuyu
 * @description
 * @date 2018 -07-25
 */
@Getter @Setter public class QueryTxVO extends BaseBO {
    @NotNull
    @Size(max = 64)
    private String txId;
}
