package com.higgschain.trust.slave.dao.po.ca;

import com.higgschain.trust.common.mybatis.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * The type Ca po.
 *
 * @author WangQuanzhou
 * @desc CA information
 * @date 2018 /6/5 10:21
 */
@Getter @Setter public class CaPO extends BaseEntity {

    private String version;

    private Date period;

    private boolean valid;

    private String pubKey;

    private String user;

    private String usage;

    private Date createTime;

    private Date updateTime;

}
