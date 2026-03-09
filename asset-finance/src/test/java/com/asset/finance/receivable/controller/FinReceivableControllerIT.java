package com.asset.finance.receivable.controller;

import com.asset.finance.FinanceControllerTestBase;
import com.asset.finance.receivable.entity.FinReceivable;
import com.asset.finance.receivable.entity.FinReceivableDeduction;
import com.asset.finance.receivable.entity.FinReceivableAdjustment;
import com.asset.finance.receivable.mapper.FinReceivableAdjustmentMapper;
import com.asset.finance.receivable.mapper.FinReceivableDeductionMapper;
import com.asset.finance.receivable.mapper.FinReceivableMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 应收管理 Controller 集成测试
 *
 * <p>覆盖场景：
 * <ol>
 *   <li>RCV-I-01：分页列表</li>
 *   <li>RCV-I-02：详情查询</li>
 *   <li>RCV-I-03：合同汇总</li>
 *   <li>RCV-I-04：欠费统计</li>
 *   <li>RCV-I-05：提交减免申请</li>
 *   <li>RCV-I-06：提交调整申请</li>
 *   <li>RCV-I-07：批量标记已打印</li>
 *   <li>RCV-I-08：手动刷新逾期</li>
 * </ol>
 */
@DisplayName("应收管理 Controller 集成测试")
class FinReceivableControllerIT extends FinanceControllerTestBase {

    @Autowired
    private FinReceivableMapper receivableMapper;

    @Autowired
    private FinReceivableDeductionMapper deductionMapper;

    @Autowired
    private FinReceivableAdjustmentMapper adjustmentMapper;

    private static final Long CONTRACT_ID = 10010L;
    private static final Long MERCHANT_ID = 20010L;
    private static final Long PROJECT_ID = 1L;

    private FinReceivable ar1, ar2, ar3;

    @BeforeEach
    void setUp() {
        ar1 = insertReceivable(new BigDecimal("1000.00"), LocalDate.now().plusDays(30), 0);
        ar2 = insertReceivable(new BigDecimal("2000.00"), LocalDate.now().plusDays(60), 0);
        ar3 = insertReceivable(new BigDecimal("3000.00"), LocalDate.now().minusDays(10), 0); // 逾期
    }

    private FinReceivable insertReceivable(BigDecimal amount, LocalDate dueDate, int status) {
        FinReceivable r = new FinReceivable();
        r.setReceivableCode("AR-IT-" + System.nanoTime());
        r.setContractId(CONTRACT_ID);
        r.setProjectId(PROJECT_ID);
        r.setMerchantId(MERCHANT_ID);
        r.setOriginalAmount(amount);
        r.setAdjustAmount(BigDecimal.ZERO);
        r.setDeductionAmount(BigDecimal.ZERO);
        r.setActualAmount(amount);
        r.setReceivedAmount(BigDecimal.ZERO);
        r.setOutstandingAmount(amount);
        r.setDueDate(dueDate);
        r.setStatus(status);
        r.setAccrualMonth(LocalDate.now().toString().substring(0, 7));
        receivableMapper.insert(r);
        return r;
    }

    // ─── RCV-I-01：分页列表 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("RCV-I-01：GET /fin/receivables 返回分页数据")
    void page_shouldReturnPaginatedList() throws Exception {
        mockMvc.perform(get("/fin/receivables")
                        .param("pageNum", "1")
                        .param("pageSize", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)));
    }

    // ─── RCV-I-02：详情查询 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("RCV-I-02：GET /fin/receivables/{id} 返回完整字段")
    void getById_shouldReturnDetail() throws Exception {
        mockMvc.perform(get("/fin/receivables/{id}", ar1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(ar1.getId()))
                .andExpect(jsonPath("$.data.receivableCode").isNotEmpty())
                .andExpect(jsonPath("$.data.originalAmount").value(1000.00));
    }

    // ─── RCV-I-03：合同汇总 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("RCV-I-03：GET /fin/receivables/summary 返回合同汇总")
    void summary_shouldReturnContractSummary() throws Exception {
        mockMvc.perform(get("/fin/receivables/summary")
                        .param("contractId", CONTRACT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].contractId").value(CONTRACT_ID));
    }

    // ─── RCV-I-04：欠费统计 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("RCV-I-04：GET /fin/receivables/overdue-statistics 返回三档分布")
    void overdueStatistics_shouldReturnThreeBuckets() throws Exception {
        mockMvc.perform(get("/fin/receivables/overdue-statistics")
                        .param("projectId", PROJECT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalOverdueCount").isNumber())
                .andExpect(jsonPath("$.data.overdue30Amount").isNumber());
    }

    // ─── RCV-I-05：提交减免申请 ──────────────────────────────────────────────────

    @Test
    @DisplayName("RCV-I-05：POST /fin/receivables/deduction 创建减免单")
    void applyDeduction_shouldCreatePendingDeduction() throws Exception {
        String body = objectMapper.writeValueAsString(
                java.util.Map.of(
                        "receivableId", ar1.getId(),
                        "deductionAmount", 200.00,
                        "reason", "IT测试减免"
                ));

        String response = mockMvc.perform(post("/fin/receivables/deduction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber())
                .andReturn().getResponse().getContentAsString();

        // 从响应提取减免单ID，验证DB状态
        Long deductionId = objectMapper.readTree(response).get("data").asLong();
        FinReceivableDeduction deduction = deductionMapper.selectById(deductionId);
        assertThat(deduction.getStatus())
                .as("减免单状态应为0(待审批)")
                .isEqualTo(0);
    }

    // ─── RCV-I-06：提交调整申请 ──────────────────────────────────────────────────

    @Test
    @DisplayName("RCV-I-06：POST /fin/receivables/adjustment 创建调整单")
    void applyAdjustment_shouldCreatePendingAdjustment() throws Exception {
        String body = objectMapper.writeValueAsString(
                java.util.Map.of(
                        "receivableId", ar2.getId(),
                        "adjustType", 1,
                        "adjustAmount", 100.00,
                        "reason", "IT测试调增"
                ));

        String response = mockMvc.perform(post("/fin/receivables/adjustment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber())
                .andReturn().getResponse().getContentAsString();

        Long adjustmentId = objectMapper.readTree(response).get("data").asLong();
        FinReceivableAdjustment adjustment = adjustmentMapper.selectById(adjustmentId);
        assertThat(adjustment.getStatus())
                .as("调整单状态应为0(待审批)")
                .isEqualTo(0);
    }

    // ─── RCV-I-07：批量标记已打印 ────────────────────────────────────────────────

    @Test
    @DisplayName("RCV-I-07：POST /fin/receivables/mark-printed 批量标记")
    void markPrinted_shouldUpdateFlag() throws Exception {
        String body = objectMapper.writeValueAsString(
                java.util.List.of(ar1.getId(), ar2.getId()));

        mockMvc.perform(post("/fin/receivables/mark-printed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        FinReceivable updated = receivableMapper.selectById(ar1.getId());
        assertThat(updated.getIsPrinted())
                .as("应收记录应标记为已打印")
                .isEqualTo(1);
    }

    // ─── RCV-I-08：手动刷新逾期 ──────────────────────────────────────────────────

    @Test
    @DisplayName("RCV-I-08：POST /fin/receivables/refresh-overdue 刷新逾期")
    void refreshOverdue_shouldSucceed() throws Exception {
        mockMvc.perform(post("/fin/receivables/refresh-overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
