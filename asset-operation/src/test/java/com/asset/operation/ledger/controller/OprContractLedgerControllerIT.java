package com.asset.operation.ledger.controller;

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
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 合同台账 Controller 集成测试（LED-I）
 * 永久测试数据：
 *   台账 92001（待双签/待生成应收）、92002（已双签/已生成应收/待审核）
 *   台账 92003（已推送/已审核）、92004（已解约）
 *   应收 92001~92005、合同 91003、商铺 90001、项目 90001、商家 90002、品牌 90001
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
@DisplayName("合同台账 Controller 集成测试（LED-I）")
class OprContractLedgerControllerIT {

    private static final String BASE_URL = "/opr/ledgers";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    // ── LED-I-01：分页查询台账列表 ────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("LED-I-01: 分页查询台账列表-返回code=200且total>=4")
    void pageQuery_returnsAtLeast4() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(4)));
    }

    // ── LED-I-02：台账详情-含项目/商家/品牌名称 ───────────────────

    @Test
    @Order(2)
    @DisplayName("LED-I-02: 台账详情92003-含项目/商家/品牌名称")
    void getDetail_includesNames() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", 92003L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.projectName").isNotEmpty())
                .andExpect(jsonPath("$.data.merchantName").isNotEmpty())
                .andExpect(jsonPath("$.data.brandName").isNotEmpty());
    }

    // ── LED-I-03：从合同生成台账 ──────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("LED-I-03: 从合同91003生成台账-返回新台账ID")
    void fromContract_createsNewLedger() throws Exception {
        // 先删除已有台账，避免重复冲突
        jdbc.update("UPDATE opr_contract_ledger SET is_deleted=1 WHERE contract_id=91003");

        String response = mockMvc.perform(post(BASE_URL + "/from-contract/{contractId}", 91003L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber())
                .andReturn().getResponse().getContentAsString();

        // 提取新ID，验证 DB 状态
        Long newId = objectMapper.readTree(response).path("data").asLong();
        Integer status = jdbc.queryForObject(
                "SELECT status FROM opr_contract_ledger WHERE id=? AND is_deleted=0",
                Integer.class, newId);
        assertEquals(0, status, "新台账 status 应为 0(进行中)");
    }

    // ── LED-I-04：双签确认 ────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("LED-I-04: 双签确认92001-doubleSignStatus变为1")
    void doubleSign_updatesStatus() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{id}/double-sign", 92001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer dsStatus = jdbc.queryForObject(
                "SELECT double_sign_status FROM opr_contract_ledger WHERE id=92001",
                Integer.class);
        assertEquals(1, dsStatus, "双签状态应为 1");
    }

    // ── LED-I-05：生成应收计划 ────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("LED-I-05: 生成应收计划92001-返回应收条数>0")
    void generateReceivable_returnsCount() throws Exception {
        // 先双签（前置条件：doubleSignStatus=1）
        jdbc.update("UPDATE opr_contract_ledger SET double_sign_status=1 WHERE id=92001");

        mockMvc.perform(post(BASE_URL + "/{id}/generate-receivable", 92001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(greaterThan(0)));

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM opr_receivable_plan WHERE ledger_id=92001 AND is_deleted=0",
                Integer.class);
        assertTrue(count > 0, "应收计划记录数应 > 0");
    }

    // ── LED-I-06：审核通过 ────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("LED-I-06: 审核通过92002-auditStatus变为1")
    void auditPass_updatesStatus() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("auditStatus", 1); // 通过

        mockMvc.perform(put(BASE_URL + "/{id}/audit", 92002L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer auditStatus = jdbc.queryForObject(
                "SELECT audit_status FROM opr_contract_ledger WHERE id=92002",
                Integer.class);
        assertEquals(1, auditStatus, "审核状态应为 1(通过)");
    }

    // ── LED-I-07：推送应收 ────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("LED-I-07: 推送应收92002-未推送记录变为0条")
    void pushReceivable_allPushed() throws Exception {
        mockMvc.perform(post(BASE_URL + "/{id}/push-receivable", 92002L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer unpushedCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM opr_receivable_plan WHERE ledger_id=92002 AND push_status=0 AND is_deleted=0",
                Integer.class);
        assertEquals(0, unpushedCount, "推送后不应有 push_status=0 的记录");
    }

    // ── LED-I-08：录入一次性首款 ──────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("LED-I-08: 录入一次性首款92002-保存成功")
    void addOneTimePayment_savesRecord() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("feeItemId", 91001L);
        body.put("amount", 5000);
        body.put("billingStart", "2026-01-01");
        body.put("billingEnd", "2026-01-31");
        body.put("entryType", 1);
        body.put("remark", "IT测试首款");

        mockMvc.perform(post(BASE_URL + "/{id}/one-time-payment", 92002L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM opr_one_time_payment WHERE ledger_id=92002 AND is_deleted=0",
                Integer.class);
        assertTrue(count >= 1, "一次性首款记录应 >= 1");
    }

    // ── LED-I-09：查询台账下应收计划 ──────────────────────────────

    @Test
    @Order(9)
    @DisplayName("LED-I-09: 查询台账92002下应收计划-返回数组")
    void listReceivables_returnsArray() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}/receivables", 92002L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", not(empty())));
    }

    // ── LED-I-10：选择器搜索 ─────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("LED-I-10: 选择器搜索keyword=TZ-返回数组")
    void search_returnsResults() throws Exception {
        mockMvc.perform(get(BASE_URL + "/search")
                        .param("keyword", "TZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", not(empty())));
    }
}
