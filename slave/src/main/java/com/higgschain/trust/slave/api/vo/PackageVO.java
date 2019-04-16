package com.higgschain.trust.slave.api.vo;

import com.higgschain.trust.slave.model.bo.BaseBO;
import com.higgschain.trust.slave.model.bo.SignedTransaction;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * The type Package vo.
 *
 * @author tangfashuang
 * @date 2018 /04/11 19:41
 * @desc receive from master or master send other node
 */
@Getter @Setter public class PackageVO extends BaseBO {
    /**
     * transaction list
     */
    @NotEmpty private List<SignedTransaction> signedTxList;

    /**
     * create package time
     */
    @NotNull private Long packageTime;

    /**
     * block height
     */
    @NotNull private Long height;


}
