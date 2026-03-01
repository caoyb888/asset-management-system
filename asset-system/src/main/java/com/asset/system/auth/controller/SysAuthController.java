package com.asset.system.auth.controller;

import com.asset.common.model.R;
import com.asset.common.security.crypto.SmCryptoUtil;
import com.asset.common.security.jwt.JwtUtil;
import com.asset.common.security.util.LoginUser;
import com.asset.system.auth.dto.*;
import com.asset.system.auth.entity.SysLoginLog;
import com.asset.system.auth.mapper.SysLoginLogMapper;
import com.asset.system.auth.service.SysTokenService;
import com.asset.system.dept.entity.SysDept;
import com.asset.system.dept.mapper.SysDeptMapper;
import com.asset.system.menu.entity.SysMenu;
import com.asset.system.menu.mapper.SysMenuMapper;
import com.asset.system.role.entity.SysRole;
import com.asset.system.role.mapper.SysRoleMapper;
import com.asset.system.user.entity.SysUser;
import com.asset.system.user.mapper.SysUserMapper;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 认证控制器
 * <p>
 * 登录 / 刷新 / 登出 / 用户信息 / 动态路由
 * </p>
 */
@Slf4j
@Tag(name = "00-认证管理")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class SysAuthController {

    /** 登录失败最大次数，超过后锁定 15 分钟 */
    private static final int MAX_FAIL_COUNT = 5;

    private final SysUserMapper      sysUserMapper;
    private final SysDeptMapper      sysDeptMapper;
    private final SysRoleMapper      sysRoleMapper;
    private final SysMenuMapper      sysMenuMapper;
    private final SysLoginLogMapper  loginLogMapper;
    private final SysTokenService    tokenService;

    // ─── 公钥 ─────────────────────────────────────────────────────────────────

    @Operation(summary = "获取 SM2 公钥（前端加密密码用）")
    @GetMapping("/publicKey")
    public R<String> publicKey() {
        return R.ok(SmCryptoUtil.PUBLIC_KEY_HEX);
    }

    // ─── 登录 ─────────────────────────────────────────────────────────────────

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public R<LoginResult> login(@RequestBody LoginRequest req, HttpServletRequest httpReq) {
        String username = req.getUsername();
        String ip = getClientIp(httpReq);

        // 1. 登录失败次数校验
        int failCount = tokenService.getFailCount(username);
        if (failCount >= MAX_FAIL_COUNT) {
            saveLoginLog(username, ip, 1, "账号已锁定，请15分钟后重试");
            return R.fail("账号已锁定，请15分钟后重试");
        }

        // 2. 查询用户
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            tokenService.incrementFailCount(username);
            saveLoginLog(username, ip, 1, "用户名或密码错误");
            return R.fail("用户名或密码错误");
        }

        // 3. 校验状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            saveLoginLog(username, ip, 1, "账号已停用");
            return R.fail("账号已停用");
        }

        // 4. SM2 解密密码
        String plainPassword;
        try {
            plainPassword = SmCryptoUtil.sm2Decrypt(req.getPassword());
        } catch (Exception e) {
            log.warn("SM2解密失败，尝试明文匹配: {}", e.getMessage());
            plainPassword = req.getPassword();
        }

        // 5. SM3 验证
        if (!SmCryptoUtil.sm3Matches(plainPassword, user.getPassword())) {
            int count = tokenService.incrementFailCount(username);
            int remaining = MAX_FAIL_COUNT - count;
            String msg = remaining > 0
                    ? "用户名或密码错误，还可尝试 " + remaining + " 次"
                    : "用户名或密码错误，账号已锁定15分钟";
            saveLoginLog(username, ip, 1, msg);
            return R.fail(msg);
        }

        // 6. 登录成功：清除失败计数
        tokenService.clearFailCount(username);

        // 7. 生成双 Token
        String accessToken  = JwtUtil.generateToken(user.getId(), user.getUsername());
        String refreshToken = UUID.randomUUID().toString().replace("-", "");
        tokenService.storeRefreshToken(refreshToken, user.getId());

        // 8. 更新最后登录信息
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setLoginIp(ip);
        update.setLoginTime(LocalDateTime.now());
        sysUserMapper.updateById(update);

        // 9. 记录登录日志
        saveLoginLog(username, ip, 0, "登录成功");

        // 10. 记录在线会话
        tokenService.storeOnlineSession(user.getId(), username, ip);

        return R.ok(LoginResult.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(JwtUtil.ACCESS_EXPIRATION_MS / 1000)
                .build());
    }

    // ─── 刷新 Token ───────────────────────────────────────────────────────────

    @Operation(summary = "刷新 Access Token")
    @PostMapping("/refresh")
    public R<LoginResult> refresh(@RequestBody RefreshRequest req) {
        String refreshToken = req.getRefreshToken();
        if (!StringUtils.hasText(refreshToken)) {
            return R.fail("refreshToken 不能为空");
        }

        Long userId = tokenService.getUserIdByRefreshToken(refreshToken);
        if (userId == null) {
            return R.fail("refreshToken 已过期或无效，请重新登录");
        }

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || (user.getStatus() != null && user.getStatus() == 0)) {
            tokenService.removeRefreshToken(refreshToken);
            return R.fail("用户不存在或已停用");
        }

        // 生成新的 Access Token
        String newAccessToken = JwtUtil.generateToken(user.getId(), user.getUsername());

        return R.ok(LoginResult.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)   // refresh token 保持不变
                .tokenType("Bearer")
                .expiresIn(JwtUtil.ACCESS_EXPIRATION_MS / 1000)
                .build());
    }

    // ─── 登出 ─────────────────────────────────────────────────────────────────

    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public R<Void> logout(@RequestBody(required = false) RefreshRequest req,
                          HttpServletRequest httpReq,
                          @AuthenticationPrincipal LoginUser loginUser) {
        // 1. 将 Access Token 加入黑名单
        String header = httpReq.getHeader(JwtUtil.HEADER_NAME);
        String accessToken = JwtUtil.extractToken(header);
        if (accessToken != null) {
            Claims claims = JwtUtil.parseToken(accessToken);
            if (claims != null) {
                tokenService.blacklistAccessToken(JwtUtil.getJti(claims), JwtUtil.getRemainingMs(claims));
            }
        }

        // 2. 删除 Refresh Token
        if (req != null && StringUtils.hasText(req.getRefreshToken())) {
            tokenService.removeRefreshToken(req.getRefreshToken());
        } else if (loginUser != null) {
            tokenService.removeAllRefreshTokensByUser(loginUser.getUserId());
        }

        // 3. 删除在线会话
        if (loginUser != null) {
            tokenService.removeOnlineSession(loginUser.getUserId());
        }

        // 4. 记录登出日志
        String username = loginUser != null ? loginUser.getUsername() : "unknown";
        saveLoginLog(username, getClientIp(httpReq), 0, "正常退出");

        return R.ok(null);
    }

    // ─── 用户信息 ─────────────────────────────────────────────────────────────

    @Operation(summary = "获取当前用户信息")
    @GetMapping({"/info", "/userInfo"})
    public R<UserInfoVO> userInfo(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) {
            return R.fail("未登录");
        }

        SysUser user = sysUserMapper.selectById(loginUser.getUserId());
        if (user == null) {
            return R.fail("用户不存在");
        }

        // 部门名称
        String deptName = null;
        if (user.getDeptId() != null) {
            SysDept dept = sysDeptMapper.selectById(user.getDeptId());
            if (dept != null) deptName = dept.getDeptName();
        }

        // 角色列表
        List<SysRole> roles = sysRoleMapper.selectByUserId(user.getId());
        List<String> roleCodes = roles.stream().map(SysRole::getRoleCode).collect(Collectors.toList());

        // 权限标识列表（超级管理员给全量通配符）
        List<String> permissions;
        boolean isSuperAdmin = roleCodes.contains("SUPER_ADMIN");
        if (isSuperAdmin) {
            permissions = List.of("*:*:*");
        } else {
            permissions = sysMenuMapper.selectPermsByUserId(user.getId());
        }

        return R.ok(UserInfoVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .avatar(user.getAvatar())
                .deptId(user.getDeptId())
                .deptName(deptName)
                .roles(roleCodes)
                .permissions(permissions)
                .build());
    }

    // ─── 动态路由 ─────────────────────────────────────────────────────────────

    @Operation(summary = "获取当前用户动态路由")
    @GetMapping("/user-routes")
    public R<List<RouteVO>> userRoutes(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) {
            return R.fail("未登录");
        }

        SysUser user = sysUserMapper.selectById(loginUser.getUserId());
        if (user == null) return R.ok(List.of());

        List<SysRole> roles = sysRoleMapper.selectByUserId(user.getId());
        boolean isSuperAdmin = roles.stream().anyMatch(r -> "SUPER_ADMIN".equals(r.getRoleCode()));

        // 查询可见菜单（目录+页面，非按钮）
        List<SysMenu> menus = isSuperAdmin
                ? sysMenuMapper.selectAllVisibleRoutes()
                : sysMenuMapper.selectRoutesByUserId(user.getId());

        List<RouteVO> routes = buildRouteTree(menus, 0L);
        return R.ok(routes);
    }

    // ─── 内部辅助 ─────────────────────────────────────────────────────────────

    private List<RouteVO> buildRouteTree(List<SysMenu> menus, Long parentId) {
        return menus.stream()
                .filter(m -> parentId.equals(m.getParentId()))
                .map(m -> {
                    Map<String, Object> meta = new HashMap<>();
                    meta.put("title", m.getMenuName());
                    meta.put("icon", m.getIcon());

                    List<RouteVO> children = buildRouteTree(menus, m.getId());
                    return RouteVO.builder()
                            .path(m.getPath())
                            .name(toCamelCase(m.getMenuName()))
                            .component(m.getComponent())
                            .meta(meta)
                            .children(children.isEmpty() ? null : children)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String toCamelCase(String name) {
        if (name == null) return "";
        return name.replaceAll("[^a-zA-Z0-9\u4e00-\u9fa5]", "");
    }

    private void saveLoginLog(String username, String ip, int status, String msg) {
        try {
            loginLogMapper.insert(SysLoginLog.builder()
                    .username(username)
                    .ipAddr(ip)
                    .status(status)
                    .msg(msg)
                    .loginTime(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("登录日志写入失败: {}", e.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
