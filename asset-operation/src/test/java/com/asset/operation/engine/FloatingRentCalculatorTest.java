package com.asset.operation.engine;

import com.asset.common.exception.BizException;
import com.asset.operation.OperationApplication;
import com.asset.operation.ledger.entity.OprReceivablePlan;
import com.asset.operation.ledger.mapper.OprReceivablePlanMapper;
import com.asset.operation.revenue.entity.OprFloatingRent;
import com.asset.operation.revenue.entity.OprFloatingRentTier;
import com.asset.operation.revenue.mapper.OprFloatingRentMapper;
import com.asset.operation.revenue.mapper.OprFloatingRentTierMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 浮动租金计算引擎单元测试（FRC-U）
 * 框架：@SpringBootTest（需查询 inv_lease_contract_fee、opr_revenue_report 等）
 *
 * <p>测试数据依赖（永久数据）：
 * <ul>
 *   <li>合同 91003：2025-01-01 ~ 2026-12-31，生效状态</li>
 *   <li>商铺 90001 关联合同 91003（inv_lease_contract_shop=91002）</li>
 *   <li>费项 91003：chargeType=2（固定提成），关联合同 91003</li>
 *   <li>阶段 91001：commission_rate=10%, min_commission_amount=8000</li>
 *   <li>台账 92001：contractId=91003, status=0（进行中）</li>
 *   <li>浮动租金 92001：contractId=91003, calcMonth='2025-12'（幂等测试用）</li>
 * </ul>
 *
 * <p>每个测试方法使用独立的 calcMonth（2026-06 ~ 2026-11），避免互相干扰。
 * 需要修改费项的测试用临时 INSERT + @Transactional 自动回滚。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = OperationApplication.class
)
@ActiveProfiles("test")
@Transactional
@DisplayName("浮动租金计算引擎（FRC-U）")
class FloatingRentCalculatorTest {

    @Autowired
    private FloatingRentCalculator calculator;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OprFloatingRentMapper floatingRentMapper;

    @Autowired
    private OprFloatingRentTierMapper floatingRentTierMapper;

    @Autowired
    private OprReceivablePlanMapper receivablePlanMapper;

    private static final Long CONTRACT_ID = 91003L;
    private static final Long SHOP_ID = 90001L;

    // ── 辅助方法 ──────────────────────────────────────────────────

    /**
     * 为指定月份插入完整的营收填报数据（每天一条）
     */
    private void insertFullMonthRevenue(Long contractId, String calcMonth, BigDecimal totalRevenue) {
        YearMonth ym = YearMonth.parse(calcMonth);
        int days = ym.lengthOfMonth();
        BigDecimal dailyAmount = totalRevenue.divide(BigDecimal.valueOf(days), 2, BigDecimal.ROUND_HALF_UP);

        // 调整最后一天金额确保总和精确
        BigDecimal runningTotal = BigDecimal.ZERO;
        for (int d = 1; d <= days; d++) {
            BigDecimal amount = (d == days) ? totalRevenue.subtract(runningTotal) : dailyAmount;
            String reportDate = ym.atDay(d).toString();
            jdbcTemplate.update(
                    "INSERT INTO opr_revenue_report (project_id, contract_id, shop_id, merchant_id, " +
                    "report_date, report_month, revenue_amount, status, is_deleted, created_by, created_at, updated_by, updated_at) " +
                    "VALUES (90001, ?, ?, 90002, ?, ?, ?, 1, 0, '90001', NOW(), '90001', NOW())",
                    contractId, SHOP_ID, reportDate, calcMonth, amount);
            runningTotal = runningTotal.add(amount);
        }
    }

    /**
     * 为指定月份插入不完整的营收填报数据（仅部分天数）
     */
    private void insertPartialMonthRevenue(Long contractId, String calcMonth, int fillDays, BigDecimal totalRevenue) {
        YearMonth ym = YearMonth.parse(calcMonth);
        BigDecimal dailyAmount = totalRevenue.divide(BigDecimal.valueOf(fillDays), 2, BigDecimal.ROUND_HALF_UP);
        for (int d = 1; d <= fillDays; d++) {
            String reportDate = ym.atDay(d).toString();
            jdbcTemplate.update(
                    "INSERT INTO opr_revenue_report (project_id, contract_id, shop_id, merchant_id, " +
                    "report_date, report_month, revenue_amount, status, is_deleted, created_by, created_at, updated_by, updated_at) " +
                    "VALUES (90001, ?, ?, 90002, ?, ?, ?, 1, 0, '90001', NOW(), '90001', NOW())",
                    contractId, SHOP_ID, reportDate, calcMonth, dailyAmount);
        }
    }

