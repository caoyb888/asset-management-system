package com.asset.common.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 * <p>生成与解析 JWT Token，有效期 8 小时</p>
 */
@Slf4j
public final class JwtUtil {

    /** 签名密钥（至少 256 位）——生产环境应从配置读取 */
    public static final String SECRET =
            "asset-management-system-secret-key-2024-prod-change-me-please!!";

    /** Token 有效期：8 小时 */
    private static final long EXPIRATION_MS = 8 * 60 * 60 * 1000L;

    /** Token 前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** Authorization Header 名称 */
    public static final String HEADER_NAME = "Authorization";

    private static final SecretKey KEY =
            Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private JwtUtil() {}

    /**
     * 生成 JWT Token
     *
     * @param userId   用户ID
     * @param username 用户名
     * @return JWT 字符串（不含 Bearer 前缀）
     */
    public static String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION_MS);
        return Jwts.builder()
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
}
