package com.asset.system.role;

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
 * §4.5 角色管理 — Controller 集成测试
 * ROLE-I-01 ~ ROLE-I-07
 *
 * 环境：SpringBootTest MOCK + 真实 MySQL(asset_db) + Redis(db=15)
 * 永久测试数据：sys_role 91001~91006
 *   91001 TEST_SUPER_ADMIN  status=1  有用户
 *   91002 TEST_AREA_MANAGER status=1  有用户  menuIds=[91001,91002,91003]
 *   91003 TEST_PROJECT_MANAGER status=1
 *   91004 TEST_CUSTOM_SCOPE  status=1  deptIds=[91003,91004]
 *   91005 TEST_EMPLOYEE      status=1
 *   91006 TEST_DISABLED_ROLE status=0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
@Rollback
@DisplayName("§4.5 角色管理集成测试")
class SysRoleControllerIT {

    private static final String BASE_URL  = "/sys/roles";
    private static final String LOGIN_URL = "/auth/login";

    private static final String ADMIN_USER  = "test_admin";
    private static final String CORRECT_PWD = "Test@12345";

    @Autowired MockMvc mockMvc;
    @Autowired StringRedisTemplate redisTemplate;
    @Autowired ObjectMapper objectMapper;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
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

    private MvcResult authGet(String url) throws Exception {
        return mockMvc.perform(get(url)
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn();
    }

    private MvcResult authPost(String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
    }

    private MvcResult authPut(String url, Object body) throws Exception {
        return mockMvc.perform(put(url)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
    }

    private MvcResult authDelete(String url) throws Exception {
        return mockMvc.perform(delete(url)
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn();
    }

    private void assertCode200(String json) {
        int code = JsonPath.parse(json).read("$.code");
        org.assertj.core.api.Assertions.assertThat(code)
                .as("期望 code=200，实际：" + json)
                .isEqualTo(200);
    }

    // ─── ROLE-I-01 ────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("ROLE-I-01 分页查询：code=200，total>=1")
    void pageQuery_returnsResults() throws Exception {
        String json = authGet(BASE_URL + "?pageNum=1&pageSize=20")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
        int total = JsonPath.parse(json).read("$.data.total");
        org.assertj.core.api.Assertions.assertThat(total).isGreaterThanOrEqualTo(1);
    }

    // ─── ROLE-I-02 ────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("ROLE-I-02 详情-角色91002含 menuIds 列表")
    void getDetail_withMenuIds() throws Exception {
        String json = authGet(BASE_URL + "/91002")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
        List<Integer> menuIds = JsonPath.parse(json).read("$.data.menuIds");
        org.assertj.core.api.Assertions.assertThat(menuIds)
                .isNotEmpty()
                .contains(91001, 91002, 91003);
    }

    // ─── ROLE-I-03 ────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("ROLE-I-03 新增角色：code=200，返回新ID")
    void createRole_success() throws Exception {
        Map<String, Object> body = Map.of(
                "roleCode", "IT_TEST_ROLE",
                "roleName", "集成测试角色",
                "status", 1
        );
        String json = authPost(BASE_URL, body)
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
        Long newId = ((Number) JsonPath.parse(json).read("$.data")).longValue();
        org.assertj.core.api.Assertions.assertThat(newId).isPositive();
    }

    // ─── ROLE-I-04 ────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("ROLE-I-04 分配菜单给角色91002：code=200，DB 更新")
    void grantMenus_success() throws Exception {
        Map<String, Object> body = Map.of("menuIds", List.of(91001L, 91006L, 91007L));
        String json = authPut(BASE_URL + "/91002/menus", body)
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);

        // 验证菜单已更新
        String detail = authGet(BASE_URL + "/91002")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Integer> menuIds = JsonPath.parse(detail).read("$.data.menuIds");
        org.assertj.core.api.Assertions.assertThat(menuIds).contains(91007);
    }

    // ─── ROLE-I-05 ────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("ROLE-I-05 设置数据权限-自定义：code=200，sys_role_data 更新")
    void setDataScope_custom_success() throws Exception {
        Map<String, Object> body = Map.of(
                "dataScope", 2,
                "deptIds", List.of(91001L, 91002L)
        );
        String json = authPut(BASE_URL + "/91004/data-scope", body)
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);

        // 验证 deptIds 已更新
        String deptIdsJson = authGet(BASE_URL + "/91004/dept-ids")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Integer> deptIds = JsonPath.parse(deptIdsJson).read("$.data");
        org.assertj.core.api.Assertions.assertThat(deptIds).containsExactlyInAnyOrder(91001, 91002);
    }

    // ─── ROLE-I-06 ────────────────────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("ROLE-I-06 删除有用户角色91002：code!=200，含关联")
    void deleteRole_hasUsers_fails() throws Exception {
        String json = authDelete(BASE_URL + "/91002")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        int code = JsonPath.parse(json).read("$.code");
        org.assertj.core.api.Assertions.assertThat(code).isNotEqualTo(200);
        String msg = JsonPath.parse(json).read("$.msg");
        org.assertj.core.api.Assertions.assertThat(msg).contains("用户");
    }

    // ─── ROLE-I-07 ────────────────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("ROLE-I-07 启用角色列表：不含停用角色(status=0)")
    void listEnabled_noDisabledRole() throws Exception {
        String json = authGet(BASE_URL + "/list")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
        List<Integer> statuses = JsonPath.parse(json).read("$.data[*].status");
        org.assertj.core.api.Assertions.assertThat(statuses)
                .isNotEmpty()
                .allMatch(s -> s == 1);
    }
}
