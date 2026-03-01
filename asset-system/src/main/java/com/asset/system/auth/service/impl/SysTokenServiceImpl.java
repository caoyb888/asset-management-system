package com.asset.system.auth.service.impl;

import com.asset.system.auth.service.SysTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token 管理服务实现（基于 StringRedisTemplate）
 * <p>
 * Redis Key 规范：
 * <ul>
 *   <li>auth:refresh:{refreshToken}   → userId（String），TTL 7天</li>
 *   <li>auth:user:refresh:{userId}    → refreshToken（String），TTL 7天（反向索引，用于强制下线）</li>
 *   <li>auth:blacklist:{jti}          → "1"，TTL = Access Token 剩余时间</li>
 *   <li>auth:fail:{username}          → 失败次数（String），TTL 15分钟</li>
 * </ul>
 * </p>
 */
@Service
@RequiredArgsConstructor
public class SysTokenServiceImpl implements SysTokenService {

    private static final String PREFIX_REFRESH      = "auth:refresh:";
    private static final String PREFIX_USER_REFRESH = "auth:user:refresh:";
    private static final String PREFIX_BLACKLIST    = "auth:blacklist:";
    private static final String PREFIX_FAIL         = "auth:fail:";

    private static final long REFRESH_TTL_DAYS  = 7L;
    private static final long FAIL_TTL_MINUTES  = 15L;

    private final StringRedisTemplate redisTemplate;

    // ─── Refresh Token ────────────────────────────────────────────────────────

    @Override
    public void storeRefreshToken(String refreshToken, Long userId) {
        String key    = PREFIX_REFRESH + refreshToken;
        String revKey = PREFIX_USER_REFRESH + userId;
        redisTemplate.opsForValue().set(key,    String.valueOf(userId), REFRESH_TTL_DAYS, TimeUnit.DAYS);
        redisTemplate.opsForValue().set(revKey, refreshToken,           REFRESH_TTL_DAYS, TimeUnit.DAYS);
    }

    @Override
    public Long getUserIdByRefreshToken(String refreshToken) {
        String value = redisTemplate.opsForValue().get(PREFIX_REFRESH + refreshToken);
        if (value == null) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void removeRefreshToken(String refreshToken) {
        String userIdStr = redisTemplate.opsForValue().get(PREFIX_REFRESH + refreshToken);
        redisTemplate.delete(PREFIX_REFRESH + refreshToken);
        if (userIdStr != null) {
            redisTemplate.delete(PREFIX_USER_REFRESH + userIdStr);
        }
    }

    @Override
    public void removeAllRefreshTokensByUser(Long userId) {
        String refreshToken = redisTemplate.opsForValue().get(PREFIX_USER_REFRESH + userId);
        if (refreshToken != null) {
            redisTemplate.delete(PREFIX_REFRESH + refreshToken);
        }
        redisTemplate.delete(PREFIX_USER_REFRESH + userId);
    }

    // ─── Access Token 黑名单 ──────────────────────────────────────────────────

    @Override
    public void blacklistAccessToken(String jti, long remainingMs) {
        if (remainingMs <= 0) return;
        redisTemplate.opsForValue().set(PREFIX_BLACKLIST + jti, "1", remainingMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX_BLACKLIST + jti));
    }

    // ─── 登录失败计数 ─────────────────────────────────────────────────────────

    @Override
    public int incrementFailCount(String username) {
        String key = PREFIX_FAIL + username;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            // 首次失败，设置 TTL
            redisTemplate.expire(key, FAIL_TTL_MINUTES, TimeUnit.MINUTES);
        }
        return count == null ? 0 : count.intValue();
    }

    @Override
    public int getFailCount(String username) {
        String value = redisTemplate.opsForValue().get(PREFIX_FAIL + username);
        if (value == null) return 0;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public void clearFailCount(String username) {
        redisTemplate.delete(PREFIX_FAIL + username);
    }
}
