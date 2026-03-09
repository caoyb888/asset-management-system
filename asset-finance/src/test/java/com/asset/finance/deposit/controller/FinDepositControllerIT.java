package com.asset.finance.deposit.controller;

import com.asset.finance.FinanceControllerTestBase;
import com.asset.finance.deposit.entity.FinDepositAccount;
import com.asset.finance.deposit.entity.FinDepositTransaction;
import com.asset.finance.deposit.mapper.FinDepositAccountMapper;
import com.asset.finance.deposit.mapper.FinDepositTransactionMapper;
import com.asset.finance.receivable.entity.FinReceivable;
import com.asset.finance.receivable.mapper.FinReceivableMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 保证金管理 Controller 集成测试
 *
 * <p>覆盖场景：DEP-I-01 ~ DEP-I-08
 */
@DisplayName("保证金管理 Controller 集成测试")
class FinDepositControllerIT extends FinanceControllerTestBase {

    @Autowired
    private FinDepositAccountMapper accountMapper;

    @Autowired
    private FinDepositTransactionMapper transactionMapper;

    @Autowired
    private FinReceivableMapper receivableMapper;

    private static final Long CONTRACT_ID = 10040L;

    private FinReceivable insertReceivable(BigDecimal amount) {
        FinReceivable r = new FinReceivable();
        r.setReceivableCode("AR-DIT-" + System.nanoTime());
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

    // ─── DEP-I-01：缴纳保证金 ───────────────────────────────────────────────────

    @Test
    @DisplayName("DEP-I-01：POST /fin/deposits/pay-in 缴纳保证金")
    void payIn_shouldCreateAccountAndTransaction() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "contractId", CONTRACT_ID,
                "amount", 5000.00,
                "sourceCode", "RC-001"
        ));

        mockMvc.perform(post("/fin/deposits/pay-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ─── DEP-I-02：查询账户余额 ─────────────────────────────────────────────────

    @Test
    @DisplayName("DEP-I-02：GET /fin/deposits/account 查询余额")
    void getAccount_shouldReturnBalance() throws Exception {
        // 先缴纳
        mockMvc.perform(post("/fin/deposits/pay-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "contractId", CONTRACT_ID, "amount", 5000.00))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/fin/deposits/account")
                        .param("contractId", CONTRACT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.balance").value(5000.00));
    }

    // ─── DEP-I-03：流水分页 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("DEP-I-03：GET /fin/deposits/transactions 查询流水")
    void pageTransaction_shouldReturnData() throws Exception {
        // 先缴纳
        mockMvc.perform(post("/fin/deposits/pay-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "contractId", CONTRACT_ID, "amount", 3000.00))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/fin/deposits/transactions")
                        .param("contractId", CONTRACT_ID.toString())
                        .param("pageNum", "1")
                        .param("pageSize", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    // ─── DEP-I-04：申请冲抵 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("DEP-I-04：POST /fin/deposits/offset 申请冲抵应收")
    void offset_shouldReturnTransactionId() throws Exception {
        // 先缴纳
        mockMvc.perform(post("/fin/deposits/pay-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "contractId", CONTRACT_ID, "amount", 5000.00))))
                .andExpect(status().isOk());

        FinReceivable ar = insertReceivable(new BigDecimal("2000.00"));

        String body = objectMapper.writeValueAsString(Map.of(
                "contractId", CONTRACT_ID,
                "receivableId", ar.getId(),
                "amount", 2000.00
        ));

        String response = mockMvc.perform(post("/fin/deposits/offset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber())
                .andReturn().getResponse().getContentAsString();

        Long transId = objectMapper.readTree(response).get("data").asLong();
        FinDepositTransaction trans = transactionMapper.selectById(transId);
        assertThat(trans.getStatus()).as("冲抵流水状态应为0(待审批)").isEqualTo(0);
    }

    // ─── DEP-I-05：申请退款 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("DEP-I-05：POST /fin/deposits/refund 申请退款")
    void refund_shouldReturnTransactionId() throws Exception {
        mockMvc.perform(post("/fin/deposits/pay-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "contractId", CONTRACT_ID, "amount", 5000.00))))
                .andExpect(status().isOk());

        String body = objectMapper.writeValueAsString(Map.of(
                "contractId", CONTRACT_ID,
                "amount", 1000.00,
                "reason", "IT退款测试"
        ));

        mockMvc.perform(post("/fin/deposits/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber());
    }

    // ─── DEP-I-06：申请罚没 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("DEP-I-06：POST /fin/deposits/forfeit 申请罚没")
    void forfeit_shouldReturnTransactionId() throws Exception {
        mockMvc.perform(post("/fin/deposits/pay-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "contractId", CONTRACT_ID, "amount", 5000.00))))
                .andExpect(status().isOk());

        String body = objectMapper.writeValueAsString(Map.of(
                "contractId", CONTRACT_ID,
                "amount", 500.00,
                "reason", "IT罚没测试"
        ));

        mockMvc.perform(post("/fin/deposits/forfeit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber());
    }

    // ─── DEP-I-07：审批回调 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("DEP-I-07：POST /fin/deposits/approval-callback 审批通过更新余额")
    void approvalCallback_shouldUpdateBalance() throws Exception {
        // 缴纳5000
        mockMvc.perform(post("/fin/deposits/pay-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "contractId", CONTRACT_ID, "amount", 5000.00))))
                .andExpect(status().isOk());

        FinReceivable ar = insertReceivable(new BigDecimal("2000.00"));

        // 申请冲抵2000
        String offsetResp = mockMvc.perform(post("/fin/deposits/offset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "contractId", CONTRACT_ID,
                                "receivableId", ar.getId(),
                                "amount", 2000.00))))
                .andReturn().getResponse().getContentAsString();
        Long transId = objectMapper.readTree(offsetResp).get("data").asLong();
        FinDepositTransaction trans = transactionMapper.selectById(transId);

        // 审批通过
        mockMvc.perform(post("/fin/deposits/approval-callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "approvalId", trans.getApprovalId(),
                                "approved", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ─── DEP-I-08：余额不足冲抵 ─────────────────────────────────────────────────

    @Test
    @DisplayName("DEP-I-08：POST /fin/deposits/offset 余额不足应失败")
    void offset_insufficientBalance_shouldFail() throws Exception {
        // 缴纳1000
        mockMvc.perform(post("/fin/deposits/pay-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "contractId", CONTRACT_ID, "amount", 1000.00))))
                .andExpect(status().isOk());

        FinReceivable ar = insertReceivable(new BigDecimal("5000.00"));

        // 尝试冲抵5000（余额只有1000）
        String body = objectMapper.writeValueAsString(Map.of(
                "contractId", CONTRACT_ID,
                "receivableId", ar.getId(),
                "amount", 5000.00
        ));

        mockMvc.perform(post("/fin/deposits/offset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.not(200)));
    }
}
