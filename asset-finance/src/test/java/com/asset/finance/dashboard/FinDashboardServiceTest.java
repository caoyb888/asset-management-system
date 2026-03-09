package com.asset.finance.dashboard;

import com.asset.finance.FinanceTestBase;
import com.asset.finance.dashboard.dto.DashboardSummaryVO;
import com.asset.finance.dashboard.dto.OverdueTopVO;
import com.asset.finance.dashboard.dto.ReceiptTrendVO;
import com.asset.finance.dashboard.service.FinDashboardService;
import com.asset.finance.receipt.entity.FinReceipt;
import com.asset.finance.receipt.entity.FinWriteOff;
import com.asset.finance.receipt.mapper.FinReceiptMapper;
import com.asset.finance.receipt.mapper.FinWriteOffMapper;
import com.asset.finance.receivable.entity.FinReceivable;
import com.asset.finance.receivable.mapper.FinReceivableMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * 财务看板 Service 单元测试
 *
 * <p>覆盖场景：
 * <ol>
 *   <li>DSH-01：汇总卡片-本月应收/已收/欠费</li>
 *   <li>DSH-02：汇总卡片-本月核销笔数</li>
 *   <li>DSH-03：收款趋势-12个月数据</li>
 *   <li>DSH-04：欠费TOP10-按欠费额排序</li>
 *   <li>DSH-05：无数据时返回空/零值</li>
 * </ol>
 */
@DisplayName("财务看板 Service 测试")
class FinDashboardServiceTest extends FinanceTestBase {

    @Autowired
    private FinDashboardService dashboardService;

    @Autowired
    private FinReceivableMapper receivableMapper;

    @Autowired
    private FinReceiptMapper receiptMapper;

    @Autowired
    private FinWriteOffMapper writeOffMapper;

    private static final String CURRENT_MONTH = LocalDate.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM"));

    // ─── 辅助方法 ────────────────────────────────────────────────────────────────

    private FinReceivable insertReceivable(Long contractId, Long merchantId,
                                           BigDecimal actual, BigDecimal received,
                                           LocalDate dueDate, int status) {
        FinReceivable r = new FinReceivable();
        r.setReceivableCode("AR-DSH-" + System.nanoTime());
        r.setContractId(contractId);
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
        return r;
    }

    private FinReceipt insertReceipt(BigDecimal amount, LocalDate receiptDate, int status) {
        FinReceipt r = new FinReceipt();
        r.setReceiptCode("RC-DSH-" + System.nanoTime());
        r.setContractId(10001L);
        r.setTotalAmount(amount);
        r.setPaymentMethod(1);
        r.setReceiptDate(receiptDate);
        r.setStatus(status);
        r.setWriteOffAmount(BigDecimal.ZERO);
        r.setPrepayAmount(BigDecimal.ZERO);
        receiptMapper.insert(r);
        return r;
    }

    private FinWriteOff insertWriteOff(int status) {
        FinWriteOff wo = new FinWriteOff();
        wo.setWriteOffCode("WO-DSH-" + System.nanoTime());
        wo.setReceiptId(1L);
        wo.setContractId(10001L);
        wo.setWriteOffType(1);
        wo.setTotalAmount(new BigDecimal("100.00"));
        wo.setStatus(status);
        writeOffMapper.insert(wo);
        return wo;
    }

    // ─── DSH-01：汇总卡片-本月应收/已收/欠费 ──────────────────────────────────────

    @Test
    @DisplayName("DSH-01：汇总卡片-本月应收3000/已收1800/欠费1200")
    void getSummary_shouldReturnCorrectMonthlyTotals() {
        LocalDate today = LocalDate.now();
        // 插入3条当月应收(actual=1000, received=600)
        for (int i = 0; i < 3; i++) {
            insertReceivable(10001L + i, 20001L + i,
                    new BigDecimal("1000.00"), new BigDecimal("600.00"),
                    today.minusDays(5), 1); // 已逾期、部分收款
        }

        DashboardSummaryVO summary = dashboardService.getSummary();

        assertThat(summary.getMonthReceivable())
                .as("本月应收合计应为3000")
                .isEqualByComparingTo("3000.00");
        assertThat(summary.getMonthReceived())
                .as("本月已收合计应为1800")
                .isEqualByComparingTo("1800.00");
        assertThat(summary.getCurrentOverdue())
                .as("当前欠费合计应为1200")
                .isEqualByComparingTo("1200.00");
    }

