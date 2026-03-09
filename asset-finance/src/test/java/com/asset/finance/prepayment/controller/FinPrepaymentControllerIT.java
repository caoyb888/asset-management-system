package com.asset.finance.prepayment.controller;

import com.asset.finance.FinanceControllerTestBase;
import com.asset.finance.receivable.entity.FinReceivable;
import com.asset.finance.receivable.mapper.FinReceivableMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 预收款管理 Controller 集成测试
 *
 * <p>覆盖场景：PRE-I-01 ~ PRE-I-06
 */
@DisplayName("预收款管理 Controller 集成测试")
class FinPrepaymentControllerIT extends FinanceControllerTestBase {

    @Autowired
    private FinReceivableMapper receivableMapper;

    private static final Long CONTRACT_ID = 10050L;

    private FinReceivable insertReceivable(BigDecimal amount) {
        FinReceivable r = new FinReceivable();
        r.setReceivableCode("AR-PIT-" + System.nanoTime());
        r.setContractId(CONTRACT_ID);
        r.setOriginalAmount(amount);
        r.setAdjustAmount(BigDecimal.ZERO);
        r.setDeductionAmount(BigDecimal.ZERO);
        r.setActualAmount(amount);
        r.setReceivedAmount(BigDecimal.ZERO);
        r.setOutstandingAmount(amount);
        r.setDueDate(LocalDate.now().minusDays(5));
        r.setStatus(0);
        r.setAccrualMonth(LocalDate.now().toString().substring(0, 7));
        receivableMapper.insert(r);
        return r;
    }

    private void deposit(double amount) throws Exception {
        mockMvc.perform(post("/fin/prepayments/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "contractId", CONTRACT_ID, "amount", amount))))
                .andExpect(status().isOk());
    }

    // ─── PRE-I-01：录入预收款 ───────────────────────────────────────────────────

    @Test
    @DisplayName("PRE-I-01：POST /fin/prepayments/deposit 录入预收款")
    void deposit_shouldSucceed() throws Exception {
        mockMvc.perform(post("/fin/prepayments/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "contractId", CONTRACT_ID, "amount", 500.00))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ─── PRE-I-02：查询账户 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("PRE-I-02：GET /fin/prepayments/account 查询余额")
    void getAccount_shouldReturnBalance() throws Exception {
        deposit(500.00);

        mockMvc.perform(get("/fin/prepayments/account")
                        .param("contractId", CONTRACT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.balance").value(500.00));
    }

    // ─── PRE-I-03：查询流水 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("PRE-I-03：GET /fin/prepayments/transactions 查询流水")
    void pageTransaction_shouldReturnData() throws Exception {
        deposit(500.00);

        mockMvc.perform(get("/fin/prepayments/transactions")
                        .param("contractId", CONTRACT_ID.toString())
                        .param("pageNum", "1")
                        .param("pageSize", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    // ─── PRE-I-04：抵冲应收 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("PRE-I-04：POST /fin/prepayments/offset 抵冲应收")
    void offset_shouldDeductBalance() throws Exception {
        deposit(500.00);
        FinReceivable ar = insertReceivable(new BigDecimal("300.00"));

        mockMvc.perform(post("/fin/prepayments/offset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "contractId", CONTRACT_ID,
                                "receivableId", ar.getId(),
                                "amount", 300.00))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证余额
        mockMvc.perform(get("/fin/prepayments/account")
                        .param("contractId", CONTRACT_ID.toString()))
                .andExpect(jsonPath("$.data.balance").value(200.00));
    }

    // ─── PRE-I-05：退款 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("PRE-I-05：POST /fin/prepayments/refund 退款")
    void refund_shouldDeductBalance() throws Exception {
        deposit(500.00);

        mockMvc.perform(post("/fin/prepayments/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "contractId", CONTRACT_ID,
                                "amount", 200.00))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/fin/prepayments/account")
                        .param("contractId", CONTRACT_ID.toString()))
                .andExpect(jsonPath("$.data.balance").value(300.00));
    }

    // ─── PRE-I-06：余额不足抵冲 ─────────────────────────────────────────────────

    @Test
    @DisplayName("PRE-I-06：POST /fin/prepayments/offset 余额不足应失败")
    void offset_insufficientBalance_shouldFail() throws Exception {
        deposit(100.00);
        FinReceivable ar = insertReceivable(new BigDecimal("200.00"));

        mockMvc.perform(post("/fin/prepayments/offset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "contractId", CONTRACT_ID,
                                "receivableId", ar.getId(),
                                "amount", 200.00))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.not(200)));
    }
}
