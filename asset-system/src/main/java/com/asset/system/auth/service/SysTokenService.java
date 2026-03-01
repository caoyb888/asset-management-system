package com.asset.system.auth.service;

/**
 * Token 管理服务（Redis）
 * <p>
 * 负责：
 * <ul>
 *   <li>Refresh Token 存储 / 查询 / 删除</li>
 *   <li>Access Token 黑名单（登出时加入，TTL = 剩余有效期）</li>
 *   <li>登录失败次数计数 / 查询 / 清除</li>
 * </ul>
 * </p>
 */
public interface SysTokenService {

    // ─── Refresh Token ───────────────────────────────────────────────────────

    /**
     * 存储 Refresh Token
     *
     * @param refreshToken UUID
     * @param userId       用户ID
     */
    void storeRefreshToken(String refreshToken, Long userId);

    /**
     * 通过 Refresh Token 获取用户ID
     *
     * @return userId；不存在或已过期返回 null
     */
    Long getUserIdByRefreshToken(String refreshToken);

    /**
     * 删除 Refresh Token（登出时调用）
     */
    void removeRefreshToken(String refreshToken);

    /**
     * 删除指定用户的所有 Refresh Token（强制下线）
     */
    void removeAllRefreshTokensByUser(Long userId);

    // ─── Access Token 黑名单 ─────────────────────────────────────────────────

    /**
     * 将 Access Token 的 jti 加入黑名单
     *
     * @param jti          Access Token 的 jti 声明
     * @param remainingMs  剩余有效毫秒数（TTL）
     */
    void blacklistAccessToken(String jti, long remainingMs);

    /**
     * 判断 Access Token 的 jti 是否已被加入黑名单
     */
    boolean isBlacklisted(String jti);

    // ─── 登录失败计数 ─────────────────────────────────────────────────────────

    /**
     * 累加登录失败次数，返回当前累计次数
     * （首次失败时设置 TTL = 15 分钟）
     */
    int incrementFailCount(String username);

    /**
     * 查询登录失败次数
     */
    int getFailCount(String username);

    /**
     * 清除登录失败计数（登录成功时调用）
     */
    void clearFailCount(String username);
}