    // ─── DSH-02：汇总卡片-本月核销笔数 ────────────────────────────────────────────

    @Test
    @DisplayName("DSH-02：汇总卡片-本月核销笔数为2")
    void getSummary_shouldReturnCorrectWriteOffCount() {
        // 插入2笔已通过核销（status=1）
        insertWriteOff(1);
        insertWriteOff(1);
        // 插入1笔待审核核销（status=0），不应计入
        insertWriteOff(0);

        DashboardSummaryVO summary = dashboardService.getSummary();

        assertThat(summary.getMonthWriteOffCount())
                .as("本月核销笔数应为2（仅统计已通过）")
                .isEqualTo(2L);
    }

    // ─── DSH-03：收款趋势-12个月数据 ──────────────────────────────────────────────

    @Test
    @DisplayName("DSH-03：收款趋势返回12个月数据")
    void getReceiptTrend_shouldReturn12Months() {
        LocalDate today = LocalDate.now();
        // 插入近12月每月各1条收款
        for (int i = 0; i < 12; i++) {
            LocalDate date = today.minusMonths(i).withDayOfMonth(1);
            insertReceipt(new BigDecimal("1000.00"), date, 0);
        }

        List<ReceiptTrendVO> trend = dashboardService.getReceiptTrend();

        assertThat(trend)
                .as("应返回12个月的数据")
                .hasSize(12);

        // 每条应含非空 month 和 amount
        assertThat(trend)
                .allSatisfy(vo -> {
                    assertThat(vo.getMonth()).as("month不应为空").isNotBlank();
                    assertThat(vo.getAmount()).as("amount不应为空").isNotNull();
                });
    }

    // ─── DSH-04：欠费TOP10-按欠费额排序 ──────────────────────────────────────────

    @Test
    @DisplayName("DSH-04：欠费TOP10返回最多10条并按欠费额降序")
    void getOverdueTop_shouldReturnTop10Sorted() {
        LocalDate today = LocalDate.now();
        // 插入15个商家的逾期应收，金额依次递增
        for (int i = 1; i <= 15; i++) {
            insertReceivable(10000L + i, 20000L + i,
                    new BigDecimal(i * 100 + ".00"), BigDecimal.ZERO,
                    today.minusDays(10), 0); // 逾期、待收
        }

        List<OverdueTopVO> top = dashboardService.getOverdueTop();

        assertThat(top)
                .as("最多返回10条")
                .hasSizeLessThanOrEqualTo(10);

        // 验证降序排列
        for (int i = 0; i < top.size() - 1; i++) {
            assertThat(top.get(i).getOverdueAmount())
                    .as("第%d条欠费应≥第%d条", i, i + 1)
                    .isGreaterThanOrEqualTo(top.get(i + 1).getOverdueAmount());
        }
    }

    // ─── DSH-05：无数据时返回空/零值 ──────────────────────────────────────────────

    @Test
    @DisplayName("DSH-05：无数据时 getSummary/getReceiptTrend/getOverdueTop 返回安全零值")
    void noData_shouldReturnSafeDefaults() {
        // 不插入任何数据

        DashboardSummaryVO summary = dashboardService.getSummary();
        assertThat(summary.getMonthReceivable())
                .as("无数据时本月应收应为0或null")
                .satisfiesAnyOf(
                        v -> assertThat(v).isNull(),
                        v -> assertThat(v).isEqualByComparingTo("0.00")
                );
        assertThat(summary.getMonthWriteOffCount())
                .as("无数据时核销笔数应为0")
                .satisfiesAnyOf(
                        v -> assertThat(v).isNull(),
                        v -> assertThat(v).isEqualTo(0L)
                );

        List<ReceiptTrendVO> trend = dashboardService.getReceiptTrend();
        assertThat(trend)
                .as("无数据时趋势应返回12个月（含零值填充）或空列表")
                .isNotNull();

        List<OverdueTopVO> top = dashboardService.getOverdueTop();
        assertThat(top)
                .as("无数据时TOP10应为空列表")
                .isNotNull()
                .isEmpty();
    }
}
