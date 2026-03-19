package com.asset.system.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * §4.1 认证中心 — Controller 集成测试
 * AUTH-I-01 ~ AUTH-I-10
 *
 * 环境：SpringBootTest MOCK + 真实 MySQL(asset_db) + Redis(db=15)
 * 安全过滤器启用，测试完整 HTTP 认证流程。
 * 测试用户均为 test-data-system.sql 中 91001~ 区间的永久数据。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("§4.1 认证中心集成测试")
class SysAuthControllerIT {

    private static final String LOGIN_URL    = "/auth/login";
    private static final String REFRESH_URL  = "/auth/refresh";
    private static final String LOGOUT_URL   = "/auth/logout";
    private static final String INFO_URL     = "/auth/info";
    private static final String PUBLIC_KEY_URL = "/auth/publicKey";
    private static final String PROTECTED_URL  = "/sys/users";   // 需认证

    private static final String ADMIN_USER   = "test_admin";
    private static final String CORRECT_PWD  = "Test@12345";

    @Autowired MockMvc mockMvc;
    @Autowired StringRedisTemplate redisTemplate;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // 清除 db=15 中 test_ 前缀用户的失败计数，保证用例隔离
        Set<String> failKeys = redisTemplate.keys("auth:fail:test_*");
        if (failKeys != null && !failKeys.isEmpty()) {
            redisTemplate.delete(failKeys);
        }
    }

    @AfterEach
    void tearDown() {
        // 清理在线会话等测试产生的 Redis key（前缀 auth:online:9100*）
        Set<String> sessionKeys = redisTemplate.keys("auth:online:910*");
        if (sessionKeys != null && !sessionKeys.isEmpty()) {
            redisTemplate.delete(sessionKeys);
        }
        Set<String> refreshKeys = redisTemplate.keys("auth:refresh:*");
        if (refreshKeys != null && !refreshKeys.isEmpty()) {
            redisTemplate.delete(refreshKeys);
        }
    }

    // ─── 工具方法 ─────────────────────────────────────────────────────────────

    private String loginBody(String username, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of("username", username, "password", password));
    }

    /** 执行登录并返回 accessToken */
    private String loginAndGetAccessToken() throws Exception {
        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(ADMIN_USER, CORRECT_PWD)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString())
                .read("$.data.accessToken");
    }

    /** 执行登录并返回 refreshToken */
    private String loginAndGetRefreshToken() throws Exception {
        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(ADMIN_USER, CORRECT_PWD)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString())
                .read("$.data.refreshToken");
    }

    // ─── AUTH-I-01 ────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("AUTH-I-01 登录成功：返回 code=200、accessToken、refreshToken")
    void authI01_login_success() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(ADMIN_USER, CORRECT_PWD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    // ─── AUTH-I-02 ────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("AUTH-I-02 登录：用户名为空，返回失败")
    void authI02_login_emptyUsername_fails() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"Test@12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.not(200)));
    }

    // ─── AUTH-I-03 ────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("AUTH-I-03 登录：密码为空，仍走正常流程但密码不匹配，返回失败")
    void authI03_login_emptyPassword_fails() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test_admin\",\"password\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.not(200)));
    }

    // ─── AUTH-I-04 ────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("AUTH-I-04 登录：密码错误，返回失败，响应含剩余次数提示")
    void authI04_login_wrongPassword_fails() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(ADMIN_USER, "WrongPassword!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.not(200)))
                .andExpect(jsonPath("$.msg").value(
                        org.hamcrest.Matchers.anyOf(
                                org.hamcrest.Matchers.containsString("密码错误"),
                                org.hamcrest.Matchers.containsString("还可尝试"),
                                org.hamcrest.Matchers.containsString("锁定"))));
    }

    // ─── AUTH-I-05 ────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("AUTH-I-05 登录：停用账号，返回停用提示")
    void authI05_login_disabledAccount_fails() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("test_disabled", CORRECT_PWD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.not(200)))
                .andExpect(jsonPath("$.msg").value(
                        org.hamcrest.Matchers.containsString("停用")));
    }

    // ─── AUTH-I-06 ────────────────────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("AUTH-I-06 Token 刷新：有效 refreshToken，返回新 accessToken")
    void authI06_tokenRefresh_success() throws Exception {
        String refreshToken = loginAndGetRefreshToken();

        String body = objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken));
        MvcResult result = mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        // refreshToken 保持不变
        String returnedRefreshToken = JsonPath.parse(
                result.getResponse().getContentAsString()).read("$.data.refreshToken");
        assertThat(returnedRefreshToken).isEqualTo(refreshToken);
    }

    // ─── AUTH-I-07 ────────────────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("AUTH-I-07 登出：返回 code=200")
    void authI07_logout_success() throws Exception {
        String accessToken = loginAndGetAccessToken();
        String refreshToken = loginAndGetRefreshToken();

        String body = objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken));
        mockMvc.perform(post(LOGOUT_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ─── AUTH-I-08 ────────────────────────────────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("AUTH-I-08 获取用户信息：携带有效 Token，返回 roles 和 permissions")
    void authI08_getUserInfo_withToken() throws Exception {
        String accessToken = loginAndGetAccessToken();

        mockMvc.perform(get(INFO_URL)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value(ADMIN_USER))
                .andExpect(jsonPath("$.data.roles").isArray())
                .andExpect(jsonPath("$.data.permissions").isArray());
    }

    // ─── AUTH-I-09 ────────────────────────────────────────────────────────────

    @Test
    @Order(9)
    @DisplayName("AUTH-I-09 获取公钥：返回 SM2 公钥字符串")
    void authI09_getPublicKey_success() throws Exception {
        mockMvc.perform(get(PUBLIC_KEY_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andExpect(jsonPath("$.data").value(
                        org.hamcrest.Matchers.startsWith("04")));
    }

    // ─── AUTH-I-10 ────────────────────────────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("AUTH-I-10 未认证访问受保护接口：返回 HTTP 401")
    void authI10_unauthenticatedAccess_returns401() throws Exception {
        mockMvc.perform(get(PROTECTED_URL))
                .andExpect(status().isUnauthorized());
    }
}
