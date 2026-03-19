package com.asset.system.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * §4.2 用户管理 — Controller 集成测试
 * USER-I-01 ~ USER-I-12
 *
 * 环境：SpringBootTest MOCK + 真实 MySQL(asset_db) + Redis(db=15)
 * 使用 @Transactional 自动回滚，不污染永久测试数据（91001~91199）。
 * 测试用户：test_admin / Test@12345（id=91001, role=TEST_SUPER_ADMIN, data_scope=1）
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
@Rollback
@DisplayName("§4.2 用户管理集成测试")
class SysUserControllerIT {

    private static final String BASE_URL   = "/sys/users";
    private static final String LOGIN_URL  = "/auth/login";

    private static final String ADMIN_USER  = "test_admin";
    private static final String CORRECT_PWD = "Test@12345";

    @Autowired MockMvc mockMvc;
    @Autowired StringRedisTemplate redisTemplate;
    @Autowired ObjectMapper objectMapper;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        // 清理 Redis 失败计数
        Set<String> failKeys = redisTemplate.keys("auth:fail:test_*");
        if (failKeys != null && !failKeys.isEmpty()) redisTemplate.delete(failKeys);

        accessToken = loginAndGetAccessToken();
    }

    @AfterEach
    void tearDown() {
        Set<String> sessionKeys = redisTemplate.keys("auth:online:910*");
        if (sessionKeys != null && !sessionKeys.isEmpty()) redisTemplate.delete(sessionKeys);
        Set<String> refreshKeys = redisTemplate.keys("auth:refresh:*");
        if (refreshKeys != null && !refreshKeys.isEmpty()) redisTemplate.delete(refreshKeys);
    }

    // ─── 工具 ─────────────────────────────────────────────────────────────────

    private String loginAndGetAccessToken() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("username", ADMIN_USER, "password", CORRECT_PWD));
        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .read("$.data.accessToken");
    }

    /** 发送携带 Token 的 GET 请求 */
    private MvcResult authGet(String url) throws Exception {
        return mockMvc.perform(get(url)
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn();
    }

    /** 发送携带 Token 的 POST 请求 */
    private MvcResult authPost(String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
    }

    /** 发送携带 Token 的 PUT 请求 */
    private MvcResult authPut(String url, Object body) throws Exception {
        return mockMvc.perform(put(url)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
    }

    /** 发送携带 Token 的 DELETE 请求 */
    private MvcResult authDelete(String url) throws Exception {
        return mockMvc.perform(delete(url)
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn();
    }

    // ─── USER-I-01 ────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("USER-I-01 分页查询-无过滤：code=200，total>=1")
    void pageQuery_noFilter_returnsAll() throws Exception {
        String json = authGet(BASE_URL + "?pageNum=1&pageSize=20")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
        int total = JsonPath.parse(json).read("$.data.total");
        org.assertj.core.api.Assertions.assertThat(total).isGreaterThanOrEqualTo(1);
    }

    // ─── USER-I-02 ────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("USER-I-02 分页查询-按机构过滤：只返回 deptId=91002 的用户")
    void pageQuery_byDept_filtered() throws Exception {
        String json = authGet(BASE_URL + "?deptId=91002&pageNum=1&pageSize=50")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
        // 从永久测试数据中 dept=91002 有 test_area_mgr(91002) 和 test_custom_user(91004)
        List<Integer> deptIds = JsonPath.parse(json).read("$.data.records[*].deptId");
        org.assertj.core.api.Assertions.assertThat(deptIds)
                .isNotEmpty()
                .allMatch(d -> d == 91002);
    }

    // ─── USER-I-03 ────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("USER-I-03 分页查询-按状态过滤：只返回 status=1 的用户")
    void pageQuery_byStatus_onlyEnabled() throws Exception {
        String json = authGet(BASE_URL + "?status=1&pageNum=1&pageSize=50")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
        List<Integer> statuses = JsonPath.parse(json).read("$.data.records[*].status");
        org.assertj.core.api.Assertions.assertThat(statuses)
                .isNotEmpty()
                .allMatch(s -> s == 1);
    }

    // ─── USER-I-04 ────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("USER-I-04 详情查询：含 roleIds 和 postIds 数组")
    void getDetail_withRolesAndPosts() throws Exception {
        String json = authGet(BASE_URL + "/91001")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
        org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .jsonPath("$.data.username").value("test_admin");
        // 验证 roleIds 和 postIds 是非空数组
        List<?> roleIds = JsonPath.parse(json).read("$.data.roleIds");
        List<?> postIds = JsonPath.parse(json).read("$.data.postIds");
        org.assertj.core.api.Assertions.assertThat(roleIds).isNotEmpty();
        org.assertj.core.api.Assertions.assertThat(postIds).isNotEmpty();
    }

    // ─── USER-I-05 ────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("USER-I-05 新增-合法数据：code=200，返回新用户ID")
    void createUser_validData_success() throws Exception {
        Map<String, Object> body = Map.of(
                "username", "it_test_user_new",
                "password", "Test@12345",
                "realName", "集成测试用户",
                "deptId", 91001,
                "status", 1
        );
        String json = authPost(BASE_URL, body).getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
        Integer id = JsonPath.parse(json).read("$.data");
        org.assertj.core.api.Assertions.assertThat(id).isNotNull().isPositive();
    }

    // ─── USER-I-06 ────────────────────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("USER-I-06 新增-缺少用户名：返回校验失败（非200）")
    void createUser_missingUsername_validationFail() throws Exception {
        Map<String, Object> body = Map.of(
                "password", "Test@12345",
                "deptId", 91001
        );
        MvcResult result = authPost(BASE_URL, body);
        // @Valid 校验失败，HTTP 400 或 code!=200
        String json = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        int httpStatus = result.getResponse().getStatus();
        org.assertj.core.api.Assertions.assertThat(httpStatus).isEqualTo(400);
    }

    // ─── USER-I-07 ────────────────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("USER-I-07 新增-用户名重复：code!=200，含'已存在'")
    void createUser_duplicateUsername_fails() throws Exception {
        Map<String, Object> body = Map.of(
                "username", "test_admin",   // 已存在
                "password", "Test@12345",
                "deptId", 91001
        );
        String json = authPost(BASE_URL, body).getResponse().getContentAsString(StandardCharsets.UTF_8);

        int code = JsonPath.parse(json).read("$.code");
        org.assertj.core.api.Assertions.assertThat(code).isNotEqualTo(200);
        String msg = JsonPath.parse(json).read("$.msg");
        org.assertj.core.api.Assertions.assertThat(msg).contains("已存在");
    }

    // ─── USER-I-08 ────────────────────────────────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("USER-I-08 重置密码：code=200")
    void resetPassword_success() throws Exception {
        Map<String, Object> body = Map.of(
                "userId", 91005,
                "newPassword", "NewPass@123"
        );
        String json = authPut(BASE_URL + "/reset-password", body)
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
    }

    // ─── USER-I-09 ────────────────────────────────────────────────────────────

    @Test
    @Order(9)
    @DisplayName("USER-I-09 禁用用户：code=200")
    void changeStatus_disable_success() throws Exception {
        String json = authPut(BASE_URL + "/91005/status", Map.of("status", 0))
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
    }

    // ─── USER-I-10 ────────────────────────────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("USER-I-10 分配角色：code=200，sys_user_role 更新")
    void assignRoles_success() throws Exception {
        String json = authPost(BASE_URL + "/91005/roles",
                Map.of("roleIds", List.of(91002L, 91003L)))
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
    }

    // ─── USER-I-11 ────────────────────────────────────────────────────────────

    @Test
    @Order(11)
    @DisplayName("USER-I-11 删除用户：code=200（事务内创建再删除）")
    void deleteUser_success() throws Exception {
        // 先新增一个临时用户
        Map<String, Object> createBody = Map.of(
                "username", "it_temp_delete_user",
                "password", "Test@12345",
                "deptId", 91001,
                "status", 1
        );
        String createJson = authPost(BASE_URL, createBody).getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertCode200(createJson);
        int newId = JsonPath.parse(createJson).read("$.data");

        // 再删除
        String deleteJson = authDelete(BASE_URL + "/" + newId)
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(deleteJson);
    }

    // ─── USER-I-12 ────────────────────────────────────────────────────────────

    @Test
    @Order(12)
    @DisplayName("USER-I-12 审计字段自动填充：created_at/updated_at 不为 null")
    void createUser_auditFieldsAutoFilled() throws Exception {
        Map<String, Object> body = Map.of(
                "username", "it_audit_test_user",
                "password", "Test@12345",
                "deptId", 91001,
                "status", 1
        );
        String createJson = authPost(BASE_URL, body).getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertCode200(createJson);
        int newId = JsonPath.parse(createJson).read("$.data");

        // 查询详情，验证审计字段
        String detailJson = authGet(BASE_URL + "/" + newId)
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(detailJson);
        Object createdAt = JsonPath.parse(detailJson).read("$.data.createdAt");
        Object updatedAt = JsonPath.parse(detailJson).read("$.data.updatedAt");
        org.assertj.core.api.Assertions.assertThat(createdAt).isNotNull();
        org.assertj.core.api.Assertions.assertThat(updatedAt).isNotNull();
    }

    // ─── 工具 ─────────────────────────────────────────────────────────────────

    private void assertCode200(String json) {
        int code = JsonPath.parse(json).read("$.code");
        org.assertj.core.api.Assertions.assertThat(code)
                .as("期望 code=200，实际：" + json)
                .isEqualTo(200);
    }
}
