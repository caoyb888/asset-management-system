package com.asset.system.menu;

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
 * §4.7 菜单管理 — Controller 集成测试
 * MENU-I-01 ~ MENU-I-04
 *
 * 永久测试数据：sys_menu 91001~91009
 *   91001 系统管理 [M, parent=0] — 含多个子菜单，不可删除
 *   91002 用户管理 [C, parent=91001]
 *   91003~91005 按钮 [F, parent=91002]
 *   91006~91007 菜单 [C, parent=91001]
 *   91008 隐藏菜单 [visible=0]
 *   91009 停用菜单 [status=0]
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
@Rollback
@DisplayName("§4.7 菜单管理集成测试")
class SysMenuControllerIT {

    private static final String BASE_URL  = "/sys/menus";
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

    // ─── MENU-I-01 ────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("MENU-I-01 获取菜单树：树形结构正确，根节点含 children")
    void getMenuTree_returnsTreeStructure() throws Exception {
        String json = authGet(BASE_URL)
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
        List<Object> roots = JsonPath.parse(json).read("$.data");
        org.assertj.core.api.Assertions.assertThat(roots).isNotEmpty();
        // 根节点 91001 应有子节点
        List<Object> children = JsonPath.parse(json).read("$.data[0].children");
        org.assertj.core.api.Assertions.assertThat(children).isNotEmpty();
    }

    // ─── MENU-I-02 ────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("MENU-I-02 新增菜单：code=200，返回新ID")
    void createMenu_success() throws Exception {
        Map<String, Object> body = Map.of(
                "menuName", "IT测试菜单",
                "menuType", "C",
                "parentId", 91001,
                "path", "/test/it-menu",
                "status", 1
        );
        String json = authPost(BASE_URL, body)
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
        Long newId = ((Number) JsonPath.parse(json).read("$.data")).longValue();
        org.assertj.core.api.Assertions.assertThat(newId).isPositive();
    }

    // ─── MENU-I-03 ────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("MENU-I-03 删除有子菜单的91001：code!=200，含子菜单")
    void deleteMenu_hasChildren_fails() throws Exception {
        String json = authDelete(BASE_URL + "/91001")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        int code = JsonPath.parse(json).read("$.code");
        org.assertj.core.api.Assertions.assertThat(code).isNotEqualTo(200);
        String msg = JsonPath.parse(json).read("$.msg");
        org.assertj.core.api.Assertions.assertThat(msg).contains("子菜单");
    }

    // ─── MENU-I-04 ────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("MENU-I-04 获取权限标识：userId=91003 返回非空 perms 列表")
    void getPerms_returnsPermsList() throws Exception {
        String json = authGet(BASE_URL + "/perms?userId=91003")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertCode200(json);
        List<String> perms = JsonPath.parse(json).read("$.data");
        org.assertj.core.api.Assertions.assertThat(perms).isNotEmpty();
    }
}
