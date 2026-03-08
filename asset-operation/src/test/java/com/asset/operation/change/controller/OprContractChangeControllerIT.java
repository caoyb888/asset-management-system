package com.asset.operation.change.controller;

import com.asset.operation.OperationApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 合同变更 Controller 集成测试（CHG-I）
 * 永久测试数据：
 *   变更 92001（草稿/RENT）、92002（审批中/TERM）、92003（已通过/RENT）
 *   变更类型 92001~92003、变更明细 92001~92002
 *   合同 91003、台账 92003、项目 90001
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = OperationApplication.class,
        properties = {
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.nacos.config.enabled=false",
                "spring.cloud.nacos.config.import-check.enabled=false"
        }
)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("合同变更 Controller 集成测试（CHG-I）")
class OprContractChangeControllerIT {

    private static final String BASE_URL = "/opr/contract-changes";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    // ── CHG-I-01：分页查询变更列表 ────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("CHG-I-01: 分页查询变更列表-返回code=200且total>=3")
    void pageQuery_returnsAtLeast3() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(3)));
    }

    // ── CHG-I-02：变更详情-含明细和类型 ───────────────────────────

    @Test
    @Order(2)
    @DisplayName("CHG-I-02: 变更详情92001-含changeTypeCodes和details")
    void getDetail_includesTypesAndDetails() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", 92001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.changeTypeCodes").isArray())
                .andExpect(jsonPath("$.data.changeTypeCodes", not(empty())))
                .andExpect(jsonPath("$.data.details").isArray())
                .andExpect(jsonPath("$.data.details", not(empty())));
    }

    // ── CHG-I-03：新增变更-租金类型 ───────────────────────────────

    @Test
    @Order(3)
    @DisplayName("CHG-I-03: 新增变更-租金类型-返回新变更ID")
    void create_rentChange_returnsNewId() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("contractId", 91003L);
        body.put("ledgerId", 92003L);
        body.put("changeTypeCodes", List.of("RENT"));
        body.put("effectiveDate", "2026-05-01");
        body.put("reason", "IT测试-租金调整");
        body.put("changeFields", Map.of(
                "newRentAmount", "20000",
                "old_newRentAmount", "15000"
        ));

        String response = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber())
                .andReturn().getResponse().getContentAsString();

        Long newId = objectMapper.readTree(response).path("data").asLong();
        Integer status = jdbc.queryForObject(
                "SELECT status FROM opr_contract_change WHERE id=? AND is_deleted=0",
                Integer.class, newId);
        assertEquals(0, status, "新变更单 status 应为 0(草稿)");
    }

    // ── CHG-I-04：编辑草稿变更 ────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("CHG-I-04: 编辑草稿变更92001-类型/明细被重建")
    void updateDraft_rebuildsTypesAndDetails() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("contractId", 91003L);
        body.put("ledgerId", 92003L);
        body.put("changeTypeCodes", List.of("RENT", "TERM"));
        body.put("effectiveDate", "2026-05-01");
        body.put("reason", "IT测试-编辑变更");
        body.put("changeFields", Map.of(
                "newRentAmount", "22000",
                "newContractEnd", "2027-12-31"
        ));

        mockMvc.perform(put(BASE_URL + "/{id}", 92001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证类型被重建为 2 条（RENT + TERM）
        Integer typeCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM opr_contract_change_type WHERE change_id=92001 AND is_deleted=0",
                Integer.class);
        assertEquals(2, typeCount, "类型应重建为 2 条（RENT+TERM）");
    }

    // ── CHG-I-05：预览变更影响 ────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("CHG-I-05: 预览变更影响92001-返回affectedPlanCount")
    void previewImpact_returnsVO() throws Exception {
        mockMvc.perform(post(BASE_URL + "/{id}/preview-impact", 92001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.affectedPlanCount").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.impactDesc").isNotEmpty());
    }

    // ── CHG-I-06：提交审批-草稿→审批中 ───────────────────────────

    @Test
    @Order(6)
    @DisplayName("CHG-I-06: 提交审批92001-status变为1")
    void submitApproval_statusBecomes1() throws Exception {
        mockMvc.perform(post(BASE_URL + "/{id}/submit-approval", 92001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer status = jdbc.queryForObject(
                "SELECT status FROM opr_contract_change WHERE id=92001",
                Integer.class);
        assertEquals(1, status, "提交审批后 status 应为 1");
    }

    // ── CHG-I-07：审批回调-通过→应收重算 ──────────────────────────

    @Test
    @Order(7)
    @DisplayName("CHG-I-07: 审批回调通过92002-status变为2")
    void approvalCallback_pass_statusBecomes2() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("status", 2); // 通过

        mockMvc.perform(post(BASE_URL + "/{id}/approval-callback", 92002L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer status = jdbc.queryForObject(
                "SELECT status FROM opr_contract_change WHERE id=92002",
                Integer.class);
        assertEquals(2, status, "审批通过后 status 应为 2");
    }

    // ── CHG-I-08：审批回调-驳回 ───────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("CHG-I-08: 审批回调驳回92002-status变为3")
    void approvalCallback_reject_statusBecomes3() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("status", 3); // 驳回

        mockMvc.perform(post(BASE_URL + "/{id}/approval-callback", 92002L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer status = jdbc.queryForObject(
                "SELECT status FROM opr_contract_change WHERE id=92002",
                Integer.class);
        assertEquals(3, status, "审批驳回后 status 应为 3");
    }

    // ── CHG-I-09：变更历史时间线 ──────────────────────────────────

    @Test
    @Order(9)
    @DisplayName("CHG-I-09: 变更历史时间线91003-返回数组")
    void history_returnsArray() throws Exception {
        mockMvc.perform(get(BASE_URL + "/history/{contractId}", 91003L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", not(empty())));
    }
}
