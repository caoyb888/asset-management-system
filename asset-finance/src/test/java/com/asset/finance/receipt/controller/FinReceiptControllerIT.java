package com.asset.finance.receipt.controller;

import com.asset.finance.FinanceControllerTestBase;
import com.asset.finance.receipt.entity.FinReceipt;
import com.asset.finance.receipt.entity.FinReceiptDetail;
import com.asset.finance.receipt.mapper.FinReceiptDetailMapper;
import com.asset.finance.receipt.mapper.FinReceiptMapper;
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
 * 收款管理 Controller 集成测试
 *
 * <p>覆盖场景：RCT-I-01 ~ RCT-I-08
 */
@DisplayName("收款管理 Controller 集成测试")
class FinReceiptControllerIT extends FinanceControllerTestBase {

    @Autowired
    private FinReceiptMapper receiptMapper;

    @Autowired
    private FinReceiptDetailMapper detailMapper;

    private static final Long CONTRACT_ID = 10020L;

    private FinReceipt insertReceipt(BigDecimal amount, int status, int isUnnamed) {
        FinReceipt r = new FinReceipt();
        r.setReceiptCode("RC-IT-" + System.nanoTime());
        r.setContractId(CONTRACT_ID);
        r.setTotalAmount(amount);
        r.setPaymentMethod(1);
        r.setReceiptDate(LocalDate.now());
        r.setStatus(status);
        r.setIsUnnamed(isUnnamed);
        r.setWriteOffAmount(BigDecimal.ZERO);
        r.setPrepayAmount(BigDecimal.ZERO);
        receiptMapper.insert(r);
        return r;
    }

    private void insertDetail(Long receiptId, BigDecimal amount) {
        FinReceiptDetail d = new FinReceiptDetail();
        d.setReceiptId(receiptId);
        d.setFeeItemId(1L);
        d.setFeeName("租金");
        d.setAmount(amount);
        detailMapper.insert(d);
    }

    // ─── RCT-I-01：分页列表 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("RCT-I-01：GET /fin/receipts 返回分页数据")
    void page_shouldReturnPaginatedList() throws Exception {
        insertReceipt(new BigDecimal("1000.00"), 0, 0);
        insertReceipt(new BigDecimal("2000.00"), 0, 0);
        insertReceipt(new BigDecimal("3000.00"), 0, 0);

        mockMvc.perform(get("/fin/receipts")
                        .param("pageNum", "1")
                        .param("pageSize", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)));
    }

    // ─── RCT-I-02：新增收款单 ───────────────────────────────────────────────────

    @Test
    @DisplayName("RCT-I-02：POST /fin/receipts 创建收款单")
    void create_shouldReturnReceiptId() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "contractId", CONTRACT_ID,
                "totalAmount", 1000.00,
                "receiptDate", LocalDate.now().toString(),
                "paymentMethod", 1,
                "details", List.of(
                        Map.of("feeItemId", 1, "feeName", "租金", "amount", 600.00),
                        Map.of("feeItemId", 2, "feeName", "物业费", "amount", 400.00)
                )
        ));

        String response = mockMvc.perform(post("/fin/receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber())
                .andReturn().getResponse().getContentAsString();

        Long receiptId = objectMapper.readTree(response).get("data").asLong();
        FinReceipt receipt = receiptMapper.selectById(receiptId);
        assertThat(receipt.getStatus()).as("新建收款单状态应为0").isEqualTo(0);
    }

    // ─── RCT-I-03：拆分不平衡 ───────────────────────────────────────────────────

    @Test
    @DisplayName("RCT-I-03：POST /fin/receipts 拆分金额不等于总额应失败")
    void create_mismatchedDetails_shouldFail() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "contractId", CONTRACT_ID,
                "totalAmount", 1000.00,
                "receiptDate", LocalDate.now().toString(),
                "paymentMethod", 1,
                "details", List.of(
                        Map.of("feeItemId", 1, "feeName", "租金", "amount", 600.00),
                        Map.of("feeItemId", 2, "feeName", "物业费", "amount", 300.00)
                )
        ));

        mockMvc.perform(post("/fin/receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.not(200)));
    }

    // ─── RCT-I-04：详情查询 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("RCT-I-04：GET /fin/receipts/{id} 返回含明细")
    void getById_shouldReturnDetail() throws Exception {
        FinReceipt receipt = insertReceipt(new BigDecimal("1000.00"), 0, 0);
        insertDetail(receipt.getId(), new BigDecimal("1000.00"));

        mockMvc.perform(get("/fin/receipts/{id}", receipt.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(receipt.getId()))
                .andExpect(jsonPath("$.data.details").isArray());
    }

    // ─── RCT-I-05：编辑收款单 ───────────────────────────────────────────────────

    @Test
    @DisplayName("RCT-I-05：PUT /fin/receipts/{id} 更新待核销收款单")
    void update_shouldSucceed() throws Exception {
        FinReceipt receipt = insertReceipt(new BigDecimal("1000.00"), 0, 0);

        String body = objectMapper.writeValueAsString(Map.of(
                "contractId", CONTRACT_ID,
                "totalAmount", 1500.00,
                "receiptDate", LocalDate.now().toString(),
                "paymentMethod", 2
        ));

        mockMvc.perform(put("/fin/receipts/{id}", receipt.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ─── RCT-I-06：作废收款单 ───────────────────────────────────────────────────

    @Test
    @DisplayName("RCT-I-06：PUT /fin/receipts/{id}/cancel 作废待核销收款单")
    void cancel_shouldSetStatus3() throws Exception {
        FinReceipt receipt = insertReceipt(new BigDecimal("1000.00"), 0, 0);

        mockMvc.perform(put("/fin/receipts/{id}/cancel", receipt.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"IT测试作废\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        FinReceipt cancelled = receiptMapper.selectById(receipt.getId());
        assertThat(cancelled.getStatus()).as("作废后状态应为3").isEqualTo(3);
    }

    // ─── RCT-I-07：已核销不可作废 ──────────────────────────────────────────────

    @Test
    @DisplayName("RCT-I-07：PUT /fin/receipts/{id}/cancel 已核销收款单不可作废")
    void cancel_alreadyWrittenOff_shouldFail() throws Exception {
        FinReceipt receipt = insertReceipt(new BigDecimal("1000.00"), 2, 0);

        mockMvc.perform(put("/fin/receipts/{id}/cancel", receipt.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"尝试作废\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.not(200)));
    }

    // ─── RCT-I-08：未名款项归名 ─────────────────────────────────────────────────

    @Test
    @DisplayName("RCT-I-08：PUT /fin/receipts/{id}/bind 未名款项归名")
    void bind_shouldClearUnnamedFlag() throws Exception {
        FinReceipt receipt = insertReceipt(new BigDecimal("1000.00"), 0, 1);

        mockMvc.perform(put("/fin/receipts/{id}/bind", receipt.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contractId\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        FinReceipt bound = receiptMapper.selectById(receipt.getId());
        assertThat(bound.getIsUnnamed()).as("归名后 isUnnamed 应为0").isEqualTo(0);
    }
}
