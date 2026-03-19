package com.asset.system.dept;

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
 * §4.3 机构管理 — Controller 集成测试
 * DEPT-I-01 ~ DEPT-I-07
 *
 * 环境：SpringBootTest MOCK + 真实 MySQL(asset_db) + Redis(db=15)
 * 使用 @Transactional 自动回滚，不污染永久测试数据（91001~91199）。
 * 测试用户：test_admin / Test@12345（id=91001, role=SUPER_ADMIN, data_scope=1）
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
@Rollback
@DisplayName("§4.3 机构管理集成测试")
class SysDeptControllerIT {

    private static final String BASE_URL  = "/sys/depts";
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
        redisTemplate.delete("sys:dept:tree");
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

    // ─── DEPT-I-01 ────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("DEPT-I-01 获取完整部门树：树形结构，根节点含 children")
    void getDeptTree_returnsTreeStructure() throws Exception {
        String json = authGet(BASE_URL)
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
        List<Object> roots = JsonPath.parse(json).read("$.data");
        org.assertj.core.api.Assertions.assertThat(roots).isNotEmpty();
        // 根节点应有子节点（91001 集团总部下有华南/华东）
        List<Object> children = JsonPath.parse(json).read("$.data[0].children");
        org.assertj.core.api.Assertions.assertThat(children).isNotEmpty();
    }

    // ─── DEPT-I-02 ────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("DEPT-I-02 新增部门：code=200，ancestors 正确")
    void createDept_success() throws Exception {
        Map<String, Object> body = Map.of(
                "parentId", 91002,
                "deptName", "IT测试项目部",
                "status", 1
        );
        String json = authPost(BASE_URL, body)
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
        Long newId = ((Number) JsonPath.parse(json).read("$.data")).longValue();
        org.assertj.core.api.Assertions.assertThat(newId).isPositive();

        // 验证 ancestors 通过详情接口
        String detail = authGet(BASE_URL + "/" + newId)
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
        String ancestors = JsonPath.parse(detail).read("$.data.ancestors");
        org.assertj.core.api.Assertions.assertThat(ancestors).isEqualTo("0,91001,91002");
    }

    // ─── DEPT-I-03 ────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("DEPT-I-03 编辑部门91003：code=200")
    void updateDept_success() throws Exception {
        Map<String, Object> body = Map.of(
                "deptName", "天河项目部（已更新）",
                "status", 1
        );
        String json = authPut(BASE_URL + "/91003", body)
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
    }

    // ─── DEPT-I-04 ────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("DEPT-I-04 删除有子部门的91002：code!=200，含子部门")
    void deleteDept_hasChildren_fails() throws Exception {
        String json = authDelete(BASE_URL + "/91002")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        int code = JsonPath.parse(json).read("$.code");
        org.assertj.core.api.Assertions.assertThat(code).isNotEqualTo(200);
        String msg = JsonPath.parse(json).read("$.msg");
        org.assertj.core.api.Assertions.assertThat(msg).contains("子部门");
    }

    // ─── DEPT-I-05 ────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("DEPT-I-05 删除空部门91007：code=200")
    void deleteDept_empty_success() throws Exception {
        String json = authDelete(BASE_URL + "/91007")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
    }

    // ─── DEPT-I-06 ────────────────────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("DEPT-I-06 移动91003到91005下：code=200，ancestors更新为0,91001,91005")
    void moveDept_success() throws Exception {
        Map<String, Object> body = Map.of("targetParentId", 91005);
        String json = authPut(BASE_URL + "/91003/move", body)
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);

        // 验证 ancestors 已更新
        String detail = authGet(BASE_URL + "/91003")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
        String ancestors = JsonPath.parse(detail).read("$.data.ancestors");
        org.assertj.core.api.Assertions.assertThat(ancestors).isEqualTo("0,91001,91005");
    }

    // ─── DEPT-I-07 ────────────────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("DEPT-I-07 查询91002下用户：返回华南区域用户列表")
    void getDeptUsers_returnsUsers() throws Exception {
        String json = authGet(BASE_URL + "/91002/users?includeChildren=false")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
        List<Object> users = JsonPath.parse(json).read("$.data");
        org.assertj.core.api.Assertions.assertThat(users).isNotEmpty();
    }
}
