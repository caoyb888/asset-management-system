package com.asset.system.category;

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
 * §4.11 分类管理 — Controller 集成测试
 * CAT-I-01 ~ CAT-I-04
 *
 * 永久测试数据：
 *   sys_category 91001 format/TEST-FMT  (根节点，有2个子节点)
 *   sys_category 91002 format/TEST-FMT-CY (叶子：餐饮)
 *   sys_category 91003 format/TEST-FMT-LS (叶子：零售)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
@Rollback
@DisplayName("§4.11 分类管理集成测试")
class SysCategoryControllerIT {

    private static final String BASE_URL  = "/sys/categories";
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

    // ─── CAT-I-01 ─────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("CAT-I-01 获取分类树：format 维度，根节点含2个子节点")
    void getCategoryTree_formatType_hasChildren() throws Exception {
        String json = authGet(BASE_URL + "/tree?categoryType=format")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
        List<Object> roots = JsonPath.parse(json).read("$.data");
        org.assertj.core.api.Assertions.assertThat(roots).isNotEmpty();
        List<Object> children = JsonPath.parse(json).read("$.data[0].children");
        org.assertj.core.api.Assertions.assertThat(children).hasSize(2);
    }

    // ─── CAT-I-02 ─────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("CAT-I-02 获取分类维度列表：包含 format")
    void listCategoryTypes_containsFormat() throws Exception {
        String json = authGet(BASE_URL + "/types")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
        List<String> types = JsonPath.parse(json).read("$.data");
        org.assertj.core.api.Assertions.assertThat(types).contains("format");
    }

    // ─── CAT-I-03 ─────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("CAT-I-03 新增叶子分类：code=200，返回新ID")
    void createCategory_leaf_success() throws Exception {
        Map<String, Object> body = Map.of(
                "categoryType", "format",
                "parentId", 91001,
                "categoryCode", "IT-FMT-TEST",
                "categoryName", "IT测试业态",
                "status", 1
        );
        String json = authPost(BASE_URL, body)
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
        Long newId = ((Number) JsonPath.parse(json).read("$.data")).longValue();
        org.assertj.core.api.Assertions.assertThat(newId).isPositive();
    }

    // ─── CAT-I-04 ─────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("CAT-I-04 删除有子节点的91001：code!=200，含子分类提示")
    void deleteCategory_hasChildren_fails() throws Exception {
        String json = authDelete(BASE_URL + "/91001")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        int code = JsonPath.parse(json).read("$.code");
        org.assertj.core.api.Assertions.assertThat(code).isNotEqualTo(200);
        String msg = JsonPath.parse(json).read("$.msg");
        org.assertj.core.api.Assertions.assertThat(msg).contains("子");
    }
}
