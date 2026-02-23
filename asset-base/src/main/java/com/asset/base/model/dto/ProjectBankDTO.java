package com.asset.base.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 项目银行账号 DTO
 */
@Data
public class ProjectBankDTO {

    /** 开户银行（必填） */
    @NotBlank(message = "开户银行不能为空")
    private String bankName;

    /** 银行账号（必填） */
    @NotBlank(message = "银行账号不能为空")
    private String bankAccount;

    /** 账户名称（必填） */
    @NotBlank(message = "账户名称不能为空")
    private String accountName;

    /** 是否默认账户：0否 1是 */
    private Integer isDefault;
}
