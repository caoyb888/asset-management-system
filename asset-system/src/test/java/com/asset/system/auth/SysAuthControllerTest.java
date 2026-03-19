package com.asset.system.auth;

import com.asset.common.security.jwt.JwtUtil;
import com.asset.common.security.util.LoginUser;
import com.asset.system.auth.controller.SysAuthController;
import com.asset.system.auth.dto.LoginRequest;
import com.asset.system.auth.dto.LoginResult;
import com.asset.system.auth.dto.RefreshRequest;
import com.asset.system.auth.dto.UserInfoVO;
import com.asset.system.auth.entity.SysLoginLog;
import com.asset.system.auth.mapper.SysLoginLogMapper;
import com.asset.system.auth.service.SysTokenService;
import com.asset.system.dept.entity.SysDept;
import com.asset.system.dept.mapper.SysDeptMapper;
import com.asset.system.menu.mapper.SysMenuMapper;
import com.asset.system.role.entity.SysRole;
import com.asset.system.role.mapper.SysRoleMapper;
import com.asset.system.user.entity.SysUser;
import com.asset.system.user.mapper.SysUserMapper;
import com.asset.common.model.R;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * §4.1 认证中心 — Service 单元测试
 * AUTH-U-01 ~ AUTH-U-18
 *
 * 测试策略：纯 Mockito，无 Spring 上下文，覆盖 SysAuthController 业务分支。
 * SmCryptoUtil / JwtUtil 使用真实实现（BouncyCastle 在 classpath 内）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("§4.1 认证中心单元测试")
class SysAuthControllerTest {

    /* SM3("Test@12345") 预计算值 */
    private static final String SM3_OF_TEST_PWD =
            "923992114bce0e956a7eb2ae11d329841a3426222bd1e27f038798ca057876d4";
    private static final String CORRECT_PWD = "Test@12345";

    @Mock SysUserMapper      sysUserMapper;
    @Mock SysDeptMapper      sysDeptMapper;
    @Mock SysRoleMapper      sysRoleMapper;
    @Mock SysMenuMapper      sysMenuMapper;
    @Mock SysLoginLogMapper  loginLogMapper;
    @Mock SysTokenService    tokenService;

    @InjectMocks SysAuthController controller;

    private HttpServletRequest httpReq;

    @BeforeEach
    void setUp() {
        httpReq = mock(HttpServletRequest.class);
        when(httpReq.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    // ─── 工具方法 ─────────────────────────────────────────────────────────────

    private SysUser buildUser(Long id, String username, int status) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setUsername(username);
        u.setPassword(SM3_OF_TEST_PWD);
        u.setStatus(status);
        return u;
    }

    private LoginRequest buildReq(String username, String password) {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);
        return req;
    }

