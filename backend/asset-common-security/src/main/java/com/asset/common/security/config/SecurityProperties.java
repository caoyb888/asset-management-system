package com.asset.common.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * JWT + 安全配置属性
 */
@Data
@ConfigurationProperties(prefix = "asset.security")
public class SecurityProperties {

    /** JWT密钥 */
    private String jwtSecret = "asset-management-system-jwt-secret-key-2026";

    /** AccessToken过期时间(分钟) */
    private int accessTokenExpire = 30;

    /** RefreshToken过期时间(天) */
    private int refreshTokenExpire = 7;

    /** Token前缀 */
    private String tokenPrefix = "Bearer ";

    /** Token请求头 */
    private String tokenHeader = "Authorization";

    /** 白名单路径(无需认证) */
    private List<String> whiteList = new ArrayList<>(List.of(
            "/login", "/logout", "/captcha",
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**",
            "/actuator/**", "/druid/**"
    ));
}
