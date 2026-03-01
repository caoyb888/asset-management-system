package com.asset.common.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 工具类
 * <p>
 * Access Token 有效期 30 分钟，携带 jti（UUID）用于登出时加入黑名单。<br>
 * Refresh Token 以 UUID 存储于 Redis，有效期 7 天，不通过 JWT 传递。
 * </p>
 */
@Slf4j
public final class JwtUtil {

    /** 签名密钥（至少 256 位）——生产环境应从配置读取 */
    public static final String SECRET =
            "asset-management-system-secret-key-2024-prod-change-me-please!!";

    /** Access Token 有效期：30 分钟 */
    public static final long ACCESS_EXPIRATION_MS = 30 * 60 * 1000L;

    /** Token 前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** Authorization Header 名称 */
    public static final String HEADER_NAME = "Authorization";

    private static final SecretKey KEY =
            Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private JwtUtil() {}

    /**
     * 生成 Access Token（含 jti 声明，用于登出黑名单）
     *
     * @param userId   用户ID
     * @param username 用户名
     * @return JWT 字符串（不含 Bearer 前缀）
     */
    public static String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ACCESS_EXPIRATION_MS);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())   // jti - 唯一ID，用于黑名单
                .subject(username)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(KEY)
                .compact();
    }

    /**
     * 解析 JWT Token，返回 Claims
     *
     * @param token JWT 字符串（不含 Bearer 前缀）
     * @return Claims；无效时返回 null
     */
    public static Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.debug("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 Authorization Header 值中提取 token
     */
    public static String extractToken(String headerValue) {
        if (headerValue != null && headerValue.startsWith(TOKEN_PREFIX)) {
            return headerValue.substring(TOKEN_PREFIX.length());
        }
        return null;
    }

    /**
     * 获取 Token 的 jti（唯一ID），用于黑名单
     */
    public static String getJti(Claims claims) {
        return claims.getId();
    }

    /**
     * 获取 Token 的剩余有效毫秒数
     */
    public static long getRemainingMs(Claims claims) {
        Date expiry = claims.getExpiration();
        if (expiry == null) return 0;
        long remaining = expiry.getTime() - System.currentTimeMillis();
        return Math.max(remaining, 0);
    }
}
