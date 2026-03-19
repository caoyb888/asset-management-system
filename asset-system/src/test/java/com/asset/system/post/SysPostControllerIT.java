package com.asset.system.post;

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
 * §4.4 岗位管理 — Controller 集成测试
 * POST-I-01 ~ POST-I-05
 *
 * 环境：SpringBootTest MOCK + 真实 MySQL(asset_db) + Redis(db=15)
 * 永久测试数据：sys_post 91001~91004
 *   91001 TEST_GM  总经理   status=1  有关联用户
 *   91002 TEST_PM  项目经理 status=1  有关联用户
 *   91003 TEST_ZS  招商专员 status=1
 *   91004 TEST_STOP 停用岗位 status=0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
@Rollback
@DisplayName("§4.4 岗位管理集成测试")
class SysPostControllerIT {

    private static final String BASE_URL  = "/sys/posts";
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

    // ─── POST-I-01 ────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("POST-I-01 分页查询：code=200，total>=1")
    void pageQuery_returnsResults() throws Exception {
        String json = authGet(BASE_URL + "?pageNum=1&pageSize=20")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
        int total = JsonPath.parse(json).read("$.data.total");
        org.assertj.core.api.Assertions.assertThat(total).isGreaterThanOrEqualTo(1);
    }

    // ─── POST-I-02 ────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("POST-I-02 新增岗位：code=200，返回新ID")
    void createPost_success() throws Exception {
        Map<String, Object> body = Map.of(
                "postCode", "IT_TEST_POST",
                "postName", "集成测试岗位",
                "status", 1
        );
        String json = authPost(BASE_URL, body)
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
        Long newId = ((Number) JsonPath.parse(json).read("$.data")).longValue();
        org.assertj.core.api.Assertions.assertThat(newId).isPositive();
    }

    // ─── POST-I-03 ────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("POST-I-03 新增-编码重复：code!=200，含已存在")
    void createPost_duplicateCode_fails() throws Exception {
        Map<String, Object> body = Map.of(
                "postCode", "TEST_GM",   // 已存在
                "postName", "总经理副本",
                "status", 1
        );
        String json = authPost(BASE_URL, body)
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        int code = JsonPath.parse(json).read("$.code");
        org.assertj.core.api.Assertions.assertThat(code).isNotEqualTo(200);
        String msg = JsonPath.parse(json).read("$.msg");
        org.assertj.core.api.Assertions.assertThat(msg).contains("已存在");
    }

    // ─── POST-I-04 ────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("POST-I-04 删除有关联用户的岗位91001：code!=200，含关联")
    void deletePost_hasUsers_fails() throws Exception {
        String json = authDelete(BASE_URL + "/91001")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        int code = JsonPath.parse(json).read("$.code");
        org.assertj.core.api.Assertions.assertThat(code).isNotEqualTo(200);
        String msg = JsonPath.parse(json).read("$.msg");
        org.assertj.core.api.Assertions.assertThat(msg).contains("用户");
    }

    // ─── POST-I-05 ────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("POST-I-05 启用岗位列表：不含停用岗位(status=0)")
    void listEnabled_noDisabledPost() throws Exception {
        String json = authGet(BASE_URL + "/list")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
        List<Integer> statuses = JsonPath.parse(json).read("$.data[*].status");
        org.assertj.core.api.Assertions.assertThat(statuses)
                .isNotEmpty()
                .allMatch(s -> s == 1);
    }
}
