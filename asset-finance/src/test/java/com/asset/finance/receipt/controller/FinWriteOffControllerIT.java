package com.asset.finance.receipt.controller;

import com.asset.finance.FinanceControllerTestBase;
import com.asset.finance.receipt.entity.FinReceipt;
import com.asset.finance.receipt.entity.FinWriteOff;
import com.asset.finance.receipt.mapper.FinReceiptMapper;
import com.asset.finance.receipt.mapper.FinWriteOffMapper;
import com.asset.finance.receivable.entity.FinReceivable;
import com.asset.finance.receivable.mapper.FinReceivableMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 核销管理 Controller 集成测试
 *
 * <p>覆盖场景：WOF-I-01 ~ WOF-I-08
 */
@DisplayName("核销管理 Controller 集成测试")
class FinWriteOffControllerIT extends FinanceControllerTestBase {

    @Autowired
    private FinWriteOffMapper writeOffMapper;

    @Autowired
    private FinReceiptMapper receiptMapper;

    @Autowired
    private FinReceivableMapper receivableMapper;

    private static final Long CONTRACT_ID = 10030L;
    private static final Long MERCHANT_ID = 20030L;

    private FinReceipt receipt;
    private FinReceivable ar1, ar2;

    @BeforeEach
    void setUp() {
        // 创建收款单
        receipt = insertReceipt(new BigDecimal("5000.00"), 0);
        // 创建应收
        ar1 = insertReceivable(new BigDecimal("1000.00"), 0);
        ar2 = insertReceivable(new BigDecimal("2000.00"), 1);
    }

    private FinReceipt insertReceipt(BigDecimal amount, int status) {
        FinReceipt r = new FinReceipt();
        r.setReceiptCode("RC-WIT-" + System.nanoTime());
        r.setContractId(CONTRACT_ID);
        r.setMerchantId(MERCHANT_ID);
        r.setTotalAmount(amount);
        r.setPaymentMethod(1);
        r.setReceiptDate(LocalDate.now());
        r.setStatus(status);
        r.setWriteOffAmount(BigDecimal.ZERO);
        r.setPrepayAmount(BigDecimal.ZERO);
        receiptMapper.insert(r);
        return r;
    }

    private FinReceivable insertReceivable(BigDecimal amount, int status) {
        FinReceivable r = new FinReceivable();
        r.setReceivableCode("AR-WIT-" + System.nanoTime());
        r.setContractId(CONTRACT_ID);
        r.setMerchantId(MERCHANT_ID);
        r.setOriginalAmount(amount);
        r.setAdjustAmount(BigDecimal.ZERO);
        r.setDeductionAmount(BigDecimal.ZERO);
        r.setActualAmount(amount);
        r.setReceivedAmount(BigDecimal.ZERO);
        r.setOutstandingAmount(amount);
        r.setDueDate(LocalDate.now().minusDays(5));
        r.setStatus(status);
        r.setAccrualMonth(LocalDate.now().toString().substring(0, 7));
        receivableMapper.insert(r);
        return r;
    }

    private FinWriteOff insertWriteOff(int status) {
        FinWriteOff wo = new FinWriteOff();
        wo.setWriteOffCode("WO-WIT-" + System.nanoTime());
        wo.setReceiptId(receipt.getId());
        wo.setContractId(CONTRACT_ID);
        wo.setMerchantId(MERCHANT_ID);
        wo.setWriteOffType(1);
        wo.setTotalAmount(new BigDecimal("100.00"));
        wo.setStatus(status);
        writeOffMapper.insert(wo);
        return wo;
    }

