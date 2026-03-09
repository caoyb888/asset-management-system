package com.asset.finance.dashboard.controller;

import com.asset.finance.FinanceControllerTestBase;
import com.asset.finance.receipt.entity.FinReceipt;
import com.asset.finance.receipt.mapper.FinReceiptMapper;
import com.asset.finance.receivable.entity.FinReceivable;
import com.asset.finance.receivable.mapper.FinReceivableMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 财务看板 Controller 集成测试
 *
 * <p>覆盖场景：DSH-I-01 ~ DSH-I-03
 */
@DisplayName("财务看板 Controller 集成测试")
class FinDashboardControllerIT extends FinanceControllerTestBase {

    @Autowired
    private FinReceivableMapper receivableMapper;

    @Autowired
    private FinReceiptMapper receiptMapper;

    private static final String CURRENT_MONTH = LocalDate.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM"));

    private void insertReceivable(Long merchantId, BigDecimal actual, BigDecimal received,
                                  LocalDate dueDate, int status) {
        FinReceivable r = new FinReceivable();
        r.setReceivableCode("AR-DCI-" + System.nanoTime());
        r.setContractId(10060L);
        r.setMerchantId(merchantId);
        r.setOriginalAmount(actual);
        r.setAdjustAmount(BigDecimal.ZERO);
        r.setDeductionAmount(BigDecimal.ZERO);
        r.setActualAmount(actual);
        r.setReceivedAmount(received);
        r.setOutstandingAmount(actual.subtract(received).max(BigDecimal.ZERO));
        r.setDueDate(dueDate);
        r.setStatus(status);
        r.setAccrualMonth(CURRENT_MONTH);
        receivableMapper.insert(r);
    }

    private void insertReceipt(BigDecimal amount, LocalDate date) {
        FinReceipt r = new FinReceipt();
        r.setReceiptCode("RC-DCI-" + System.nanoTime());
        r.setContractId(10060L);
        r.setTotalAmount(amount);
        r.setPaymentMethod(1);
        r.setReceiptDate(date);
        r.setStatus(0);
        r.setWriteOffAmount(BigDecimal.ZERO);
        r.setPrepayAmount(BigDecimal.ZERO);
        receiptMapper.insert(r);
    }

    // ─── DSH-I-01：看板汇总 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("DSH-I-01：GET /fin/dashboard/summary 返回汇总数据")
    void summary_shouldReturnCards() throws Exception {
        LocalDate today = LocalDate.now();
        insertReceivable(20060L, new BigDecimal("1000.00"), new BigDecimal("600.00"),
                today.minusDays(5), 1);

        mockMvc.perform(get("/fin/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.monthReceivable").isNumber())
                .andExpect(jsonPath("$.data.monthReceived").isNumber())
                .andExpect(jsonPath("$.data.currentOverdue").isNumber());
    }

    // ─── DSH-I-02：收款趋势 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("DSH-I-02：GET /fin/dashboard/receipt-trend 返回12个月趋势")
    void receiptTrend_shouldReturn12Months() throws Exception {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 3; i++) {
            insertReceipt(new BigDecimal("1000.00"), today.minusMonths(i).withDayOfMonth(1));
        }

        mockMvc.perform(get("/fin/dashboard/receipt-trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(12));
    }

    // ─── DSH-I-03：欠费TOP10 ────────────────────────────────────────────────────

    @Test
    @DisplayName("DSH-I-03：GET /fin/dashboard/overdue-top 返回欠费排名")
    void overdueTop_shouldReturnSorted() throws Exception {
        LocalDate today = LocalDate.now();
        for (int i = 1; i <= 3; i++) {
            insertReceivable(20060L + i,
                    new BigDecimal(i * 500 + ".00"), BigDecimal.ZERO,
                    today.minusDays(10), 0);
        }

        mockMvc.perform(get("/fin/dashboard/overdue-top"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }
}