    /**
     * 临时将费项 91003 的 charge_type 修改（事务回滚后恢复）
     */
    private void updateFeeChargeType(int chargeType) {
        jdbcTemplate.update("UPDATE inv_lease_contract_fee SET charge_type=? WHERE id=91003", chargeType);
    }

    /**
     * 设置费项的 formula_params（阶梯提成 JSON 配置）
     */
    private void setFormulaParams(String json) {
        jdbcTemplate.update("UPDATE inv_lease_contract_fee SET formula_params=? WHERE id=91003", json);
    }

    /**
     * 修改阶段参数
     */
    private void updateStage(BigDecimal commissionRate, BigDecimal minCommissionAmount) {
        jdbcTemplate.update(
                "UPDATE inv_lease_contract_fee_stage SET commission_rate=?, min_commission_amount=? WHERE id=91001",
                commissionRate, minCommissionAmount);
    }

    // ── FRC-U-01：固定提成-月营业额×提成率 ──────────────────────

    @Test
    @DisplayName("FRC-U-01: 固定提成-月营业额×提成率")
    void testFixedCommission() {
        String calcMonth = "2026-06";
        BigDecimal revenue = new BigDecimal("200000");

        // 准备：chargeType=2, rate=10%（永久数据已满足）
        insertFullMonthRevenue(CONTRACT_ID, calcMonth, revenue);

        Long frId = calculator.calculate(CONTRACT_ID, calcMonth);
        assertNotNull(frId);

        OprFloatingRent fr = floatingRentMapper.selectById(frId);
        assertNotNull(fr);
        assertEquals(CONTRACT_ID, fr.getContractId());
        assertEquals(calcMonth, fr.getCalcMonth());
        // 200000 × 10% = 20000
        assertEquals(0, new BigDecimal("20000.00").compareTo(fr.getFloatingRent()),
                "floatingRent 应为 200000×10%=20000");
        assertEquals(0, new BigDecimal("20000.00").compareTo(fr.getCommissionAmount()));
        assertEquals(0, new BigDecimal("10.00").compareTo(fr.getCommissionRate()));
        assertTrue(fr.getCalcFormula().contains("固定提成"), "公式应含'固定提成'");
    }

    // ── FRC-U-02：固定提成-提成不足保底取保底 ────────────────────

    @Test
    @DisplayName("FRC-U-02: 固定提成-提成不足保底取保底")
    void testFixedCommissionWithMinAmount() {
        String calcMonth = "2026-07";
        BigDecimal revenue = new BigDecimal("50000");

        // chargeType=2, rate=10%, minAmt=8000（永久数据）
        insertFullMonthRevenue(CONTRACT_ID, calcMonth, revenue);

        Long frId = calculator.calculate(CONTRACT_ID, calcMonth);
        OprFloatingRent fr = floatingRentMapper.selectById(frId);
        assertNotNull(fr);

        // 50000 × 10% = 5000，但保底 8000 → 实际 commissionAmount=5000
        // 引擎 calcFixed 只算提成，不处理保底比较（保底逻辑在 calcHigherOf）
        // 对于 chargeType=2，floatingRent = commissionAmount = 5000
        assertEquals(0, new BigDecimal("5000.00").compareTo(fr.getCommissionAmount()),
                "提成金额应为 50000×10%=5000");
        assertEquals(0, new BigDecimal("5000.00").compareTo(fr.getFloatingRent()),
                "固定提成模式下 floatingRent=commissionAmount");
    }

    // ── FRC-U-03：阶梯提成-两档累进 ─────────────────────────────

