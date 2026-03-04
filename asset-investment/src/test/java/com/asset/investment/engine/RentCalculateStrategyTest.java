package com.asset.investment.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 计租引擎单元测试（CALC-U-01 ~ CALC-U-08）
 * 直接 new 策略对象，无需 Spring 上下文，验证数学精确性
 */
@DisplayName("计租引擎单元测试")
class RentCalculateStrategyTest {

    private final FixedRentStrategy fixedRent = new FixedRentStrategy();
    private final FixedCommissionStrategy fixedCommission = new FixedCommissionStrategy();
    private final HigherOfStrategy higherOf = new HigherOfStrategy();
    private final OneTimeStrategy oneTime = new OneTimeStrategy();

    // ═══════════════════════════════════════════════════════════
    // CALC-U-01 ~ CALC-U-04  FixedRentStrategy
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("CALC-U-01 FixedRentStrategy 整月计算：12月 × 100元/㎡ × 200㎡ = 240,000.00")
    void calcU01_fixedRent_fullYearMonths() {
        RentCalculateContext ctx = RentCalculateContext.builder()
                .unitPrice(new BigDecimal("100"))
                .area(new BigDecimal("200"))
                .stageStart(LocalDate.of(2026, 1, 1))
                .stageEnd(LocalDate.of(2026, 12, 31))
                .build();

        BigDecimal result = fixedRent.calculate(ctx);

        assertThat(result).isEqualByComparingTo("240000.00");
    }

    @Test
    @DisplayName("CALC-U-02 FixedRentStrategy 含零头月：2026-01-01~02-14 = 1.5月 × 100 × 200 = 30,000.00")
    void calcU02_fixedRent_partialMonth() {
        // 2月有28天，14/28 = 0.5月
        BigDecimal months = FixedRentStrategy.calcMonths(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 14));
        assertThat(months).isEqualByComparingTo("1.5");

        RentCalculateContext ctx = RentCalculateContext.builder()
                .unitPrice(new BigDecimal("100"))
                .area(new BigDecimal("200"))
                .stageStart(LocalDate.of(2026, 1, 1))
                .stageEnd(LocalDate.of(2026, 2, 14))
                .build();
        assertThat(fixedRent.calculate(ctx)).isEqualByComparingTo("30000.00");
    }

    @Test
    @DisplayName("CALC-U-03 FixedRentStrategy 单日计算：1/31月 × 310 × 1 = 10.00（HALF_UP）")
    void calcU03_fixedRent_singleDay() {
        RentCalculateContext ctx = RentCalculateContext.builder()
                .unitPrice(new BigDecimal("310"))
                .area(new BigDecimal("1"))
                .stageStart(LocalDate.of(2026, 1, 15))
                .stageEnd(LocalDate.of(2026, 1, 15))
                .build();

        BigDecimal result = fixedRent.calculate(ctx);

        // 1/31 × 310 × 1 = 310/31 = 10.00
        assertThat(result).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("CALC-U-04 FixedRentStrategy 跨两年：24月 × 100 × 200 = 480,000.00")
    void calcU04_fixedRent_twoYears() {
        RentCalculateContext ctx = RentCalculateContext.builder()
                .unitPrice(new BigDecimal("100"))
                .area(new BigDecimal("200"))
                .stageStart(LocalDate.of(2026, 1, 1))
                .stageEnd(LocalDate.of(2027, 12, 31))
                .build();

        assertThat(fixedRent.calculate(ctx)).isEqualByComparingTo("480000.00");
    }

    // ═══════════════════════════════════════════════════════════
    // CALC-U-05 ~ CALC-U-06  FixedCommissionStrategy
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("CALC-U-05 FixedCommissionStrategy 提成>保底：200000×10%=20000 > 10000 → 20,000.00")
    void calcU05_fixedCommission_commissionAboveMin() {
        BigDecimal result = FixedCommissionStrategy.calcCommission(
                new BigDecimal("200000"),
                new BigDecimal("10"),
                new BigDecimal("10000"));

        assertThat(result).isEqualByComparingTo("20000.00");
    }

    @Test
    @DisplayName("CALC-U-06 FixedCommissionStrategy 提成<保底取保底：50000×10%=5000 < 8000 → 8,000.00")
    void calcU06_fixedCommission_commissionBelowMin_returnsMin() {
        BigDecimal result = FixedCommissionStrategy.calcCommission(
                new BigDecimal("50000"),
                new BigDecimal("10"),
                new BigDecimal("8000"));

        assertThat(result).isEqualByComparingTo("8000.00");
    }

    // ═══════════════════════════════════════════════════════════
    // CALC-U-07  OneTimeStrategy
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("CALC-U-07 OneTimeStrategy formulaParams.amount=50000 → 50,000.00")
    void calcU07_oneTime_fromFormulaParams() throws Exception {
        ObjectMapper om = new ObjectMapper();
        ObjectNode params = om.createObjectNode();
        params.put("amount", "50000");

        RentCalculateContext ctx = RentCalculateContext.builder()
                .formulaParams(params)
                .build();

        assertThat(oneTime.calculate(ctx)).isEqualByComparingTo("50000.00");
    }

    // ═══════════════════════════════════════════════════════════
    // CALC-U-08  HigherOfStrategy
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("CALC-U-08 HigherOfStrategy 固定部分>提成部分：max(240000, 2000) = 240,000.00")
    void calcU08_higherOf_fixedWins() {
        // 固定：100×200×12月 = 240000
        // 提成：20000×10%=2000，保底1000 → 2000
        RentCalculateContext ctx = RentCalculateContext.builder()
                .unitPrice(new BigDecimal("100"))
                .area(new BigDecimal("200"))
                .stageStart(LocalDate.of(2026, 1, 1))
                .stageEnd(LocalDate.of(2026, 12, 31))
                .revenue(new BigDecimal("20000"))
                .commissionRate(new BigDecimal("10"))
                .minCommissionAmount(new BigDecimal("1000"))
                .build();

        assertThat(higherOf.calculate(ctx)).isEqualByComparingTo("240000.00");
    }
}
