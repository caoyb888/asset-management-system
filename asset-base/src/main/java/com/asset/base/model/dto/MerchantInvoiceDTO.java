package com.asset.base.model.dto;

import lombok.Data;

/**
 * 商家开票信息 DTO（嵌入 MerchantSaveDTO）
 */
@Data
public class MerchantInvoiceDTO {

    /** 开票信息ID（编辑时传入，新增时为空） */
    private Long id;

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
