package com.asset.operation.engine;

import com.asset.common.exception.BizException;
import com.asset.operation.OperationApplication;
import com.asset.operation.ledger.entity.OprReceivablePlan;
import com.asset.operation.ledger.mapper.OprReceivablePlanMapper;
import com.asset.operation.termination.entity.OprContractTermination;
import com.asset.operation.termination.entity.OprTerminationSettlement;
import com.asset.operation.termination.mapper.OprContractTerminationMapper;
import com.asset.operation.termination.mapper.OprTerminationSettlementMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 解约清算引擎单元测试（TSE-U）
 * 框架：@SpringBootTest（需查询 inv_lease_contract、biz_shop 等）
 *
 * <p>测试数据依赖（永久数据）：
 * <ul>
 *   <li>解约单 92001：到期解约(1)，草稿(0)，contractId=91003, ledgerId=92003, shopId=90001</li>
 *   <li>解约单 92002：提前解约(2)，审批中(1)，contractId=91003, ledgerId=92003, shopId=90001, penaltyAmount=8000</li>
 *   <li>台账 92003：contractId=91003, status=0(进行中)</li>
 *   <li>应收 92004：ledgerId=92003, amount=10000, received=3000, status=0</li>
 *   <li>应收 92005：ledgerId=92003, amount=10000, received=0, status=0</li>
 *   <li>合同 91003：2025-01-01 ~ 2026-12-31</li>
 *   <li>商铺 90001：shop_status=1(已租)</li>
 * </ul>
 *
 * <p>每个测试方法通过临时 INSERT / UPDATE + @Transactional 自动回滚保证隔离。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = OperationApplication.class
)
@ActiveProfiles("test")
@Transactional
@DisplayName("解约清算引擎（TSE-U）")
class TerminationSettlementEngineTest {

    @Autowired
    private TerminationSettlementEngine engine;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OprContractTerminationMapper terminationMapper;

    @Autowired
    private OprTerminationSettlementMapper settlementMapper;

    @Autowired
    private OprReceivablePlanMapper receivablePlanMapper;

    private static final Long CONTRACT_ID = 91003L;
    private static final Long LEDGER_ID = 92003L;
    private static final Long SHOP_ID = 90001L;

    // ── 辅助方法 ──────────────────────────────────────────────────

    /**
     * 插入临时解约单（自增 ID）
     */
    private Long insertTermination(int terminationType, int status, LocalDate terminationDate,
                                   BigDecimal penaltyAmount, Long newContractId) {
        OprContractTermination t = new OprContractTermination();
        t.setTerminationCode("JY-TEST-" + System.nanoTime());
        t.setContractId(CONTRACT_ID);
        t.setLedgerId(LEDGER_ID);
        t.setProjectId(90001L);
        t.setMerchantId(90002L);
        t.setBrandId(90001L);
        t.setShopId(SHOP_ID);
        t.setTerminationType(terminationType);
        t.setTerminationDate(terminationDate);
        t.setReason("单元测试-" + terminationType);
        t.setNewContractId(newContractId);
        t.setPenaltyAmount(penaltyAmount != null ? penaltyAmount : BigDecimal.ZERO);
        t.setRefundDeposit(BigDecimal.ZERO);
        t.setUnsettledAmount(BigDecimal.ZERO);
        t.setSettlementAmount(BigDecimal.ZERO);
        t.setStatus(status);
        terminationMapper.insert(t);
        return t.getId();
    }

    /**
     * 插入临时应收计划
     */
    private void insertReceivablePlan(Long ledgerId, BigDecimal amount, BigDecimal received, int status) {
        OprReceivablePlan plan = new OprReceivablePlan();
        plan.setLedgerId(ledgerId);
        plan.setContractId(CONTRACT_ID);
        plan.setShopId(SHOP_ID);
        plan.setFeeName("测试费用");
        plan.setAmount(amount);
        plan.setReceivedAmount(received);
        plan.setBillingStart(LocalDate.of(2026, 5, 1));
        plan.setBillingEnd(LocalDate.of(2026, 5, 31));
        plan.setDueDate(LocalDate.of(2026, 5, 31));
        plan.setStatus(status);
        plan.setPushStatus(0);
        plan.setSourceType(1);
        plan.setVersion(1);
        receivablePlanMapper.insert(plan);
    }

