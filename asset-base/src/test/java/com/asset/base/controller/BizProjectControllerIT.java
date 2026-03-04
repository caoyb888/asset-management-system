package com.asset.base.controller;

import com.asset.base.BaseApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 项目管理接口集成测试
 *
 * <ul>
 *   <li>使用真实 MySQL 数据库，每个测试方法均在事务内执行并自动回滚</li>
 *   <li>通过 {@code addFilters=false} 完全跳过 Spring Security 过滤器</li>
 *   <li>Nacos 注册/配置中心在 application-test.yml 中已禁用</li>
 * </ul>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = BaseApplication.class,
        properties = {
                // 在上下文初始化最早期阶段禁用 Nacos，防止连接超时导致启动失败
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.nacos.config.enabled=false",
                "spring.cloud.nacos.config.import-check.enabled=false"
        }
)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@DisplayName("项目管理接口集成测试")
class BizProjectControllerIT {

    private static final String BASE_URL = "/base/projects";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    /** 测试用公司 ID（每个测试方法前插入，测试结束后随事务回滚） */
    private Long testCompanyId;

    /** 预置的测试项目 ID */
    private Long testProjectId;

    // ─────────────────────────────────────────────────────────────────────────
    // 前置数据准备（在本测试事务内执行，测试结束自动回滚）
    // ─────────────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        // 1. 插入测试公司（biz_project.company_id 外键依赖）
        jdbc.update(
                "INSERT INTO sys_company (company_code, company_name) VALUES ('TEST_CO', '测试公司')"
        );
        testCompanyId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        // 2. 插入测试项目（operation_status=1 开业，方便验证枚举名称）
        jdbc.update("""
                        INSERT INTO biz_project
                            (project_code, project_name, company_id, operation_status,
                             is_deleted, created_by, updated_by, created_at, updated_at)
                        VALUES ('P001', '测试项目Alpha', ?, 1, 0, 0, 0, NOW(), NOW())
                        """,
                testCompanyId);
        testProjectId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 分页查询
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("分页查询-无过滤条件-返回分页结构")
    void listProjects_noFilter_returnsPageStructure() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("分页查询-按名称模糊过滤-只返回匹配记录")
    void listProjects_filterByName_returnsMatchedOnly() throws Exception {
        mockMvc.perform(get(BASE_URL).param("projectName", "Alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.records[0].projectName", containsString("Alpha")));
    }

