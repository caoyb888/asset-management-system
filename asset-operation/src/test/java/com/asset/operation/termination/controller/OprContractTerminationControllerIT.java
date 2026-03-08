package com.asset.operation.termination.controller;

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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 合同解约 Controller 集成测试（TRM-I）
 * 永久测试数据：
 *   解约 92001（到期/草稿/settlement=0）、92002（提前/审批中/settlement=25000）、92003（重签/已生效）
 *   清算明细 92001~92003、合同 91003、台账 92003、商铺 90001、项目 90001
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
@DisplayName("合同解约 Controller 集成测试（TRM-I）")
class OprContractTerminationControllerIT {

    private static final String BASE_URL = "/opr/terminations";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    // ── TRM-I-01：分页查询解约列表 ────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("TRM-I-01: 分页查询解约列表-返回code=200且total>=3")
    void pageQuery_returnsAtLeast3() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(3)));
    }

    // ── TRM-I-02：解约详情-含清算明细 ────────────────────────────

    @Test
    @Order(2)
    @DisplayName("TRM-I-02: 解约详情92002-含清算明细settlements")
    void getDetail_includesSettlements() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", 92002L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.settlements").isArray())
                .andExpect(jsonPath("$.data.settlements", not(empty())));
    }

    // ── TRM-I-03：新增到期解约 ───────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("TRM-I-03: 新增到期解约-返回新ID且status=0")
    void create_returnsNewId() throws Exception {
        // 先删除已有同合同解约，避免重复校验
        jdbc.update("UPDATE opr_contract_termination SET is_deleted=1 WHERE contract_id=91003 AND status IN (0,1)");

        Map<String, Object> body = new HashMap<>();
        body.put("contractId", 91003L);
        body.put("terminationType", 1);
        body.put("terminationDate", "2026-12-31");
        body.put("reason", "IT测试-到期解约");

        String response = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber())
                .andReturn().getResponse().getContentAsString();

        Long newId = objectMapper.readTree(response).path("data").asLong();
        Integer status = jdbc.queryForObject(
                "SELECT status FROM opr_contract_termination WHERE id=? AND is_deleted=0",
                Integer.class, newId);
        assertEquals(0, status, "新解约单 status 应为 0(草稿)");
    }

    // ── TRM-I-04：编辑草稿解约 ───────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("TRM-I-04: 编辑草稿解约92001-日期更新且清算明细清空")
    void updateDraft_updatesDateAndClearsSettlement() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("contractId", 91003L);
        body.put("terminationType", 1);
        body.put("terminationDate", "2026-11-30");
        body.put("reason", "IT测试-编辑解约");

        mockMvc.perform(put(BASE_URL + "/{id}", 92001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证日期已更新
        String dateStr = jdbc.queryForObject(
                "SELECT termination_date FROM opr_contract_termination WHERE id=92001",
                String.class);
        assertTrue(dateStr.contains("2026-11-30"), "解约日期应更新为 2026-11-30");

        // 验证清算明细被清空
        Integer settlementCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM opr_termination_settlement WHERE termination_id=92001 AND is_deleted=0",
                Integer.class);
        assertEquals(0, settlementCount, "编辑后清算明细应被清空");
    }

    // ── TRM-I-05：计算清算金额 ───────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("TRM-I-05: 计算清算金额92001-settlementAmount变为非NULL")
    void calculateSettlement_updatesAmount() throws Exception {
        mockMvc.perform(post(BASE_URL + "/{id}/calculate-settlement", 92001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        BigDecimal amount = jdbc.queryForObject(
                "SELECT settlement_amount FROM opr_contract_termination WHERE id=92001",
                BigDecimal.class);
        assertNotNull(amount, "清算金额应已计算（非NULL）");
    }

    // ── TRM-I-06：提交审批-草稿→审批中 ──────────────────────────

    @Test
    @Order(6)
    @DisplayName("TRM-I-06: 提交审批92001-status变为1")
    void submitApproval_statusBecomes1() throws Exception {
        // 前置条件：settlement_amount 不能为 null
        jdbc.update("UPDATE opr_contract_termination SET settlement_amount=20000 WHERE id=92001");

        mockMvc.perform(post(BASE_URL + "/{id}/submit-approval", 92001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer status = jdbc.queryForObject(
                "SELECT status FROM opr_contract_termination WHERE id=92001",
                Integer.class);
        assertEquals(1, status, "提交审批后 status 应为 1");
    }

    // ── TRM-I-07：审批回调-通过→执行解约 ─────────────────────────

    @Test
    @Order(7)
    @DisplayName("TRM-I-07: 审批回调通过92002-status变为2，合同/商铺状态联动")
    void approvalCallback_pass_executesTermination() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("status", 2); // 通过

        mockMvc.perform(post(BASE_URL + "/{id}/approval-callback", 92002L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证解约单状态
        Integer status = jdbc.queryForObject(
                "SELECT status FROM opr_contract_termination WHERE id=92002",
                Integer.class);
        assertEquals(2, status, "审批通过后解约单 status 应为 2(已生效)");

        // 验证合同状态联动（写 inv_lease_contract）
        Integer contractStatus = jdbc.queryForObject(
                "SELECT status FROM inv_lease_contract WHERE id=91003 AND is_deleted=0",
                Integer.class);
        assertEquals(5, contractStatus, "合同 status 应为 5(已解约)");

        // 验证商铺状态联动（写 biz_shop）
        Integer shopStatus = jdbc.queryForObject(
                "SELECT shop_status FROM biz_shop WHERE id=90001 AND is_deleted=0",
                Integer.class);
        assertEquals(0, shopStatus, "商铺 shop_status 应为 0(空置)");
    }

    // ── TRM-I-08：审批回调-驳回 ──────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("TRM-I-08: 审批回调驳回92002-status变为3")
    void approvalCallback_reject_statusBecomes3() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("status", 3); // 驳回

        mockMvc.perform(post(BASE_URL + "/{id}/approval-callback", 92002L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer status = jdbc.queryForObject(
                "SELECT status FROM opr_contract_termination WHERE id=92002",
                Integer.class);
        assertEquals(3, status, "审批驳回后 status 应为 3");
    }
}
