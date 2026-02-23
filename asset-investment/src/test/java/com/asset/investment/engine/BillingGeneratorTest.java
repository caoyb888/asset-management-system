package com.asset.investment.engine;

import com.asset.investment.common.enums.BillingMode;
import com.asset.investment.common.enums.PaymentCycle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 账期生成器单元测试（任务3.3）
 * 覆盖：月付/季付/年付 × 预付/后付 + 闰年2月边界 + 尾账期截断
 */
@DisplayName("账期生成器测试")
class BillingGeneratorTest {

    private BillingGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new BillingGenerator();
    }

    // ═══════════════════════════════════════════════════════════
    // §1  月付（paymentCycle=1）
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("月付-预付-12个月：账期数=12，首账期billingType=1，其余=2")
    void monthly_prepay_12months() {
        List<BillingPeriod> periods = generator.generate(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                PaymentCycle.MONTHLY, BillingMode.PREPAY);
        assertEquals(12, periods.size());
        assertEquals(1, periods.get(0).getBillingType());
        assertEquals(LocalDate.of(2026, 1, 1), periods.get(0).getBillingStart());
        assertEquals(LocalDate.of(2026, 1, 31), periods.get(0).getBillingEnd());
        assertEquals(LocalDate.of(2026, 1, 1), periods.get(0).getDueDate());  // 预付=账期开始
        assertEquals(2, periods.get(1).getBillingType());
        assertEquals(LocalDate.of(2026, 12, 1), periods.get(11).getBillingStart());
        assertEquals(LocalDate.of(2026, 12, 31), periods.get(11).getBillingEnd());
    }

    @Test
    @DisplayName("月付-后付-12个月：dueDate等于账期结束日")
    void monthly_postpay_dueDate() {
        List<BillingPeriod> periods = generator.generate(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                PaymentCycle.MONTHLY, BillingMode.POSTPAY);
        assertEquals(12, periods.size());
        // 每个账期的 dueDate = billingEnd
        periods.forEach(p -> assertEquals(p.getBillingEnd(), p.getDueDate()));
        assertEquals(LocalDate.of(2026, 1, 31), periods.get(0).getDueDate());
    }

    @Test
    @DisplayName("月付-预付：连续账期无缝连接（上期end+1 = 下期start）")
    void monthly_continuousPeriods() {
        List<BillingPeriod> periods = generator.generate(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30),
                PaymentCycle.MONTHLY, BillingMode.PREPAY);
        for (int i = 0; i < periods.size() - 1; i++) {
            LocalDate expectedNextStart = periods.get(i).getBillingEnd().plusDays(1);
            assertEquals(expectedNextStart, periods.get(i + 1).getBillingStart(),
                    "第" + (i + 1) + "期结束+1应等于第" + (i + 2) + "期开始");
        }
    }

    // ═══════════════════════════════════════════════════════════
    // §2  季付（paymentCycle=3）
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("季付-预付-1年：账期数=4，每期3个月")
    void quarterly_prepay_1year() {
        List<BillingPeriod> periods = generator.generate(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                PaymentCycle.QUARTERLY, BillingMode.PREPAY);
        assertEquals(4, periods.size());
        assertEquals(LocalDate.of(2026, 1, 1), periods.get(0).getBillingStart());
        assertEquals(LocalDate.of(2026, 3, 31), periods.get(0).getBillingEnd());
        assertEquals(LocalDate.of(2026, 4, 1), periods.get(1).getBillingStart());
        assertEquals(LocalDate.of(2026, 6, 30), periods.get(1).getBillingEnd());
        assertEquals(LocalDate.of(2026, 10, 1), periods.get(3).getBillingStart());
        assertEquals(LocalDate.of(2026, 12, 31), periods.get(3).getBillingEnd());
    }

    @Test
    @DisplayName("季付-后付-1年：dueDate=每期最后一天")
    void quarterly_postpay_dueDates() {
        List<BillingPeriod> periods = generator.generate(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                PaymentCycle.QUARTERLY, BillingMode.POSTPAY);
        assertEquals(LocalDate.of(2026, 3, 31), periods.get(0).getDueDate());
        assertEquals(LocalDate.of(2026, 6, 30), periods.get(1).getDueDate());
        assertEquals(LocalDate.of(2026, 9, 30), periods.get(2).getDueDate());
        assertEquals(LocalDate.of(2026, 12, 31), periods.get(3).getDueDate());
    }

    @Test
    @DisplayName("季付-3年合同：账期数=12，首账期billingType=1其余=2")
    void quarterly_3years() {
        List<BillingPeriod> periods = generator.generate(
                LocalDate.of(2026, 1, 1), LocalDate.of(2028, 12, 31),
                PaymentCycle.QUARTERLY, BillingMode.PREPAY);
        assertEquals(12, periods.size());
        assertEquals(1, periods.get(0).getBillingType());
        periods.subList(1, 12).forEach(p -> assertEquals(2, p.getBillingType()));
    }

    // ═══════════════════════════════════════════════════════════
    // §3  年付（paymentCycle=6）
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("年付-预付-3年：账期数=3")
    void annual_prepay_3years() {
        List<BillingPeriod> periods = generator.generate(
                LocalDate.of(2026, 1, 1), LocalDate.of(2028, 12, 31),
                PaymentCycle.ANNUAL, BillingMode.PREPAY);
        assertEquals(3, periods.size());
        assertEquals(LocalDate.of(2026, 1, 1), periods.get(0).getBillingStart());
        assertEquals(LocalDate.of(2026, 12, 31), periods.get(0).getBillingEnd());
        assertEquals(LocalDate.of(2027, 1, 1), periods.get(1).getBillingStart());
        assertEquals(LocalDate.of(2027, 12, 31), periods.get(1).getBillingEnd());
    }

    @Test
    @DisplayName("年付-后付-dueDate=每年12月31日")
    void annual_postpay_dueDates() {
        List<BillingPeriod> periods = generator.generate(
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 12, 31),
                PaymentCycle.ANNUAL, BillingMode.POSTPAY);
        assertEquals(2, periods.size());
        assertEquals(LocalDate.of(2026, 12, 31), periods.get(0).getDueDate());
        assertEquals(LocalDate.of(2027, 12, 31), periods.get(1).getDueDate());
    }

    @Test
    @DisplayName("年付-5年合同：账期数=5，年份连续")
    void annual_5years() {
        List<BillingPeriod> periods = generator.generate(
                LocalDate.of(2026, 1, 1), LocalDate.of(2030, 12, 31),
                PaymentCycle.ANNUAL, BillingMode.PREPAY);
        assertEquals(5, periods.size());
        for (int i = 0; i < 5; i++) {
            assertEquals(LocalDate.of(2026 + i, 1, 1), periods.get(i).getBillingStart());
            assertEquals(LocalDate.of(2026 + i, 12, 31), periods.get(i).getBillingEnd());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // §4  当期账期模式（billingMode=2，dueDate=billingStart，等同预付）
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("当期账期模式：dueDate=billingStart（与预付相同）")
    void currentMode_samePrepay() {
        List<BillingPeriod> prepay = generator.generate(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31),
                PaymentCycle.QUARTERLY, BillingMode.PREPAY);
        List<BillingPeriod> current = generator.generate(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31),
                PaymentCycle.QUARTERLY, BillingMode.CURRENT);
        assertEquals(prepay.get(0).getDueDate(), current.get(0).getDueDate());
    }

    // ═══════════════════════════════════════════════════════════
    // §5  闰年2月边界
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("闰年2月（2024）-月付：2024-02-01到2024-02-29为一个账期")
    void leapYear_feb_monthlyPeriod() {
        List<BillingPeriod> periods = generator.generate(
                LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29),
                PaymentCycle.MONTHLY, BillingMode.PREPAY);
        assertEquals(1, periods.size());
        assertEquals(LocalDate.of(2024, 2, 1), periods.get(0).getBillingStart());
        assertEquals(LocalDate.of(2024, 2, 29), periods.get(0).getBillingEnd());
    }

    @Test
    @DisplayName("闰年跨2月-季付：2024-01-01到2024-03-31（含闰年2月）正确生成1个账期")
    void leapYear_feb_quarterlyCross() {
        List<BillingPeriod> periods = generator.generate(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 31),
                PaymentCycle.QUARTERLY, BillingMode.PREPAY);
        assertEquals(1, periods.size());
        assertEquals(LocalDate.of(2024, 1, 1), periods.get(0).getBillingStart());
        assertEquals(LocalDate.of(2024, 3, 31), periods.get(0).getBillingEnd());
    }

    @Test
    @DisplayName("闰年-从1月31日开始月付：算法确保 nextStart-1 正确截断（非越界）")
    void leapYear_jan31Start_monthly() {
        // 算法：current=Jan31, nextStart=Jan31+1M=Feb29（Java溢出处理）, periodEnd=Feb29-1=Feb28
        // 期1: Jan31~Feb28（29天）; 期2: Feb29~Mar28; 期3: Mar29~Mar31(截断)
        List<BillingPeriod> periods = generator.generate(
                LocalDate.of(2024, 1, 31), LocalDate.of(2024, 3, 31),
                PaymentCycle.MONTHLY, BillingMode.PREPAY);
        assertEquals(3, periods.size());
        assertEquals(LocalDate.of(2024, 1, 31), periods.get(0).getBillingStart());
        assertEquals(LocalDate.of(2024, 2, 28), periods.get(0).getBillingEnd()); // nextStart(Feb29)-1=Feb28
        assertEquals(LocalDate.of(2024, 2, 29), periods.get(1).getBillingStart()); // 下期从Feb29起
    }

    // ═══════════════════════════════════════════════════════════
    // §6  尾账期截断
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("季付-合同不整除：最后账期被截断至合同结束日")
    void quarterly_tailTruncation() {
        // 合同 2026-01-01 ~ 2026-10-15（非整季结束）
        // 期1: 01-01~03-31, 期2: 04-01~06-30, 期3: 07-01~09-30, 期4: 10-01~10-15(截断)
        List<BillingPeriod> periods = generator.generate(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 10, 15),
                PaymentCycle.QUARTERLY, BillingMode.PREPAY);
        assertEquals(4, periods.size());
        assertEquals(LocalDate.of(2026, 10, 1), periods.get(3).getBillingStart());
        assertEquals(LocalDate.of(2026, 10, 15), periods.get(3).getBillingEnd()); // 截断
    }

    @Test
    @DisplayName("单个账期合同（合同期 < 1个周期）：生成1条账期")
    void singlePeriod_shortContract() {
        List<BillingPeriod> periods = generator.generate(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 15),
                PaymentCycle.MONTHLY, BillingMode.PREPAY);
        assertEquals(1, periods.size());
        assertEquals(1, periods.get(0).getBillingType()); // 首账期
        assertEquals(LocalDate.of(2026, 3, 1), periods.get(0).getBillingStart());
        assertEquals(LocalDate.of(2026, 3, 15), periods.get(0).getBillingEnd());
    }

    // ═══════════════════════════════════════════════════════════
    // §7  两月付 & 半年付
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("两月付-1年：账期数=6")
    void bimonthly_1year() {
        List<BillingPeriod> periods = generator.generate(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                PaymentCycle.BIMONTHLY, BillingMode.PREPAY);
        assertEquals(6, periods.size());
    }

    @Test
    @DisplayName("半年付-2年：账期数=4")
    void semiannual_2years() {
        List<BillingPeriod> periods = generator.generate(
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 12, 31),
                PaymentCycle.SEMIANNUAL, BillingMode.PREPAY);
        assertEquals(4, periods.size());
        assertEquals(LocalDate.of(2026, 1, 1), periods.get(0).getBillingStart());
        assertEquals(LocalDate.of(2026, 6, 30), periods.get(0).getBillingEnd());
        assertEquals(LocalDate.of(2026, 7, 1), periods.get(1).getBillingStart());
        assertEquals(LocalDate.of(2026, 12, 31), periods.get(1).getBillingEnd());
    }
}
