package com.asset.base.model.vo;

import lombok.Data;

/**
 * 项目财务联系人 VO
 */
@Data
public class ProjectFinanceContactVO {

    private Long id;
    private Long projectId;
    private String contactName;
    private String phone;
    private String email;
    private String creditCode;
    private String sealType;
    private String sealDesc;
}
