package com.asset.base.model.vo;

import lombok.Data;

/**
 * 商家开票信息 VO
 */
@Data
public class MerchantInvoiceVO {

    private Long id;
    private Long merchantId;
    private String invoiceTitle;
    private String taxNumber;
    private String bankName;
    private String bankAccount;
    private String address;
    private String phone;
    private Integer isDefault;
}
