package com.asset.system.code;

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
import java.util.Map;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * §4.9 编码规则 — Controller 集成测试
 * CODE-I-01 ~ CODE-I-03
 *
 * 永久测试数据：
 *   sys_code_rule 91001 TEST_CONTRACT (prefix=HT, yyyyMMdd, sep=-, len=4, resetType=2)
 *   sys_code_rule 91002 TEST_RECEIPT  (prefix=SK, yyyyMMdd, sep=-, len=4, resetType=0)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
@Rollback
@DisplayName("§4.9 编码规则集成测试")
class SysCodeRuleControllerIT {

    private static final String BASE_URL  = "/sys/code-rules";
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

    private MvcResult authPut(String url) throws Exception {
        return mockMvc.perform(put(url)
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn();
    }

    private void assertCode200(String json) {
        int code = JsonPath.parse(json).read("$.code");
        org.assertj.core.api.Assertions.assertThat(code)
                .as("期望 code=200，实际：" + json)
                .isEqualTo(200);
    }

    // ─── CODE-I-01 ────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("CODE-I-01 生成合同编码：返回 HT 前缀编码")
    void generate_contractCode_htPrefix() throws Exception {
        String json = authGet(BASE_URL + "/generate/TEST_CONTRACT")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
        String code = JsonPath.parse(json).read("$.data");
        org.assertj.core.api.Assertions.assertThat(code).startsWith("HT-");
    }

    // ─── CODE-I-02 ────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("CODE-I-02 连续生成递增：第2次序号 > 第1次")
    void generate_sequential_secondGreaterThanFirst() throws Exception {
        String json1 = authGet(BASE_URL + "/generate/TEST_CONTRACT")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
        String json2 = authGet(BASE_URL + "/generate/TEST_CONTRACT")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json1);
        assertCode200(json2);

        String code1 = JsonPath.parse(json1).read("$.data");
        String code2 = JsonPath.parse(json2).read("$.data");

        // extract trailing sequence number (last segment after last "-")
        int seq1 = Integer.parseInt(code1.substring(code1.lastIndexOf('-') + 1));
        int seq2 = Integer.parseInt(code2.substring(code2.lastIndexOf('-') + 1));
        org.assertj.core.api.Assertions.assertThat(seq2).isGreaterThan(seq1);
    }

    // ─── CODE-I-03 ────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("CODE-I-03 重置序列号：code=200")
    void resetSeq_success() throws Exception {
        String json = authPut(BASE_URL + "/91001/reset-seq")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
    }
}
