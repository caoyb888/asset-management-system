package com.asset.base.model.vo;

import lombok.Data;

/**
 * 项目银行账号 VO
 */
@Data
public class ProjectBankVO {

    private Long id;
    private Long projectId;
    private String bankName;
    private String bankAccount;
    private String accountName;
    private Integer isDefault;
}
