package com.higgschain.trust.rs.core.vo.manage;

import com.higgschain.trust.rs.core.vo.BaseVO;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;

/**
 * The type Cancel rs vo.
 */
@Getter
@Setter
public class CancelRsVO extends BaseVO {
    @NotBlank
    @Length(max = 64)
    private String requestId;

    /**
     * rs id
     */
    @NotBlank
    @Length(max = 32)
    private String rsId;
}
