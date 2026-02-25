package com.asset.investment.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 租金计算引擎单元测试（任务3.3）
 * 覆盖5种收费方式的核心场景，验证金额精度（精确到分，无浮点误差）
 */
@DisplayName("租金计算引擎测试")
class RentCalculateEngineTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private FixedRentStrategy fixedRentStrategy;
    private FixedCommissionStrategy fixedCommissionStrategy;
    private StepCommissionStrategy stepCommissionStrategy;
    private HigherOfStrategy higherOfStrategy;
    private OneTimeStrategy oneTimeStrategy;

    @BeforeEach
    void setUp() {
        fixedRentStrategy = new FixedRentStrategy();
        fixedCommissionStrategy = new FixedCommissionStrategy();
        stepCommissionStrategy = new StepCommissionStrategy();
        higherOfStrategy = new HigherOfStrategy();
        oneTimeStrategy = new OneTimeStrategy();
    }

    // ═══════════════════════════════════════════════════════════
    // §1  固定租金（charge_type=1）
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("固定租金-月付-整月：150元/㎡ × 100㎡ × 1个月 = 15,000.00")
    void fixedRent_oneMonth() {
        RentCalculateContext ctx = RentCalculateContext.builder()
                .unitPrice(new BigDecimal("150.00"))
                .area(new BigDecimal("100.00"))
                .stageStart(LocalDate.of(2026, 1, 1))
                .stageEnd(LocalDate.of(2026, 1, 31))
                .build();
        assertEquals(new BigDecimal("15000.00"), fixedRentStrategy.calculate(ctx));
    }

    @Test
    @DisplayName("固定租金-季付-3个月：200元/㎡ × 80㎡ × 3个月 = 48,000.00")
    void fixedRent_quarterly() {
        RentCalculateContext ctx = RentCalculateContext.builder()
                .unitPrice(new BigDecimal("200.00"))
                .area(new BigDecimal("80.00"))
                .stageStart(LocalDate.of(2026, 1, 1))
                .stageEnd(LocalDate.of(2026, 3, 31))
                .build();
        assertEquals(new BigDecimal("48000.00"), fixedRentStrategy.calculate(ctx));
    }

    @Test
    @DisplayName("固定租金-年付-12个月：100元/㎡ × 120㎡ × 12个月 = 144,000.00")
    void fixedRent_annual() {
        RentCalculateContext ctx = RentCalculateContext.builder()
                .unitPrice(new BigDecimal("100.00"))
                .area(new BigDecimal("120.00"))
                .stageStart(LocalDate.of(2026, 1, 1))
                .stageEnd(LocalDate.of(2026, 12, 31))
                .build();
        assertEquals(new BigDecimal("144000.00"), fixedRentStrategy.calculate(ctx));
    }

    @Test
    @DisplayName("固定租金-非整面积：85.50㎡ × 200元/㎡ × 2个月 = 34,200.00")
    void fixedRent_fractionalArea() {
        RentCalculateContext ctx = RentCalculateContext.builder()
                .unitPrice(new BigDecimal("200.00"))
                .area(new BigDecimal("85.50"))
                .stageStart(LocalDate.of(2026, 3, 1))
                .stageEnd(LocalDate.of(2026, 4, 30))
                .build();
        assertEquals(new BigDecimal("34200.00"), fixedRentStrategy.calculate(ctx));
    }

    @Test
    @DisplayName("固定租金-半年6个月：150元/㎡ × 100.50㎡ × 6个月 = 90,450.00")
    void fixedRent_halfYear() {
        RentCalculateContext ctx = RentCalculateContext.builder()
                .unitPrice(new BigDecimal("150.00"))
                .area(new BigDecimal("100.50"))
                .stageStart(LocalDate.of(2026, 1, 1))
                .stageEnd(LocalDate.of(2026, 6, 30))
                .build();
        assertEquals(new BigDecimal("90450.00"), fixedRentStrategy.calculate(ctx));
    }

    @Test
    @DisplayName("固定租金-闰年2月（2024）：整月正确计算1个月")
    void fixedRent_leapYearFeb() {
        RentCalculateContext ctx = RentCalculateContext.builder()
                .unitPrice(new BigDecimal("150.00"))
                .area(new BigDecimal("50.00"))
                .stageStart(LocalDate.of(2024, 2, 1))
                .stageEnd(LocalDate.of(2024, 2, 29)) // 2024年2月有29天
                .build();
        assertEquals(new BigDecimal("7500.00"), fixedRentStrategy.calculate(ctx));
    }

    @Test
    @DisplayName("固定租金-非闰年2月（2026）：整月正确计算1个月")
    void fixedRent_normalYearFeb() {
        RentCalculateContext ctx = RentCalculateContext.builder()
                .unitPrice(new BigDecimal("150.00"))
                .area(new BigDecimal("50.00"))
                .stageStart(LocalDate.of(2026, 2, 1))
                .stageEnd(LocalDate.of(2026, 2, 28)) // 2026年2月只有28天
                .build();
        assertEquals(new BigDecimal("7500.00"), fixedRentStrategy.calculate(ctx));
    }

    @Test
    @DisplayName("固定租金-不足整月（15天）：按当月实际天数折算")
    void fixedRent_partialMonth() {
        // 2026-01-01 ~ 2026-01-15（15天，1月有31天）
        // months = 15/31 ≈ 0.4839...
        // amount = 100 × 100 × (15/31) = 4838.71
        RentCalculateContext ctx = RentCalculateContext.builder()
                .unitPrice(new BigDecimal("100.00"))
                .area(new BigDecimal("100.00"))
                .stageStart(LocalDate.of(2026, 1, 1))
                .stageEnd(LocalDate.of(2026, 1, 15))
                .build();
        BigDecimal result = fixedRentStrategy.calculate(ctx);
        // 100 × 100 × (15/31) = 4838.7096... → HALF_UP → 4838.71
        assertEquals(new BigDecimal("4838.71"), result);
    }

    // ═══════════════════════════════════════════════════════════
    // §2  固定提成（charge_type=2）
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("固定提成-提成额 > 保底：100万 × 5% = 50,000 > 10,000 → 50,000")
    void commission_exceedsMin() {
        RentCalculateContext ctx = RentCalculateContext.builder()
                .revenue(new BigDecimal("1000000.00"))
                .commissionRate(new BigDecimal("5.00"))
                .minCommissionAmount(new BigDecimal("10000.00"))
                .build();
        assertEquals(new BigDecimal("50000.00"), fixedCommissionStrategy.calculate(ctx));
    }

    @Test
    @DisplayName("固定提成-提成额 < 保底（触发保底）：10万 × 5% = 5,000 < 10,000 → 10,000")
    void commission_belowMin_usesFloor() {
        RentCalculateContext ctx = RentCalculateContext.builder()
                .revenue(new BigDecimal("100000.00"))
                .commissionRate(new BigDecimal("5.00"))
                .minCommissionAmount(new BigDecimal("10000.00"))
                .build();
        assertEquals(new BigDecimal("10000.00"), fixedCommissionStrategy.calculate(ctx));
    }

    @Test
    @DisplayName("固定提成-恰好等于保底：20万 × 5% = 10,000 = 保底 → 10,000")
    void commission_equalsMin() {
        RentCalculateContext ctx = RentCalculateContext.builder()
                .revenue(new BigDecimal("200000.00"))
                .commissionRate(new BigDecimal("5.00"))
                .minCommissionAmount(new BigDecimal("10000.00"))
                .build();
        assertEquals(new BigDecimal("10000.00"), fixedCommissionStrategy.calculate(ctx));
    }

    @Test
    @DisplayName("固定提成-零营业额触发保底：0 × 5% = 0 < 8,000 → 8,000")
    void commission_zeroRevenue_usesFloor() {
        RentCalculateContext ctx = RentCalculateContext.builder()
                .revenue(BigDecimal.ZERO)
                .commissionRate(new BigDecimal("5.00"))
                .minCommissionAmount(new BigDecimal("8000.00"))
                .build();
        assertEquals(new BigDecimal("8000.00"), fixedCommissionStrategy.calculate(ctx));
    }

    // ═══════════════════════════════════════════════════════════
    // §3  阶梯提成（charge_type=3）
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("阶梯提成-单阶段模式（无formulaParams）：与固定提成相同")
    void stepCommission_singleStage() {
        RentCalculateContext ctx = RentCalculateContext.builder()
                .revenue(new BigDecimal("500000.00"))
                .commissionRate(new BigDecimal("6.00"))
                .minCommissionAmount(new BigDecimal("20000.00"))
                .build();
        // 500000 × 6% = 30000 > 20000 → 30000
        assertEquals(new BigDecimal("30000.00"), stepCommissionStrategy.calculate(ctx));
    }

    @Test
    @DisplayName("阶梯提成-单阶段触发保底")
    void stepCommission_singleStage_floor() {
        RentCalculateContext ctx = RentCalculateContext.builder()
                .revenue(new BigDecimal("100000.00"))
                .commissionRate(new BigDecimal("6.00"))
                .minCommissionAmount(new BigDecimal("20000.00"))
                .build();
        // 100000 × 6% = 6000 < 20000 → 20000
        assertEquals(new BigDecimal("20000.00"), stepCommissionStrategy.calculate(ctx));
    }

    @Test
    @DisplayName("阶梯提成-多阶段（跨年，各阶段不同费率）：各段求和")
    void stepCommission_multiStage() throws Exception {
        // 第1年：120万营业额 × 5%=60000，第2年：150万 × 6%=90000，共 150000
        String json = """
                {"stages":[
                  {"commission_rate":5.0,"min_commission_amount":10000,"revenue":1200000},
                  {"commission_rate":6.0,"min_commission_amount":12000,"revenue":1500000}
                ]}""";
        RentCalculateContext ctx = RentCalculateContext.builder()
                .formulaParams(objectMapper.readTree(json))
                .build();
        assertEquals(new BigDecimal("150000.00"), stepCommissionStrategy.calculate(ctx));
    }

    @Test
    @DisplayName("阶梯提成-多阶段第一年保底触发：5000 < 10000，第二年正常")
    void stepCommission_multiStage_firstYearFloor() throws Exception {
        String json = """
                {"stages":[
                  {"commission_rate":5.0,"min_commission_amount":10000,"revenue":100000},
                  {"commission_rate":6.0,"min_commission_amount":12000,"revenue":1500000}
                ]}""";
        // 第1年: 5000 < 10000 → 10000；第2年: 90000 > 12000 → 90000；合计 100000
        RentCalculateContext ctx = RentCalculateContext.builder()
                .formulaParams(objectMapper.readTree(json))
                .build();
        assertEquals(new BigDecimal("100000.00"), stepCommissionStrategy.calculate(ctx));
    }

    @Test
    @DisplayName("阶梯提成-多阶段全部触发保底：各阶段均低于保底")
    void stepCommission_allFloor() throws Exception {
        String json = """
                {"stages":[
                  {"commission_rate":1.0,"min_commission_amount":5000,"revenue":100000},
                  {"commission_rate":1.0,"min_commission_amount":6000,"revenue":100000},
                  {"commission_rate":1.0,"min_commission_amount":7000,"revenue":100000}
                ]}""";
        // 1000 < 5000 → 5000; 1000 < 6000 → 6000; 1000 < 7000 → 7000; 合计 18000
        RentCalculateContext ctx = RentCalculateContext.builder()
                .formulaParams(objectMapper.readTree(json))
                .build();
        assertEquals(new BigDecimal("18000.00"), stepCommissionStrategy.calculate(ctx));
    }

    @Test
    @DisplayName("阶梯提成-阶段revenue缺省时回退到context.revenue")
    void stepCommission_fallbackContextRevenue() throws Exception {
        // stages中无revenue字段，使用context.revenue = 500000
        String json = """
                {"stages":[
                  {"commission_rate":5.0,"min_commission_amount":10000},
                  {"commission_rate":6.0,"min_commission_amount":10000}
                ]}""";
        // 500000×5%=25000 + 500000×6%=30000 = 55000
        RentCalculateContext ctx = RentCalculateContext.builder()
                .formulaParams(objectMapper.readTree(json))
                .revenue(new BigDecimal("500000.00"))
                .build();
        assertEquals(new BigDecimal("55000.00"), stepCommissionStrategy.calculate(ctx));
    }

    // ═══════════════════════════════════════════════════════════
    // §4  两者取高（charge_type=4）
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("两者取高-固定 > 提成：固定50,000 > 提成30,000 → 50,000")
    void higherOf_fixedWins() {
        RentCalculateContext ctx = RentCalculateContext.builder()
                .unitPrice(new BigDecimal("100.00"))
                .area(new BigDecimal("500.00"))
                .stageStart(LocalDate.of(2026, 1, 1))
                .stageEnd(LocalDate.of(2026, 1, 31))   // 1个月，固定=50000
                .revenue(new BigDecimal("500000.00"))
                .commissionRate(new BigDecimal("6.00")) // 500000×6%=30000
                .minCommissionAmount(new BigDecimal("10000.00"))
                .build();
        assertEquals(new BigDecimal("50000.00"), higherOfStrategy.calculate(ctx));
    }

    @Test
    @DisplayName("两者取高-提成 > 固定：提成70,000 > 固定30,000 → 70,000")
    void higherOf_commissionWins() {
        RentCalculateContext ctx = RentCalculateContext.builder()
                .unitPrice(new BigDecimal("100.00"))
                .area(new BigDecimal("300.00"))
                .stageStart(LocalDate.of(2026, 1, 1))
                .stageEnd(LocalDate.of(2026, 1, 31))   // 1个月，固定=30000
                .revenue(new BigDecimal("1000000.00"))
                .commissionRate(new BigDecimal("7.00")) // 1000000×7%=70000
                .minCommissionAmount(new BigDecimal("10000.00"))
                .build();
        assertEquals(new BigDecimal("70000.00"), higherOfStrategy.calculate(ctx));
    }

    @Test
    @DisplayName("两者取高-提成保底 > 固定：保底50,000 > 固定30,000 → 50,000")
    void higherOf_commissionFloorBeatsFixed() {
        RentCalculateContext ctx = RentCalculateContext.builder()
                .unitPrice(new BigDecimal("100.00"))
                .area(new BigDecimal("300.00"))
                .stageStart(LocalDate.of(2026, 1, 1))
                .stageEnd(LocalDate.of(2026, 1, 31))   // 固定=30000
                .revenue(new BigDecimal("100000.00"))
                .commissionRate(new BigDecimal("3.00")) // 100000×3%=3000 < 50000保底
                .minCommissionAmount(new BigDecimal("50000.00"))
                .build();
        assertEquals(new BigDecimal("50000.00"), higherOfStrategy.calculate(ctx));
    }

    @Test
    @DisplayName("两者取高-相等时返回固定值")
    void higherOf_equal() {
        RentCalculateContext ctx = RentCalculateContext.builder()
                .unitPrice(new BigDecimal("100.00"))
                .area(new BigDecimal("500.00"))
                .stageStart(LocalDate.of(2026, 1, 1))
                .stageEnd(LocalDate.of(2026, 1, 31))   // 固定=50000
                .revenue(new BigDecimal("1000000.00"))
                .commissionRate(new BigDecimal("5.00")) // 1000000×5%=50000
                .minCommissionAmount(new BigDecimal("10000.00"))
                .build();
        assertEquals(new BigDecimal("50000.00"), higherOfStrategy.calculate(ctx));
    }

    // ═══════════════════════════════════════════════════════════
    // §5  一次性收费（charge_type=5）
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("一次性收费-从formulaParams.amount读取：50,000.00")
    void oneTime_fromFormulaParams() throws Exception {
        RentCalculateContext ctx = RentCalculateContext.builder()
                .formulaParams(objectMapper.readTree("{\"amount\":50000.00}"))
                .build();
        assertEquals(new BigDecimal("50000.00"), oneTimeStrategy.calculate(ctx));
    }

    @Test
    @DisplayName("一次性收费-退化模式：单价 × 面积")
    void oneTime_fallbackUnitPriceArea() {
        RentCalculateContext ctx = RentCalculateContext.builder()
                .unitPrice(new BigDecimal("20.00"))
                .area(new BigDecimal("100.00"))
                .build();
        assertEquals(new BigDecimal("2000.00"), oneTimeStrategy.calculate(ctx));
    }

    // ═══════════════════════════════════════════════════════════
    // §6  calcMonths 辅助方法边界测试
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("calcMonths-闰年2月整月（2024-02-01 ~ 2024-02-29）= 1")
    void calcMonths_leapFeb_exactMonth() {
        BigDecimal months = FixedRentStrategy.calcMonths(
                LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29));
        assertEquals(new BigDecimal("1"), months);
    }

    @Test
    @DisplayName("calcMonths-非闰年2月整月（2026-02-01 ~ 2026-02-28）= 1")
    void calcMonths_normalFeb_exactMonth() {
        BigDecimal months = FixedRentStrategy.calcMonths(
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        assertEquals(new BigDecimal("1"), months);
    }

    @Test
    @DisplayName("calcMonths-1月31日~3月31日 = 2个月整")
    void calcMonths_jan31ToMar31() {
        BigDecimal months = FixedRentStrategy.calcMonths(
                LocalDate.of(2026, 1, 31), LocalDate.of(2026, 3, 30));
        assertEquals(new BigDecimal("2"), months);
    }

    // ═══════════════════════════════════════════════════════════
    // §7  跨年精度 & 边界（任务9.2 补充用例）
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("阶梯提成-小数费率不损失精度：3.7% × 1,000,001 = 37,000.04（非float误差37000.037）")
    void stepCommission_decimalRate_noPrecisionLoss() throws Exception {
        // 如果用 double 运算：1000001 * 0.037 = 37000.037（精度丢失）
        // BigDecimal 应精确：1000001 × 3.7% = 37000.037 → 保底不触发，取 37000.04（HALF_UP）
        String json = """
                {"stages":[
                  {"commission_rate":3.7,"min_commission_amount":1000,"revenue":1000001}
                ]}""";
        RentCalculateContext ctx = RentCalculateContext.builder()
                .formulaParams(objectMapper.readTree(json))
                .build();
        // 1000001 × 3.7% = 37000.037 → HALF_UP → 37000.04
        BigDecimal result = stepCommissionStrategy.calculate(ctx);
        assertEquals(new BigDecimal("37000.04"), result,
                "BigDecimal 应精确到分，不因浮点误差截断");
    }

    @Test
    @DisplayName("阶梯提成-三年跨年合同（三阶段不同费率）：各年提成精确求和")
    void stepCommission_threeYears_crossYear() throws Exception {
        // 第1年：年营业额 120万 × 4% = 48000
        // 第2年：年营业额 150万 × 5% = 75000
        // 第3年：年营业额 180万 × 6% = 108000
        // 合计 = 231000
        String json = """
                {"stages":[
                  {"commission_rate":4.0,"min_commission_amount":5000,"revenue":1200000},
                  {"commission_rate":5.0,"min_commission_amount":6000,"revenue":1500000},
                  {"commission_rate":6.0,"min_commission_amount":7000,"revenue":1800000}
                ]}""";
        RentCalculateContext ctx = RentCalculateContext.builder()
                .formulaParams(objectMapper.readTree(json))
                .build();
        assertEquals(new BigDecimal("231000.00"), stepCommissionStrategy.calculate(ctx));
    }

    @Test
    @DisplayName("阶梯提成-三年跨年第二年保底触发：第一、三年正常，第二年触发保底")
    void stepCommission_threeYears_middleYearFloor() throws Exception {
        // 第1年：500000 × 5% = 25000 > 5000 → 25000
        // 第2年：50000 × 5% = 2500 < 30000保底 → 30000（触发保底）
        // 第3年：800000 × 5% = 40000 > 5000 → 40000
        // 合计 = 95000
        String json = """
                {"stages":[
                  {"commission_rate":5.0,"min_commission_amount":5000,"revenue":500000},
                  {"commission_rate":5.0,"min_commission_amount":30000,"revenue":50000},
                  {"commission_rate":5.0,"min_commission_amount":5000,"revenue":800000}
                ]}""";
        RentCalculateContext ctx = RentCalculateContext.builder()
                .formulaParams(objectMapper.readTree(json))
                .build();
        assertEquals(new BigDecimal("95000.00"), stepCommissionStrategy.calculate(ctx));
    }

    @Test
    @DisplayName("固定租金-跨月非整数：calcMonths 精度确保金额计算正确")
    void fixedRent_partialMonth_preciseAmount() {
        // 2026-01-16 ~ 2026-02-15 = 1个月（Jan16到Feb15，月末+1=Feb16，Feb16-1=Feb15）
        // 面积 200㎡，单价 50元/㎡/月 → 200×50×1=10000
        RentCalculateContext ctx = RentCalculateContext.builder()
                .unitPrice(new BigDecimal("50.00"))
                .area(new BigDecimal("200.00"))
                .stageStart(LocalDate.of(2026, 1, 16))
                .stageEnd(LocalDate.of(2026, 2, 15))
                .build();
        assertEquals(new BigDecimal("10000.00"), fixedRentStrategy.calculate(ctx));
    }

    @Test
    @DisplayName("两者取高-高精度场景：固定租金与提成差额极小时取较大值")
    void higherOf_nearEqual_takesLarger() {
        // 固定：100元 × 100㎡ × 1月 = 10000
        // 提成：200001 × 5% = 10000.05 > 保底8000 → 10000.05
        // 取高：10000.05（提成）> 10000.00（固定）
        RentCalculateContext ctx = RentCalculateContext.builder()
                .unitPrice(new BigDecimal("100.00"))
                .area(new BigDecimal("100.00"))
                .stageStart(LocalDate.of(2026, 1, 1))
                .stageEnd(LocalDate.of(2026, 1, 31))
                .revenue(new BigDecimal("200001.00"))
                .commissionRate(new BigDecimal("5.00"))
                .minCommissionAmount(new BigDecimal("8000.00"))
                .build();
        // 提成 = 200001 × 5% = 10000.05; 固定 = 10000; 取高 = 10000.05
        assertEquals(new BigDecimal("10000.05"), higherOfStrategy.calculate(ctx));
    }
}
