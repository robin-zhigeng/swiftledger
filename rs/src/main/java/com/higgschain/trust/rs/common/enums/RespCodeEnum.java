package com.higgschain.trust.rs.common.enums;

/**
 * Created by young001 on 2017/6/17.
 */

public enum RespCodeEnum {
    //maincode意义：    000-》成功； 100-》参数异常；200-》业务异常；500-》系统异常 600->失败大类

//    与沈腾沟通，在成功类，只存放请求时正常接受并且是正常以成功方式内容返回的，比如鉴权成功是放在000成功类中，如果鉴权失败放在
//    200业务异常中，表示这个请求业务业务处理失败，包括外部业务系统异常、内部业务处理逻辑异常、请求没有成功的方式返回，比如鉴权
//    如果鉴权失败，那么也放在业务异常类中，不放在处理成功类型中，这样业务系统调用的时候只要判断返回码即可，这种返回的时候bizResponse
//    为null即可，不用解析bizResponse中的ppAuthenorizeFlag
//    分类参考：https://doc.open.alipay.com/docs/doc.htm?spm=a219a.7395905.0.0.25RpvU&treeId=262&articleId=105806&docType=1

    /**
     * Success resp code enum.
     */
    SUCCESS("000", "000", "操作成功"),
    /**
     * Properties setter success resp code enum.
     */
    PROPERTIES_SETTER_SUCCESS("000", "000", "修改内存参数成功"),

    /**
     * Async send identity request resp code enum.
     */
    //异步存证
    ASYNC_SEND_IDENTITY_REQUEST("000", "000", "下发存证请求成功"),
    /**
     * Get identity request success resp code enum.
     */
    GET_IDENTITY_REQUEST_SUCCESS("000", "000", "获取存证信息成功"),

    /**
     * Create bill process resp code enum.
     */
    //票据处理
    CREATE_BILL_PROCESS("000", "001", "创建票据处理中"),
    /**
     * Transfer bill process resp code enum.
     */
    TRANSFER_BILL_PROCESS("000", "002", "转移票据处理中"),

    /**
     * Signature verify fail resp code enum.
     */
    SIGNATURE_VERIFY_FAIL("100", "000", "签名验证失败"),
    /**
     * Param not valid resp code enum.
     */
    PARAM_NOT_VALID("100", "001", "请求参数校验失败"),
    /**
     * Request repeat check valid resp code enum.
     */
    REQUEST_REPEAT_CHECK_VALID("100", "007", "重复请求"),
    /**
     * Param check empty valid resp code enum.
     */
    PARAM_CHECK_EMPTY_VALID("100", "008", "请求参数为null"),
    /**
     * Pubkey or address check valid resp code enum.
     */
    PUBKEY_OR_ADDRESS_CHECK_VALID("100", "009", "公钥与地址不匹配"),
    /**
     * Param address not valid resp code enum.
     */
    PARAM_ADDRESS_NOT_VALID("100", "022", "地址或公钥格式不合法"),
    /**
     * Identity not exist resp code enum.
     */
    IDENTITY_NOT_EXIST("100", "023", "存证信息不存在"),
    /**
     * Rs id not match node name resp code enum.
     */
    RS_ID_NOT_MATCH_NODE_NAME("100", "024", "节点名称与注册RS不匹配"),
    /**
     * Ca is not exist or is not valid resp code enum.
     */
    CA_IS_NOT_EXIST_OR_IS_NOT_VALID("100", "025", "未注册CA或者CA不可用"),
    /**
     * Rs node already exist resp code enum.
     */
    RS_NODE_ALREADY_EXIST("100", "026", "RS节点已经注册"),
    /**
     * Policy rs ids must have sender resp code enum.
     */
    POLICY_RS_IDS_MUST_HAVE_SENDER("100", "027", "policy发起方必须包含在policy的rsIds中"),
    /**
     * Policy already exist resp code enum.
     */
    POLICY_ALREADY_EXIST("100", "028", "policy已经存在"),
    /**
     * Rs node not exist or rs node already canceled resp code enum.
     */
    RS_NODE_NOT_EXIST_OR_RS_NODE_ALREADY_CANCELED("100", "029", "RS节点不存在或状态已经注销"),
    /**
     * Request duplicate resp code enum.
     */
    REQUEST_DUPLICATE("100", "030", "请求重复"),

    /**
     * Bill holder not existed resp code enum.
     */
    BILL_HOLDER_NOT_EXISTED("200", "001", "票据持有人不存在"),
    /**
     * Bill transfer invalid param resp code enum.
     */
    BILL_TRANSFER_INVALID_PARAM("200", "002", "持票人对应的转账票据不存在异常"),
    /**
     * Data not exist resp code enum.
     */
    DATA_NOT_EXIST("200", "003", "数据库查询结果为空"),
    /**
     * Bill transfer billid idempotent faied resp code enum.
     */
    BILL_TRANSFER_BILLID_IDEMPOTENT_FAIED("200", "004", "票据拆分有重复 billId存在"),
    /**
     * Bill billid exist exception resp code enum.
     */
    BILL_BILLID_EXIST_EXCEPTION("200", "005", "新生成的票据 billId 已存在  异常"),

    /**
     * Sys limited resp code enum.
     */
    SYS_LIMITED("500", "003", "系统限流，请稍后重试"),
    /**
     * Sys fail retry resp code enum.
     */
    SYS_FAIL_RETRY("500", "001", "系统繁忙，请稍后重试"),
    /**
     * Sys maintain resp code enum.
     */
    SYS_MAINTAIN("500", "002", "bankchain维护中"),
    /**
     * Sys fail resp code enum.
     */
    SYS_FAIL("500", "000", "系统异常"),
    /**
     * Sys database fail resp code enum.
     */
    SYS_DATABASE_FAIL("500", "000", "数据库异常，请重试");

    RespCodeEnum(String mainCode, String subCode, String msg) {
        this.mainCode = mainCode;
        this.subCode = subCode;
        this.msg = msg;
    }

    /**
     * Gets resp code.
     *
     * @return the resp code
     */
    public String getRespCode() {
        return this.mainCode + this.subCode;
    }

    /**
     * Gets msg.
     *
     * @return the msg
     */
    public String getMsg() {
        return msg;
    }
  /*  public boolean equals(String code) {
        return this.getCode().equals(code);
    }*/

    /**
     * 主码
     */
    private String mainCode;
    /**
     * 子码
     */
    private String subCode;
    /**
     * 返回信息
     */
    private String msg;

    /**
     * Gets main code.
     *
     * @return the main code
     */
    public String getMainCode() {
        return mainCode;
    }

    /**
     * Gets sub code.
     *
     * @return the sub code
     */
    public String getSubCode() {
        return subCode;
    }

}