    /**
     * 插入临时解约单，指定 ledgerId
     */
    private Long insertTerminationWithLedger(int terminationType, int status, LocalDate terminationDate,
                                              BigDecimal penaltyAmount, Long newContractId, Long ledgerId) {
        OprContractTermination t = new OprContractTermination();
        t.setTerminationCode("JY-TEST-" + System.nanoTime());
        t.setContractId(CONTRACT_ID);
        t.setLedgerId(ledgerId);
        t.setProjectId(90001L);
        t.setMerchantId(90002L);
        t.setBrandId(90001L);
        t.setShopId(SHOP_ID);
        t.setTerminationType(terminationType);
        t.setTerminationDate(terminationDate);
        t.setReason("单元测试-" + terminationType);
        t.setNewContractId(newContractId);
        t.setPenaltyAmount(penaltyAmount != null ? penaltyAmount : BigDecimal.ZERO);
        t.setRefundDeposit(BigDecimal.ZERO);
        t.setUnsettledAmount(BigDecimal.ZERO);
        t.setSettlementAmount(BigDecimal.ZERO);
        t.setStatus(status);
        terminationMapper.insert(t);
        return t.getId();
    }

    /**
     * 创建临时台账（自增 ID），关联合同 91003
     */
    private Long createTempLedger() {
        jdbcTemplate.update(
                "INSERT INTO opr_contract_ledger (ledger_code, contract_id, project_id, merchant_id, brand_id, " +
                "contract_type, contract_start, contract_end, double_sign_status, receivable_status, audit_status, " +
                "status, is_deleted, created_by, created_at, updated_by, updated_at) " +
                "VALUES (?, ?, 90001, 90002, 90001, 1, '2025-01-01', '2026-12-31', 1, 0, 0, 0, 0, '90001', NOW(), '90001', NOW())",
                "TZ-TEST-" + System.nanoTime(), CONTRACT_ID);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    /**
     * 插入临时应收计划到指定台账
     */
    private void insertReceivablePlanForLedger(Long ledgerId, BigDecimal amount, BigDecimal received, int status) {
        OprReceivablePlan plan = new OprReceivablePlan();
        plan.setLedgerId(ledgerId);
        plan.setContractId(CONTRACT_ID);
        plan.setShopId(SHOP_ID);
        plan.setFeeName("测试费用");
        plan.setAmount(amount);
        plan.setReceivedAmount(received);
        plan.setBillingStart(LocalDate.of(2026, 5, 1));
        plan.setBillingEnd(LocalDate.of(2026, 5, 31));
        plan.setDueDate(LocalDate.of(2026, 5, 31));
        plan.setStatus(status);
        plan.setPushStatus(0);
        plan.setSourceType(1);
        plan.setVersion(1);
        receivablePlanMapper.insert(plan);
    }

    /**
     * 查询解约清算明细列表
     */
    private List<OprTerminationSettlement> getSettlements(Long terminationId) {
        return settlementMapper.selectList(
                new LambdaQueryWrapper<OprTerminationSettlement>()
                        .eq(OprTerminationSettlement::getTerminationId, terminationId)
                        .orderByAsc(OprTerminationSettlement::getItemType));
    }

    // ── TSE-U-01：到期解约-汇总未收租费 ─────────────────────────

    @Test
    @DisplayName("TSE-U-01: 到期解约-汇总未收租费")
    void testNaturalSettlement() {
        // 永久数据：应收92004(10000-3000=7000) + 92005(10000-0=10000) → 未收17000
        Long tid = insertTermination(1, 0, LocalDate.of(2026, 12, 31),
                BigDecimal.ZERO, null);

        engine.calculateSettlement(tid);

        OprContractTermination updated = terminationMapper.selectById(tid);
        assertNotNull(updated);
        assertEquals(0, new BigDecimal("17000.00").compareTo(updated.getSettlementAmount()),
                "到期解约清算金额应为 7000+10000=17000");

        List<OprTerminationSettlement> items = getSettlements(tid);
        assertEquals(1, items.size(), "应有1条清算明细");
        assertEquals("未收租费", items.get(0).getItemName());
        assertEquals(1, items.get(0).getItemType());
        assertEquals(0, new BigDecimal("17000.00").compareTo(items.get(0).getAmount()));
    }

    // ── TSE-U-02：提前解约-违约金计算 ────────────────────────────

    @Test
    @DisplayName("TSE-U-02: 提前解约-违约金计算")
    void testEarlyTerminationPenalty() {
        // inv_lease_contract 无 rent_amount 列，引擎会降级从应收均值估算月租
        // 为精确控制月租金=15000，插入独立台账+应收，隔离于永久数据
        Long testLedgerId = createTempLedger();
        insertReceivablePlanForLedger(testLedgerId, new BigDecimal("15000"), BigDecimal.ZERO, 0);

        // 解约日期 2026-07-04，合同到期 2026-12-31
        LocalDate terminationDate = LocalDate.of(2026, 7, 4);
        Long tid = insertTerminationWithLedger(2, 0, terminationDate, BigDecimal.ZERO, null, testLedgerId);

        engine.calculateSettlement(tid);

        OprContractTermination updated = terminationMapper.selectById(tid);
        assertNotNull(updated);

        // 违约金 = dailyRent × remainDays × 0.3
        // 月租=AVG(15000)=15000, dailyRent = 15000 / 30 = 500
        // remainDays = ChronoUnit.DAYS.between(2026-07-04, 2026-12-31) = 180
        // penalty = 500 × 180 × 0.3 = 27000
        long remainDays = ChronoUnit.DAYS.between(terminationDate, LocalDate.of(2026, 12, 31));
        BigDecimal dailyRent = new BigDecimal("15000")
                .divide(BigDecimal.valueOf(30), 10, RoundingMode.HALF_UP);
        BigDecimal expectedPenalty = dailyRent
                .multiply(BigDecimal.valueOf(remainDays))
                .multiply(new BigDecimal("0.30"))
                .setScale(2, RoundingMode.HALF_UP);

        assertEquals(0, expectedPenalty.compareTo(updated.getPenaltyAmount()),
                "违约金应为 dailyRent×" + remainDays + "×0.3=" + expectedPenalty);

        // 清算明细应有2条：未收租费 + 违约金
        List<OprTerminationSettlement> items = getSettlements(tid);
        assertEquals(2, items.size(), "应有2条清算明细(未收租费+违约金)");

        assertTrue(items.stream().anyMatch(i -> i.getItemType() == 1), "应有未收租费明细");
        assertTrue(items.stream().anyMatch(i -> i.getItemType() == 2), "应有违约金明细");
    }

    // ── TSE-U-03：提前解约-月租不可用时从应收均值估算 ──────────

    @Test
    @DisplayName("TSE-U-03: 提前解约-月租不可用时从应收均值估算")
    void testEarlyPenaltyFromReceivableAvg() {
        // inv_lease_contract 无 rent_amount 列，引擎自动降级到 AVG(opr_receivable_plan.amount)
        // 台账 92003 下应收 92004(10000) + 92005(10000)，均值=10000
        LocalDate terminationDate = LocalDate.of(2026, 7, 4);
        Long tid = insertTermination(2, 0, terminationDate, BigDecimal.ZERO, null);

        engine.calculateSettlement(tid);

        OprContractTermination updated = terminationMapper.selectById(tid);
        assertNotNull(updated);

        // 应收均值 = AVG(10000,10000) = 10000
        // dailyRent = 10000 / 30 ≈ 333.33...
        // penalty = dailyRent × remainDays × 0.3
        long remainDays = ChronoUnit.DAYS.between(terminationDate, LocalDate.of(2026, 12, 31));
        BigDecimal avgMonthly = new BigDecimal("10000");
        BigDecimal dailyRent = avgMonthly.divide(BigDecimal.valueOf(30), 10, RoundingMode.HALF_UP);
        BigDecimal expectedPenalty = dailyRent
                .multiply(BigDecimal.valueOf(remainDays))
                .multiply(new BigDecimal("0.30"))
                .setScale(2, RoundingMode.HALF_UP);

        assertEquals(0, expectedPenalty.compareTo(updated.getPenaltyAmount()),
                "违约金应基于应收均值估算，预期=" + expectedPenalty);
    }

    // ── TSE-U-04：重签解约-清算金额为0 ──────────────────────────

    @Test
    @DisplayName("TSE-U-04: 重签解约-清算金额为0")
    void testRenewalSettlement() {
        Long newContractId = 99999L;
        Long tid = insertTermination(3, 0, LocalDate.of(2026, 12, 31),
                BigDecimal.ZERO, newContractId);

        engine.calculateSettlement(tid);

        OprContractTermination updated = terminationMapper.selectById(tid);
        assertNotNull(updated);
        assertEquals(0, BigDecimal.ZERO.compareTo(updated.getSettlementAmount()),
                "重签解约清算金额应为0");

        // 明细：应有1条冲减记录，金额为负
        List<OprTerminationSettlement> items = getSettlements(tid);
        assertEquals(1, items.size(), "应有1条冲减明细");
        assertTrue(items.get(0).getAmount().compareTo(BigDecimal.ZERO) < 0,
                "重签解约明细金额应为负数(冲减)");
        assertTrue(items.get(0).getRemark().contains(String.valueOf(newContractId)),
                "备注应包含新合同ID");
    }

    // ── TSE-U-05：执行解约-作废所有待收应收 ─────────────────────

    @Test
    @DisplayName("TSE-U-05: 执行解约-作废所有待收应收")
    void testExecuteVoidsReceivables() {
        // 使用一个独立台账+应收来隔离测试
        Long testLedgerId = 92003L; // 永久台账
        // 插入额外的待收应收
        insertReceivablePlan(testLedgerId, new BigDecimal("5000"), BigDecimal.ZERO, 0);

        // 创建审批中状态的解约单
        Long tid = insertTermination(1, 1, LocalDate.of(2026, 12, 31),
                BigDecimal.ZERO, null);

        engine.execute(tid);

        // 验证台账下所有待收应收均已作废(status=3)
        List<OprReceivablePlan> plans = receivablePlanMapper.selectList(
                new LambdaQueryWrapper<OprReceivablePlan>()
                        .eq(OprReceivablePlan::getLedgerId, testLedgerId)
                        .eq(OprReceivablePlan::getIsDeleted, 0));
        for (OprReceivablePlan p : plans) {
            // 原本待收(0)/部分收(1)的应变为已作废(3)，已收(2)保持不变
            if (p.getId() == 92003L) {
                // 92003 是已收(status=2)，不应变化
                assertEquals(2, p.getStatus(), "已收应收不应被作废");
            } else {
                assertEquals(3, p.getStatus(),
                        "待收/部分收应收应被作废(status=3)，planId=" + p.getId());
            }
        }
    }

    // ── TSE-U-06：执行解约-台账状态→已解约(2) ───────────────────

    @Test
    @DisplayName("TSE-U-06: 执行解约-台账状态→已解约(2)")
    void testExecuteUpdatesLedgerStatus() {
        // 创建审批中状态的解约单
        Long tid = insertTermination(1, 1, LocalDate.of(2026, 12, 31),
                BigDecimal.ZERO, null);

        engine.execute(tid);

        // 验证台账 92003 状态变为已解约(2)
        Integer ledgerStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM opr_contract_ledger WHERE id=? AND is_deleted=0",
                Integer.class, LEDGER_ID);
        assertEquals(2, ledgerStatus, "台账状态应变为已解约(2)");
    }

