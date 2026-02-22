package com.asset.base.model.vo;

import lombok.Data;

/**
 * 商家联系人 VO
 */
@Data
public class MerchantContactVO {

    private Long id;
    private Long merchantId;
    private String contactName;
    private String phone;
    private String email;
    private String position;
    private Integer isPrimary;
}