    // ─── AUTH-U-01 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("AUTH-U-01 登录成功：返回双 Token")
    void login_success_returnTokenPair() {
        SysUser user = buildUser(91001L, "test_admin", 1);
        when(tokenService.getFailCount("test_admin")).thenReturn(0);
        when(sysUserMapper.selectByUsername("test_admin")).thenReturn(user);

        R<LoginResult> result = controller.login(buildReq("test_admin", CORRECT_PWD), httpReq);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getAccessToken()).isNotBlank();
        assertThat(result.getData().getRefreshToken()).isNotBlank();
        assertThat(result.getData().getTokenType()).isEqualTo("Bearer");
        verify(tokenService).storeRefreshToken(anyString(), eq(91001L));
        verify(tokenService).clearFailCount("test_admin");
    }

    // ─── AUTH-U-02 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("AUTH-U-02 登录：用户不存在，失败计数+1")
    void login_userNotFound_incrementsFailCount() {
        when(tokenService.getFailCount("ghost_user")).thenReturn(0);
        when(sysUserMapper.selectByUsername("ghost_user")).thenReturn(null);
        when(tokenService.incrementFailCount("ghost_user")).thenReturn(1);

        R<LoginResult> result = controller.login(buildReq("ghost_user", CORRECT_PWD), httpReq);

        assertThat(result.getCode()).isNotEqualTo(200);
        verify(tokenService).incrementFailCount("ghost_user");
    }

    // ─── AUTH-U-03 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("AUTH-U-03 登录：密码错误，失败计数+1，提示剩余次数")
    void login_wrongPassword_incrementsFailCount() {
        SysUser user = buildUser(91001L, "test_admin", 1);
        when(tokenService.getFailCount("test_admin")).thenReturn(0);
        when(sysUserMapper.selectByUsername("test_admin")).thenReturn(user);
        when(tokenService.incrementFailCount("test_admin")).thenReturn(1);

        R<LoginResult> result = controller.login(buildReq("test_admin", "WrongPass!"), httpReq);

        assertThat(result.getCode()).isNotEqualTo(200);
        assertThat(result.getMsg()).contains("还可尝试");
        verify(tokenService).incrementFailCount("test_admin");
        verify(tokenService, never()).clearFailCount(any());
    }

    // ─── AUTH-U-04 / AUTH-U-05 ────────────────────────────────────────────────

    @Test
    @DisplayName("AUTH-U-04/05 登录：失败计数已达阈值，账号锁定，直接拒绝")
    void login_failCountAtMax_locked() {
        when(tokenService.getFailCount("test_admin")).thenReturn(5);

        R<LoginResult> result = controller.login(buildReq("test_admin", CORRECT_PWD), httpReq);

        assertThat(result.getCode()).isNotEqualTo(200);
        assertThat(result.getMsg()).contains("账号已锁定");
        // 锁定时不应查询用户，避免多余 DB 操作
        verify(sysUserMapper, never()).selectByUsername(any());
    }

    // ─── AUTH-U-06 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("AUTH-U-06 登录：Redis TTL 过期后失败计数归零，自动解锁")
    void login_lockExpired_autoUnlock() {
        // Redis TTL 到期后 getFailCount 自动返回 0
        SysUser user = buildUser(91001L, "test_admin", 1);
        when(tokenService.getFailCount("test_admin")).thenReturn(0);
        when(sysUserMapper.selectByUsername("test_admin")).thenReturn(user);

        R<LoginResult> result = controller.login(buildReq("test_admin", CORRECT_PWD), httpReq);

        assertThat(result.getCode()).isEqualTo(200);
    }

    // ─── AUTH-U-07 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("AUTH-U-07 登录：账号已停用，返回停用提示")
    void login_accountDisabled_fails() {
        SysUser user = buildUser(91006L, "test_disabled", 0); // status=0
        when(tokenService.getFailCount("test_disabled")).thenReturn(0);
        when(sysUserMapper.selectByUsername("test_disabled")).thenReturn(user);

        R<LoginResult> result = controller.login(buildReq("test_disabled", CORRECT_PWD), httpReq);

        assertThat(result.getCode()).isNotEqualTo(200);
        assertThat(result.getMsg()).contains("停用");
        verify(tokenService, never()).clearFailCount(any());
    }

    // ─── AUTH-U-08 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("AUTH-U-08 登录成功：清除历史失败计数")
    void login_success_clearsFailCount() {
        SysUser user = buildUser(91001L, "test_admin", 1);
        when(tokenService.getFailCount("test_admin")).thenReturn(0);
        when(sysUserMapper.selectByUsername("test_admin")).thenReturn(user);

        controller.login(buildReq("test_admin", CORRECT_PWD), httpReq);

        verify(tokenService).clearFailCount("test_admin");
    }

    // ─── AUTH-U-09 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("AUTH-U-09 登录：SM2 解密失败降级为明文比对，仍能正常验证")
    void login_sm2DecryptFallback_plainTextMatchSuccess() {
        SysUser user = buildUser(91001L, "test_admin", 1);
        when(tokenService.getFailCount("test_admin")).thenReturn(0);
        when(sysUserMapper.selectByUsername("test_admin")).thenReturn(user);

        // 传入非合法 SM2 密文 -> sm2Decrypt 抛异常 -> 降级明文比对
        R<LoginResult> result = controller.login(buildReq("test_admin", CORRECT_PWD), httpReq);

        assertThat(result.getCode()).isEqualTo(200);
    }

    // ─── AUTH-U-10 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("AUTH-U-10 登录成功：记录 status=0 的登录日志")
    void login_success_recordsLoginLog() {
        SysUser user = buildUser(91001L, "test_admin", 1);
        when(tokenService.getFailCount("test_admin")).thenReturn(0);
        when(sysUserMapper.selectByUsername("test_admin")).thenReturn(user);

        controller.login(buildReq("test_admin", CORRECT_PWD), httpReq);

        verify(loginLogMapper).insert(argThat((SysLoginLog log) ->
                log.getStatus() == 0 && "test_admin".equals(log.getUsername())));
    }

    // ─── AUTH-U-11 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("AUTH-U-11 登录失败：记录 status=1 的失败日志")
    void login_fail_recordsFailureLog() {
        when(tokenService.getFailCount("test_admin")).thenReturn(0);
        when(sysUserMapper.selectByUsername("test_admin")).thenReturn(null);
        when(tokenService.incrementFailCount("test_admin")).thenReturn(1);

        controller.login(buildReq("test_admin", CORRECT_PWD), httpReq);

        verify(loginLogMapper).insert(argThat((SysLoginLog log) ->
                log.getStatus() == 1 && "test_admin".equals(log.getUsername())));
    }

    // ─── AUTH-U-12 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("AUTH-U-12 Token 刷新：有效 refreshToken，返回新 accessToken")
    void refresh_validToken_returnsNewAccessToken() {
        SysUser user = buildUser(91001L, "test_admin", 1);
        when(tokenService.getUserIdByRefreshToken("valid-rt")).thenReturn(91001L);
        when(sysUserMapper.selectById(91001L)).thenReturn(user);

        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken("valid-rt");

        R<LoginResult> result = controller.refresh(req);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getAccessToken()).isNotBlank();
        assertThat(result.getData().getRefreshToken()).isEqualTo("valid-rt");
    }

    // ─── AUTH-U-13 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("AUTH-U-13 Token 刷新：refreshToken 不存在，返回失败")
    void refresh_invalidToken_fails() {
        when(tokenService.getUserIdByRefreshToken("invalid-rt")).thenReturn(null);

        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken("invalid-rt");

        R<LoginResult> result = controller.refresh(req);

        assertThat(result.getCode()).isNotEqualTo(200);
        assertThat(result.getMsg()).contains("无效");
    }

    // ─── AUTH-U-14 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("AUTH-U-14 登出：Access Token JTI 加入黑名单")
    void logout_blacklistsAccessToken() {
        String token = JwtUtil.generateToken(91001L, "test_admin");
        when(httpReq.getHeader("Authorization")).thenReturn("Bearer " + token);

        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken("some-rt");

        controller.logout(req, httpReq, null);

        verify(tokenService).blacklistAccessToken(anyString(), anyLong());
    }

    // ─── AUTH-U-15 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("AUTH-U-15 获取用户信息：返回角色列表和权限标识")
    void getUserInfo_includesRolesAndPerms() {
        SysUser user = buildUser(91001L, "test_admin", 1);
        user.setDeptId(91001L);
        SysDept dept = new SysDept();
        dept.setId(91001L);
        dept.setDeptName("集团总部");
        SysRole role = new SysRole();
        role.setRoleCode("SUPER_ADMIN");   // 控制器判断超管用的是 "SUPER_ADMIN"

        when(sysUserMapper.selectById(91001L)).thenReturn(user);
        when(sysDeptMapper.selectById(91001L)).thenReturn(dept);
        when(sysRoleMapper.selectByUserId(91001L)).thenReturn(List.of(role));

        LoginUser loginUser = new LoginUser(91001L, "test_admin", null, Collections.emptyList());

        R<UserInfoVO> result = controller.userInfo(loginUser);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getRoles()).containsExactly("SUPER_ADMIN");
        assertThat(result.getData().getPermissions()).containsExactly("*:*:*");
        assertThat(result.getData().getDeptName()).isEqualTo("集团总部");
    }

    // ─── AUTH-U-16 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("AUTH-U-16 获取动态路由：按角色过滤，超管获取全量路由")
    void getUserRoutes_superAdmin_getsAllRoutes() {
        SysUser user = buildUser(91001L, "test_admin", 1);
        SysRole role = new SysRole();
        role.setRoleCode("SUPER_ADMIN");

        when(sysUserMapper.selectById(91001L)).thenReturn(user);
        when(sysRoleMapper.selectByUserId(91001L)).thenReturn(List.of(role));
        when(sysMenuMapper.selectAllVisibleRoutes()).thenReturn(Collections.emptyList());

        LoginUser loginUser = new LoginUser(91001L, "test_admin", null, Collections.emptyList());

        R<?> result = controller.userRoutes(loginUser);

        assertThat(result.getCode()).isEqualTo(200);
        verify(sysMenuMapper).selectAllVisibleRoutes();
        verify(sysMenuMapper, never()).selectRoutesByUserId(any());
    }

    // ─── AUTH-U-17 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("AUTH-U-17 Token 黑名单：blacklistAccessToken 被正确调用（logout流程验证）")
    void logout_tokenBlacklist_called() {
        String token = JwtUtil.generateToken(91001L, "test_admin");
        when(httpReq.getHeader("Authorization")).thenReturn("Bearer " + token);

        LoginUser loginUser = new LoginUser(91001L, "test_admin", null, Collections.emptyList());
        controller.logout(null, httpReq, loginUser);

        verify(tokenService).blacklistAccessToken(anyString(), anyLong());
        verify(tokenService).removeAllRefreshTokensByUser(91001L);
        verify(tokenService).removeOnlineSession(91001L);
    }

    // ─── AUTH-U-18 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("AUTH-U-18 登录成功：在线会话存入 Redis")
    void login_success_storesOnlineSession() {
        SysUser user = buildUser(91001L, "test_admin", 1);
        when(tokenService.getFailCount("test_admin")).thenReturn(0);
        when(sysUserMapper.selectByUsername("test_admin")).thenReturn(user);

        controller.login(buildReq("test_admin", CORRECT_PWD), httpReq);

        verify(tokenService).storeOnlineSession(eq(91001L), eq("test_admin"), anyString());
    }
}
