package com.asset.base.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 项目财务联系人 DTO
 */
@Data
public class ProjectFinanceContactDTO {

    /** 联系人姓名（必填） */
    @NotBlank(message = "联系人姓名不能为空")
    private String contactName;

    /** 联系电话 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 信用代码 */
    private String creditCode;

    /** 印章类型 */
    private String sealType;

    /** 印章说明 */
    private String sealDesc;
}
