package com.asset.common.security.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Spring Security 上下文工具类
 */
public final class SecurityUtil {

    private SecurityUtil() {}

    /**
     * 获取当前登录用户ID
     * <p>
     * 若 principal 为 {@link LoginUser} 则直接取 userId；
     * 未认证时（如初始化脚本）返回 0L。
     * </p>
     */
    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return 0L;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof LoginUser loginUser) {
            return loginUser.getUserId();
        }
        return 0L;
    }

    /**
     * 获取当前登录用户名
     */
    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return "anonymous";
        }
        return auth.getName();
    }
}