    @Test
    @DisplayName("分页查询-按运营状态过滤-只返回对应状态记录")
    void listProjects_filterByStatus_returnsMatchedOnly() throws Exception {
        mockMvc.perform(get(BASE_URL).param("operationStatus", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.records[0].operationStatus").value(1));
    }

    @Test
    @DisplayName("分页查询-名称无匹配-返回空记录列表")
    void listProjects_nameNoMatch_returnsEmptyRecords() throws Exception {
        mockMvc.perform(get(BASE_URL).param("projectName", "NOTEXIST_XYZ_99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isEmpty());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 详情查询
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("详情查询-存在的项目-返回VO并含枚举名称")
    void getProject_exists_returnsVOWithEnumNames() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", testProjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.projectCode").value("P001"))
                .andExpect(jsonPath("$.data.projectName").value("测试项目Alpha"))
                .andExpect(jsonPath("$.data.operationStatusName").value("开业"));
    }

    @Test
    @DisplayName("详情查询-ID不存在-返回业务异常码500")
    void getProject_notFound_returnsBizError() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", 999_999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("不存在")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 新增项目
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("新增-合法数据-返回新ID并写入数据库")
    void createProject_validData_returnsIdAndPersists() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("projectCode", "P002");
        body.put("projectName", "新项目Beta");
        body.put("companyId", testCompanyId);
        body.put("operationStatus", 0);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber());

        // 验证数据库中确实写入
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM biz_project WHERE project_code='P002' AND is_deleted=0",
                Integer.class
        );
        assertEquals(1, count, "新增的项目应在数据库中存在");
    }

    @Test
    @DisplayName("新增-缺少项目编号-返回400校验失败")
    void createProject_missingProjectCode_returns400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("projectName", "缺少编号的项目");
        body.put("companyId", testCompanyId);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg", containsString("projectCode")));
    }

    @Test
    @DisplayName("新增-缺少项目名称-返回400校验失败")
    void createProject_missingProjectName_returns400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("projectCode", "P003");
        body.put("companyId", testCompanyId);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg", containsString("projectName")));
    }

    @Test
    @DisplayName("新增-缺少所属公司-返回400校验失败")
    void createProject_missingCompanyId_returns400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("projectCode", "P004");
        body.put("projectName", "无公司归属项目");

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg", containsString("companyId")));
    }

    @Test
    @DisplayName("新增-项目编号重复-返回业务异常码500")
    void createProject_duplicateCode_returnsBizError() throws Exception {
        // P001 已在 setUp 中插入
        Map<String, Object> body = new HashMap<>();
        body.put("projectCode", "P001");
        body.put("projectName", "编号重复的项目");
        body.put("companyId", testCompanyId);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("已存在")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 编辑项目
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("编辑-合法数据-修改成功并更新数据库")
    void updateProject_validData_updatesDatabase() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("projectCode", "P001");
        body.put("projectName", "修改后的名称");
        body.put("companyId", testCompanyId);
        body.put("operationStatus", 2); // 停业

        mockMvc.perform(put(BASE_URL + "/{id}", testProjectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证数据库字段已更新
        String updatedName = jdbc.queryForObject(
                "SELECT project_name FROM biz_project WHERE id=?",
                String.class, testProjectId
        );
        assertEquals("修改后的名称", updatedName, "project_name 应已被更新");
    }

    @Test
    @DisplayName("编辑-ID不存在-返回业务异常码500")
    void updateProject_notFound_returnsBizError() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("projectCode", "P001");
        body.put("projectName", "不存在的项目");
        body.put("companyId", testCompanyId);

        mockMvc.perform(put(BASE_URL + "/{id}", 999_999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("不存在")));
    }

    @Test
    @DisplayName("编辑-修改编号为已存在编号-返回业务异常码500")
    void updateProject_changeToDuplicateCode_returnsBizError() throws Exception {
        // 插入第二个项目 P002
        jdbc.update("""
                        INSERT INTO biz_project
                            (project_code, project_name, company_id, operation_status,
                             is_deleted, created_by, updated_by, created_at, updated_at)
                        VALUES ('P002', '项目Beta', ?, 0, 0, 0, 0, NOW(), NOW())
                        """,
                testCompanyId);
        Long secondId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        // 尝试将 P002 改为 P001（P001 已被占用）
        Map<String, Object> body = new HashMap<>();
        body.put("projectCode", "P001");
        body.put("projectName", "改成P001编号");
        body.put("companyId", testCompanyId);

        mockMvc.perform(put(BASE_URL + "/{id}", secondId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("已存在")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 逻辑删除
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("逻辑删除-存在的项目-is_deleted置为1")
    void deleteProject_exists_setsIsDeletedFlag() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", testProjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 绕过 MyBatis-Plus 逻辑删除过滤，直接查 is_deleted 原始值
        Integer isDeleted = jdbc.queryForObject(
                "SELECT is_deleted FROM biz_project WHERE id=?",
                Integer.class, testProjectId
        );
        assertEquals(1, isDeleted, "逻辑删除后 is_deleted 应为 1");
    }

    @Test
    @DisplayName("逻辑删除-ID不存在-返回业务异常码500")
    void deleteProject_notExists_returnsBizError() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", 999_999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("不存在")));
    }

    @Test
    @DisplayName("逻辑删除-已删除的项目-返回业务异常码500")
    void deleteProject_alreadyDeleted_returnsBizError() throws Exception {
        // 先手动将项目标记为已删除
        jdbc.update("UPDATE biz_project SET is_deleted=1 WHERE id=?", testProjectId);

        mockMvc.perform(delete(BASE_URL + "/{id}", testProjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("不存在")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 审计字段自动填充
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("新增-审计字段由MetaObjectHandler自动填充-不为空")
    void createProject_auditFields_areAutoFilled() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("projectCode", "PAUDIT");
        body.put("projectName", "审计字段验证项目");
        body.put("companyId", testCompanyId);

        String responseStr = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long newId = objectMapper.readTree(responseStr).path("data").longValue();

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT created_at, updated_at, created_by, updated_by FROM biz_project WHERE id=?",
                newId
        );

        assertNotNull(row.get("created_at"),  "created_at 不应为空");
        assertNotNull(row.get("updated_at"),  "updated_at 不应为空");
        assertNotNull(row.get("created_by"),  "created_by 不应为空");
        assertNotNull(row.get("updated_by"),  "updated_by 不应为空");
    }
}
