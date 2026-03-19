package com.asset.system.algorithm;

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
 * §4.10 租费算法 — Controller 集成测试
 * ALGO-I-01 ~ ALGO-I-04
 *
 * sys_fee_algorithm 中已有 system_init.sql 导入的算法数据（auto-increment ID）。
 * 通过 GET /sys/fee-algorithms/enabled 获取实际 ID 后做试算。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
@Rollback
@DisplayName("§4.10 租费算法集成测试")
class SysFeeAlgorithmControllerIT {

    private static final String BASE_URL  = "/sys/fee-algorithms";
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

    private void assertCode200(String json) {
        int code = JsonPath.parse(json).read("$.code");
        org.assertj.core.api.Assertions.assertThat(code)
                .as("期望 code=200，实际：" + json)
                .isEqualTo(200);
    }

    /** 获取启用算法列表并返回第一个 ID */
    private Long getFirstEnabledAlgoId() throws Exception {
        String json = authGet(BASE_URL + "/enabled")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertCode200(json);
        List<Integer> ids = JsonPath.parse(json).read("$.data[*].id");
        org.assertj.core.api.Assertions.assertThat(ids).isNotEmpty();
        return ids.get(0).longValue();
    }

    // ─── ALGO-I-01 ────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("ALGO-I-01 查询启用算法列表：code=200，返回非空")
    void listEnabled_returnsNonEmpty() throws Exception {
        String json = authGet(BASE_URL + "/enabled")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
        List<Object> items = JsonPath.parse(json).read("$.data");
        org.assertj.core.api.Assertions.assertThat(items).isNotEmpty();
    }

    // ─── ALGO-I-02 ────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("ALGO-I-02 试算固定租金：100㎡ × 50元/㎡ × 1月 = 5000")
    void testCalc_fixedRent_success() throws Exception {
        Long algoId = getFirstEnabledAlgoId();

        // First enabled algo is ALG_RENT_FIXED: unit_price * area * months
        Map<String, Object> body = Map.of(
                "algoId", algoId,
                "inputs", Map.of("unit_price", "50", "area", "100", "months", "1")
        );
        String json = authPost(BASE_URL + "/test-calc", body)
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
        String result = JsonPath.parse(json).read("$.data.result");
        org.assertj.core.api.Assertions.assertThat(result).isEqualTo("5000.00");
    }

    // ─── ALGO-I-03 ────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("ALGO-I-03 新增算法：code=200，返回新ID")
    void createAlgorithm_success() throws Exception {
        Map<String, Object> body = Map.of(
                "algoCode", "IT_TEST_ALG",
                "algoName", "IT测试算法",
                "algoType", 1,
                "calcMode", 4,
                "formula", "price * qty",
                "status", 1
        );
        String json = authPost(BASE_URL, body)
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
        Long newId = ((Number) JsonPath.parse(json).read("$.data")).longValue();
        org.assertj.core.api.Assertions.assertThat(newId).isPositive();
    }

    // ─── ALGO-I-04 ────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("ALGO-I-04 分页查询：code=200，total>=1")
    void pageQuery_returnsResults() throws Exception {
        String json = authGet(BASE_URL + "?pageNum=1&pageSize=20")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
        int total = JsonPath.parse(json).read("$.data.total");
        org.assertj.core.api.Assertions.assertThat(total).isGreaterThanOrEqualTo(1);
    }
}
