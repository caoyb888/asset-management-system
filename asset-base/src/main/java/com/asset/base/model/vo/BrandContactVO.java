package com.asset.base.model.vo;

import lombok.Data;

/**
 * 品牌联系人 VO
 */
@Data
public class BrandContactVO {

    private Long id;
    private Long brandId;
    private String contactName;
    private String phone;
    private String email;
    private String position;
    private Integer isPrimary;
}