    // ── TSE-U-07：执行解约-合同状态→已解约(5) ───────────────────

    @Test
    @DisplayName("TSE-U-07: 执行解约-合同状态→已解约(5)【跨模块写入 inv_lease_contract】")
    void testExecuteUpdatesContractStatus() {
        Long tid = insertTermination(1, 1, LocalDate.of(2026, 12, 31),
                BigDecimal.ZERO, null);

        engine.execute(tid);

        // **跨模块验证**：inv_lease_contract.status → 5
        Integer contractStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM inv_lease_contract WHERE id=? AND is_deleted=0",
                Integer.class, CONTRACT_ID);
        assertEquals(5, contractStatus,
                "合同状态应变为已解约(5)，跨模块写入 inv_lease_contract");
    }

    // ── TSE-U-08：执行解约-商铺状态→空置(0) ─────────────────────

    @Test
    @DisplayName("TSE-U-08: 执行解约-商铺状态→空置(0)【跨模块写入 biz_shop】")
    void testExecuteUpdatesShopStatus() {
        // 确保商铺当前为已租(1)
        jdbcTemplate.update("UPDATE biz_shop SET shop_status=1 WHERE id=?", SHOP_ID);

        Long tid = insertTermination(1, 1, LocalDate.of(2026, 12, 31),
                BigDecimal.ZERO, null);

        engine.execute(tid);

        // **跨模块验证**：biz_shop.shop_status → 0
        Integer shopStatus = jdbcTemplate.queryForObject(
                "SELECT shop_status FROM biz_shop WHERE id=? AND is_deleted=0",
                Integer.class, SHOP_ID);
        assertEquals(0, shopStatus,
                "商铺状态应变为空置可租(0)，跨模块写入 biz_shop");
    }

    // ── TSE-U-09：非草稿/驳回状态拒绝计算 ──────────────────────

    @Test
    @DisplayName("TSE-U-09: 非草稿/驳回状态拒绝计算")
    void testRejectNonDraftStatus() {
        // 永久数据 92002：status=1(审批中)
        BizException ex = assertThrows(BizException.class,
                () -> engine.calculateSettlement(92002L));
        assertTrue(ex.getMessage().contains("草稿") || ex.getMessage().contains("驳回")
                        || ex.getMessage().contains("状态"),
                "异常消息应提示状态限制，实际: " + ex.getMessage());
    }
}
