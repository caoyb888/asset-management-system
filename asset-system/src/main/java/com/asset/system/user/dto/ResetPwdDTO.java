package com.asset.system.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 重置密码请求 */
@Data
public class ResetPwdDTO {
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