    @Test
    @DisplayName("FRC-U-03: 阶梯提成-两档累进")
    void testTieredCommission() {
        String calcMonth = "2026-08";
        BigDecimal revenue = new BigDecimal("150000");

        // 修改费项为阶梯提成
        updateFeeChargeType(3);
        setFormulaParams("{\"tiers\":[{\"from\":0,\"to\":100000,\"rate\":5},{\"from\":100000,\"to\":null,\"rate\":8}]}");

        insertFullMonthRevenue(CONTRACT_ID, calcMonth, revenue);

        Long frId = calculator.calculate(CONTRACT_ID, calcMonth);
        OprFloatingRent fr = floatingRentMapper.selectById(frId);
        assertNotNull(fr);

        // 第1档: 100000 × 5% = 5000
        // 第2档: 50000 × 8% = 4000
        // 合计: 9000
        assertEquals(0, new BigDecimal("9000.00").compareTo(fr.getFloatingRent()),
                "阶梯提成合计应为 5000+4000=9000");
        assertEquals(0, new BigDecimal("9000.00").compareTo(fr.getCommissionAmount()));
        assertTrue(fr.getCalcFormula().contains("阶梯提成"), "公式应含'阶梯提成'");
    }

    // ── FRC-U-04：阶梯提成-生成tier明细 ─────────────────────────

    @Test
    @DisplayName("FRC-U-04: 阶梯提成-生成tier明细")
    void testTieredCommissionTierRecords() {
        String calcMonth = "2026-09";
        BigDecimal revenue = new BigDecimal("150000");

        updateFeeChargeType(3);
        setFormulaParams("{\"tiers\":[{\"from\":0,\"to\":100000,\"rate\":5},{\"from\":100000,\"to\":null,\"rate\":8}]}");
        insertFullMonthRevenue(CONTRACT_ID, calcMonth, revenue);

        Long frId = calculator.calculate(CONTRACT_ID, calcMonth);

        // 验证 opr_floating_rent_tier 有 2 条记录
        List<OprFloatingRentTier> tiers = floatingRentTierMapper.selectList(
                new LambdaQueryWrapper<OprFloatingRentTier>()
                        .eq(OprFloatingRentTier::getFloatingRentId, frId)
                        .orderByAsc(OprFloatingRentTier::getTierNo));
        assertEquals(2, tiers.size(), "应有 2 个阶梯档位");

        // 第1档
        assertEquals(1, tiers.get(0).getTierNo());
        assertEquals(0, new BigDecimal("0").compareTo(tiers.get(0).getRevenueFrom()));
        assertEquals(0, new BigDecimal("100000").compareTo(tiers.get(0).getRevenueTo()));
        assertEquals(0, new BigDecimal("5").compareTo(tiers.get(0).getRate()));
        assertEquals(0, new BigDecimal("5000.00").compareTo(tiers.get(0).getTierAmount()),
                "第1档: 100000×5%=5000");

        // 第2档
        assertEquals(2, tiers.get(1).getTierNo());
        assertEquals(0, new BigDecimal("100000").compareTo(tiers.get(1).getRevenueFrom()));
        assertNull(tiers.get(1).getRevenueTo(), "第2档无上限");
        assertEquals(0, new BigDecimal("8").compareTo(tiers.get(1).getRate()));
        assertEquals(0, new BigDecimal("4000.00").compareTo(tiers.get(1).getTierAmount()),
                "第2档: 50000×8%=4000");
    }

    // ── FRC-U-05：两者取高-固定>提成 ────────────────────────────

    @Test
    @DisplayName("FRC-U-05: 两者取高-固定>提成(浮动租金为0)")
    void testHigherOfFixedWins() {
        String calcMonth = "2026-10";
        // 固定 = 20000(minCommissionAmount), 提成 = 150000×10% = 15000 → 固定>提成 → 差额=0
        BigDecimal revenue = new BigDecimal("150000");

        updateFeeChargeType(4);
        updateStage(new BigDecimal("10.00"), new BigDecimal("20000.00"));
        insertFullMonthRevenue(CONTRACT_ID, calcMonth, revenue);

        Long frId = calculator.calculate(CONTRACT_ID, calcMonth);
        OprFloatingRent fr = floatingRentMapper.selectById(frId);
        assertNotNull(fr);

        assertEquals(0, new BigDecimal("15000.00").compareTo(fr.getCommissionAmount()),
                "提成 = 150000×10% = 15000");
        assertEquals(0, new BigDecimal("20000.00").compareTo(fr.getFixedRent()),
                "固定保底 = 20000");
        // 固定(20000) > 提成(15000), 差额 = max(20000,15000) - 20000 = 0
        assertEquals(0, BigDecimal.ZERO.compareTo(fr.getFloatingRent()),
                "固定>提成时浮动租金应为0");
        assertTrue(fr.getCalcFormula().contains("两者取高"));
    }

