package com.asset.system.dict;

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
 * §4.8 业务字典 — Controller 集成测试
 * DICT-I-01 ~ DICT-I-05
 *
 * 永久测试数据：
 *   sys_dict_type  91001 test_project_status (含3条数据)
 *   sys_dict_type  91002 test_shop_status    (含2条数据)
 *   sys_dict_data  91001~91003 test_project_status
 *   sys_dict_data  91004~91005 test_shop_status
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
@Rollback
@DisplayName("§4.8 业务字典集成测试")
class SysDictControllerIT {

    private static final String TYPE_URL  = "/sys/dict/types";
    private static final String DATA_URL  = "/sys/dict/data";
    private static final String CACHE_URL = "/sys/dict/cache";
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
        // 清理字典缓存
        redisTemplate.delete("sys:dict:test_project_status");
        redisTemplate.delete("sys:dict:test_shop_status");
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

    // ─── DICT-I-01 ────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("DICT-I-01 字典类型分页：code=200，total>=1")
    void pageType_returnsResults() throws Exception {
        String json = authGet(TYPE_URL + "?pageNum=1&pageSize=20")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
        int total = JsonPath.parse(json).read("$.data.total");
        org.assertj.core.api.Assertions.assertThat(total).isGreaterThanOrEqualTo(1);
    }

    // ─── DICT-I-02 ────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("DICT-I-02 按类型查数据：test_project_status 返回3条")
    void listData_projectStatus_returns3() throws Exception {
        String json = authGet(DATA_URL + "/test_project_status")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
        List<Object> items = JsonPath.parse(json).read("$.data");
        org.assertj.core.api.Assertions.assertThat(items).hasSize(3);
    }

    // ─── DICT-I-03 ────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("DICT-I-03 新增字典数据：code=200，返回新ID")
    void createData_success() throws Exception {
        Map<String, Object> body = Map.of(
                "dictType", "test_project_status",
                "dictLabel", "IT测试项",
                "dictValue", "99",
                "status", 1
        );
        String json = authPost(DATA_URL, body)
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
        Long newId = ((Number) JsonPath.parse(json).read("$.data")).longValue();
        org.assertj.core.api.Assertions.assertThat(newId).isPositive();
    }

    // ─── DICT-I-04 ────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("DICT-I-04 删除字典类型-级联删关联数据")
    void deleteType_cascadeDeletesData() throws Exception {
        // 先新增一个临时字典类型
        Map<String, Object> typeBody = Map.of(
                "dictType", "it_tmp_type",
                "dictName", "IT临时类型",
                "status", 1
        );
        String createJson = authPost(TYPE_URL, typeBody)
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertCode200(createJson);
        Long typeId = ((Number) JsonPath.parse(createJson).read("$.data")).longValue();

        // 再新增一条关联数据
        Map<String, Object> dataBody = Map.of(
                "dictType", "it_tmp_type",
                "dictLabel", "临时项",
                "dictValue", "1",
                "status", 1
        );
        String dataJson = authPost(DATA_URL, dataBody)
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertCode200(dataJson);

        // 删除类型（级联删数据）
        String deleteJson = authDelete(TYPE_URL + "/" + typeId)
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertCode200(deleteJson);

        // 验证数据也被删除（查询返回空列表）
        String dataAfter = authGet(DATA_URL + "/it_tmp_type")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertCode200(dataAfter);
        List<Object> remaining = JsonPath.parse(dataAfter).read("$.data");
        org.assertj.core.api.Assertions.assertThat(remaining).isEmpty();
    }

    // ─── DICT-I-05 ────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("DICT-I-05 刷新缓存：code=200")
    void refreshCache_success() throws Exception {
        String json = authDelete(CACHE_URL + "/test_project_status")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
    }
}
