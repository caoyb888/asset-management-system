package com.asset.system.auth.dto;

import lombok.Data;

/**
 * 登录请求 DTO
 */
@Data
public class LoginRequest {

    /** 用户名 */
    private String username;

    /** SM2 加密后的密码（前端用公钥加密） */
    private String password;
}
