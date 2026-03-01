package com.asset.system.auth.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 登录响应 DTO - 双令牌
 */
@Data
@Builder
public class LoginResult {

    /** Access Token（JWT，30分钟有效） */
    private String accessToken;

    /** Refresh Token（UUID，存 Redis，7天有效） */
    private String refreshToken;

    /** Token 类型，固定为 Bearer */
    private String tokenType;

    /** Access Token 有效期（秒）*/
    private Long expiresIn;
}