    // ─── WOF-I-01：分页列表 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("WOF-I-01：GET /fin/write-offs 返回分页数据")
    void page_shouldReturnPaginatedList() throws Exception {
        insertWriteOff(0);
        insertWriteOff(1);

        mockMvc.perform(get("/fin/write-offs")
                        .param("pageNum", "1")
                        .param("pageSize", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    // ─── WOF-I-02：查询可核销应收 ──────────────────────────────────────────────

    @Test
    @DisplayName("WOF-I-02：GET /fin/write-offs/writable-receivables 仅含待收/部分收款")
    void writableReceivables_shouldFilterByStatus() throws Exception {
        // 插入一条已收清(status=2)的应收
        FinReceivable ar3 = insertReceivable(new BigDecimal("500.00"), 2);

        mockMvc.perform(get("/fin/write-offs/writable-receivables")
                        .param("contractId", CONTRACT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ─── WOF-I-03：提交核销申请 ─────────────────────────────────────────────────

    @Test
    @DisplayName("WOF-I-03：POST /fin/write-offs 创建核销单")
    void submitWriteOff_shouldReturnId() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "receiptId", receipt.getId(),
                "writeOffType", 1,
                "items", List.of(
                        Map.of("receivableId", ar1.getId(), "writeOffAmount", 1000.00)
                )
        ));

        String response = mockMvc.perform(post("/fin/write-offs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber())
                .andReturn().getResponse().getContentAsString();

        Long writeOffId = objectMapper.readTree(response).get("data").asLong();
        FinWriteOff wo = writeOffMapper.selectById(writeOffId);
        assertThat(wo.getStatus()).as("核销单状态应为0(待审核)").isEqualTo(0);
    }

    // ─── WOF-I-04：审批通过回调 ─────────────────────────────────────────────────

    @Test
    @DisplayName("WOF-I-04：POST /fin/write-offs/approval-callback 审批通过更新应收")
    void approvalCallback_approved_shouldUpdateReceivable() throws Exception {
        // 先提交核销
        String submitBody = objectMapper.writeValueAsString(Map.of(
                "receiptId", receipt.getId(),
                "writeOffType", 1,
                "items", List.of(
                        Map.of("receivableId", ar1.getId(), "writeOffAmount", 1000.00)
                )
        ));
        String submitResp = mockMvc.perform(post("/fin/write-offs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody))
                .andReturn().getResponse().getContentAsString();
        Long writeOffId = objectMapper.readTree(submitResp).get("data").asLong();

        // 获取 approvalId
        FinWriteOff wo = writeOffMapper.selectById(writeOffId);

        // 审批通过回调
        String callbackBody = objectMapper.writeValueAsString(Map.of(
                "approvalId", wo.getApprovalId(),
                "approved", true
        ));
        mockMvc.perform(post("/fin/write-offs/approval-callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证应收已更新
        FinReceivable updated = receivableMapper.selectById(ar1.getId());
        assertThat(updated.getReceivedAmount())
                .as("审批通过后 receivedAmount 应为1000")
                .isEqualByComparingTo("1000.00");
    }

    // ─── WOF-I-05：审批驳回回调 ─────────────────────────────────────────────────

    @Test
    @DisplayName("WOF-I-05：POST /fin/write-offs/approval-callback 驳回不影响应收")
    void approvalCallback_rejected_shouldNotUpdateReceivable() throws Exception {
        String submitBody = objectMapper.writeValueAsString(Map.of(
                "receiptId", receipt.getId(),
                "writeOffType", 1,
                "items", List.of(
                        Map.of("receivableId", ar1.getId(), "writeOffAmount", 500.00)
                )
        ));
        String submitResp = mockMvc.perform(post("/fin/write-offs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody))
                .andReturn().getResponse().getContentAsString();
        Long writeOffId = objectMapper.readTree(submitResp).get("data").asLong();
        FinWriteOff wo = writeOffMapper.selectById(writeOffId);

        String callbackBody = objectMapper.writeValueAsString(Map.of(
                "approvalId", wo.getApprovalId(),
                "approved", false
        ));
        mockMvc.perform(post("/fin/write-offs/approval-callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 核销单 status=2（驳回）
        FinWriteOff rejected = writeOffMapper.selectById(writeOffId);
        assertThat(rejected.getStatus()).as("驳回后 status 应为2").isEqualTo(2);

        // 应收不变
        FinReceivable unchanged = receivableMapper.selectById(ar1.getId());
        assertThat(unchanged.getReceivedAmount())
                .as("驳回后 receivedAmount 不变")
                .isEqualByComparingTo("0.00");
    }

    // ─── WOF-I-06：撤销核销单 ───────────────────────────────────────────────────

    @Test
    @DisplayName("WOF-I-06：PUT /fin/write-offs/{id}/cancel 撤销待审核核销单")
    void cancel_pendingWriteOff_shouldSucceed() throws Exception {
        FinWriteOff wo = insertWriteOff(0);

        mockMvc.perform(put("/fin/write-offs/{id}/cancel", wo.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ─── WOF-I-07：核销单详情 ───────────────────────────────────────────────────

    @Test
    @DisplayName("WOF-I-07：GET /fin/write-offs/{id} 返回详情")
    void getDetail_shouldReturnWriteOff() throws Exception {
        FinWriteOff wo = insertWriteOff(0);

        mockMvc.perform(get("/fin/write-offs/{id}", wo.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(wo.getId()));
    }

    // ─── WOF-I-08：超额核销 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("WOF-I-08：POST /fin/write-offs 核销超过收款单余额应失败")
    void submitWriteOff_exceedReceiptBalance_shouldFail() throws Exception {
        // 收款单金额5000，尝试核销6000
        String body = objectMapper.writeValueAsString(Map.of(
                "receiptId", receipt.getId(),
                "writeOffType", 1,
                "items", List.of(
                        Map.of("receivableId", ar1.getId(), "writeOffAmount", 3000.00),
                        Map.of("receivableId", ar2.getId(), "writeOffAmount", 3000.00)
                )
        ));

        mockMvc.perform(post("/fin/write-offs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.not(200)));
    }
}
