package com.asset.base.model.dto;

import lombok.Data;

/**
 * 商家开票信息独立 CRUD DTO
 */
@Data
public class InvoiceSaveDTO {

    /** 发票抬头 */
    private String invoiceTitle;

    /** 税号 */
    private String taxNumber;

    /** 开户银行 */
    private String bankName;

    /** 银行账号 */
    private String bankAccount;

    /** 注册地址 */
    private String address;

    /** 注册电话 */
    private String phone;

    /** 是否默认：0否 1是 */
    private Integer isDefault;
}
