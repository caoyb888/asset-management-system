package com.asset.common.security.service;

import com.asset.common.security.config.SecurityProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Token服务 - JWT生成/验证 + Redis存储(支持强制下线)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private static final String TOKEN_PREFIX = "auth:token:";
    private static final String REFRESH_PREFIX = "auth:refresh:";

    private final SecurityProperties properties;
    private final StringRedisTemplate redisTemplate;

    /**
     * 生成AccessToken + RefreshToken
     */
    public String[] createTokenPair(String username, Long userId) {
        String accessToken = createJwt(username, userId, properties.getAccessTokenExpire() * 60L);
        String refreshToken = UUID.randomUUID().toString().replace("-", "");

        // 存储到Redis
        redisTemplate.opsForValue().set(
                TOKEN_PREFIX + accessToken, String.valueOf(userId),
                properties.getAccessTokenExpire(), TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(
                REFRESH_PREFIX + refreshToken, username,
                properties.getRefreshTokenExpire(), TimeUnit.DAYS);

        return new String[]{accessToken, refreshToken};
    }

    public boolean validateToken(String token) {
        try {
            parseJwt(token);
            return Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_PREFIX + token));
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        return parseJwt(token).getSubject();
    }

    /** 续期(滑动窗口) */
    public void refreshExpire(String token) {
        redisTemplate.expire(TOKEN_PREFIX + token, properties.getAccessTokenExpire(), TimeUnit.MINUTES);
    }

    /** 强制下线(删除Token) */
    public void forceLogout(String token) {
        redisTemplate.delete(TOKEN_PREFIX + token);
    }

    private String createJwt(String username, Long userId, long expireSeconds) {
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expireSeconds * 1000))
                .signWith(getSignKey())
                .compact();
    }

    private Claims parseJwt(String token) {
        return Jwts.parser().verifyWith(getSignKey()).build().parseSignedClaims(token).getPayload();
    }

    private SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }
}