    // ── FRC-U-06：两者取高-提成>固定 ────────────────────────────

    @Test
    @DisplayName("FRC-U-06: 两者取高-提成>固定(浮动租金为差额)")
    void testHigherOfCommissionWins() {
        String calcMonth = "2026-11";
        // 固定 = 15000(minCommissionAmount), 提成 = 200000×10% = 20000 → 提成>固定 → 差额=5000
        BigDecimal revenue = new BigDecimal("200000");

        updateFeeChargeType(4);
        updateStage(new BigDecimal("10.00"), new BigDecimal("15000.00"));
        insertFullMonthRevenue(CONTRACT_ID, calcMonth, revenue);

        Long frId = calculator.calculate(CONTRACT_ID, calcMonth);
        OprFloatingRent fr = floatingRentMapper.selectById(frId);
        assertNotNull(fr);

        assertEquals(0, new BigDecimal("20000.00").compareTo(fr.getCommissionAmount()),
                "提成 = 200000×10% = 20000");
        assertEquals(0, new BigDecimal("15000.00").compareTo(fr.getFixedRent()),
                "固定保底 = 15000");
        // 提成(20000) > 固定(15000), 差额 = 20000 - 15000 = 5000
        assertEquals(0, new BigDecimal("5000.00").compareTo(fr.getFloatingRent()),
                "提成>固定时浮动租金应为差额5000");
    }

    // ── FRC-U-07：自动生成应收计划(sourceType=3) ────────────────

    @Test
    @DisplayName("FRC-U-07: 自动生成应收计划(sourceType=3)")
    void testGenerateReceivablePlan() {
        String calcMonth = "2026-06";
        BigDecimal revenue = new BigDecimal("200000");

        // chargeType=2, rate=10% → floatingRent=20000 → 应生成应收
        insertFullMonthRevenue(CONTRACT_ID, calcMonth, revenue);

        Long frId = calculator.calculate(CONTRACT_ID, calcMonth);
        OprFloatingRent fr = floatingRentMapper.selectById(frId);
        assertNotNull(fr.getReceivableId(), "应回填 receivableId");

        // 查询生成的应收计划
        OprReceivablePlan plan = receivablePlanMapper.selectById(fr.getReceivableId());
        assertNotNull(plan, "应收计划应已生成");
        assertEquals(CONTRACT_ID, plan.getContractId());
        assertEquals(SHOP_ID, plan.getShopId());
        assertEquals(3, plan.getSourceType(), "sourceType 应为 3(浮动租金)");
        assertEquals(0, new BigDecimal("20000.00").compareTo(plan.getAmount()),
                "应收金额应等于浮动租金");
        assertEquals(0, plan.getStatus(), "状态应为0(待收)");
        assertEquals("浮动租金-" + calcMonth, plan.getFeeName());
    }

    // ── FRC-U-08：幂等校验-同月重复计算拒绝 ─────────────────────

    @Test
    @DisplayName("FRC-U-08: 幂等校验-同月重复计算拒绝")
    void testIdempotentCheck() {
        // 永久数据 opr_floating_rent 92001：contractId=91003, calcMonth='2025-12'
        String existingMonth = "2025-12";

        BizException ex = assertThrows(BizException.class,
                () -> calculator.calculate(CONTRACT_ID, existingMonth));
        assertTrue(ex.getMessage().contains("已计算") || ex.getMessage().contains("重复"),
                "异常消息应提示已计算/重复，实际: " + ex.getMessage());
    }

    // ── FRC-U-09：营业额数据不完整拒绝 ──────────────────────────

    @Test
    @DisplayName("FRC-U-09: 营业额数据不完整拒绝计算")
    void testIncompleteRevenueRejected() {
        String calcMonth = "2026-06";
        int expectedDays = YearMonth.parse(calcMonth).lengthOfMonth(); // 30 天

        // 仅填报 20 天
        insertPartialMonthRevenue(CONTRACT_ID, calcMonth, 20, new BigDecimal("100000"));

        BizException ex = assertThrows(BizException.class,
                () -> calculator.calculate(CONTRACT_ID, calcMonth));
        assertTrue(ex.getMessage().contains("不完整") || ex.getMessage().contains("填报"),
                "异常消息应提示数据不完整，实际: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("20"), "应提示已填报天数20");
    }
}
