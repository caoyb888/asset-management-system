package com.asset.operation.engine;

import com.asset.operation.OperationApplication;
import com.asset.operation.change.dto.ChangeImpactVO;
import com.asset.operation.change.entity.OprContractChange;
import com.asset.operation.change.entity.OprContractChangeSnapshot;
import com.asset.operation.change.mapper.OprContractChangeMapper;
import com.asset.operation.change.mapper.OprContractChangeSnapshotMapper;
import com.asset.operation.ledger.entity.OprContractLedger;
import com.asset.operation.ledger.entity.OprReceivablePlan;
import com.asset.operation.ledger.mapper.OprContractLedgerMapper;
import com.asset.operation.ledger.mapper.OprReceivablePlanMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 应收重算引擎单元测试（RRE-U）
 * 框架：@SpringBootTest（需真实数据库读写 opr_receivable_plan）
 *
 * 测试数据依赖（永久数据）：
 * - 台账 92003：contractId=91003, status=0(进行中), receivableStatus=2
 * - 合同 91003：2025-01-01 ~ 2026-12-31，生效状态
 * - 应收计划 92004/92005：台账92003下的待收应收（billingStart 2026-02/03，用于隔离验证）
 *
 * 每个测试方法自行插入临时变更单和应收计划（ID 92050+），@Transactional 自动回滚。
 * effectiveDate 统一使用 2026-04-01，避免干扰永久数据 92004/92005（billingStart < 04-01）。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = OperationApplication.class
)
@ActiveProfiles("test")
@Transactional
@DisplayName("应收重算引擎（RRE-U）")
class ReceivableRecalculateEngineTest {

    @Autowired
    private ReceivableRecalculateEngine engine;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OprReceivablePlanMapper receivablePlanMapper;

    @Autowired
    private OprContractChangeMapper changeMapper;

    @Autowired
    private OprContractChangeSnapshotMapper snapshotMapper;

    @Autowired
    private OprContractLedgerMapper ledgerMapper;

    // ────────────────────────────────────────────────────────────────────
    // 辅助方法
    // ────────────────────────────────────────────────────────────────────

    /**
     * 插入测试变更单（带 impact_summary JSON）
     */
    private void insertChange(long id, long contractId, long ledgerId,
                              String effectiveDate, String impactSummaryJson) {
        jdbcTemplate.update("""
                INSERT INTO opr_contract_change
                    (id, change_code, contract_id, ledger_id, project_id,
                     status, effective_date, reason, impact_summary,
                     is_deleted, created_by, created_at, updated_by, updated_at)
                VALUES (?, ?, ?, ?, 90001,
                        0, ?, '重算引擎测试', ?,
                        0, '90001', NOW(), '90001', NOW())
                """, id, "BG-RRE-" + id, contractId, ledgerId,
                effectiveDate, impactSummaryJson);
    }

    /**
     * 插入变更类型记录
     */
    private void insertChangeType(long id, long changeId, String typeCode) {
        jdbcTemplate.update("""
                INSERT INTO opr_contract_change_type
                    (id, change_id, change_type_code,
                     is_deleted, created_by, created_at, updated_by, updated_at)
                VALUES (?, ?, ?, 0, '90001', NOW(), '90001', NOW())
                """, id, changeId, typeCode);
    }

    /**
     * 插入应收计划（status=0 待收，sourceType=1 合同生成）
     */
    private void insertPlan(long id, long ledgerId, String billingStart,
                            String billingEnd, BigDecimal amount,
                            int pushStatus, int version) {
        jdbcTemplate.update("""
                INSERT INTO opr_receivable_plan
                    (id, ledger_id, contract_id, shop_id, fee_item_id, fee_name,
                     billing_start, billing_end, due_date,
                     amount, received_amount, status,
                     push_status, source_type, version,
                     is_deleted, created_by, created_at, updated_by, updated_at)
                VALUES (?, ?, 91003, 90001, 91001, '测试租金',
                        ?, ?, ?,
                        ?, 0.00, 0,
                        ?, 1, ?,
                        0, '90001', NOW(), '90001', NOW())
                """, id, ledgerId, billingStart, billingEnd, billingStart,
                amount, pushStatus, version);
    }

    // ========================================================================
    // RRE-U-01: 租金变更-按比例重算未推送应收
    // ========================================================================
    @Test
    @DisplayName("RRE-U-01: 租金变更-按比例重算未推送应收（原地更新）")
    void execute_rentChange_unpushedPlan_shouldUpdateInPlace() {
        // 准备：变更单 + RENT 类型 + 1笔未推送应收(amount=15000)
        String impactJson = """
                {"changeFields":{"newRentAmount":"18000","oldRentAmount":"15000"}}""";
        insertChange(92050, 91003, 92003, "2026-04-01", impactJson);
        insertChangeType(92050, 92050, "RENT");
        insertPlan(92050, 92003, "2026-04-01", "2026-04-30",
                new BigDecimal("15000"), 0, 1);

        OprContractChange change = changeMapper.selectById(92050L);
        assertNotNull(change);

        // 执行
        engine.execute(change);

        // 验证：原地更新，amount = 15000 × (18000/15000) = 18000，version 递增
        OprReceivablePlan updated = receivablePlanMapper.selectById(92050L);
        assertNotNull(updated, "应收计划应仍存在（原地更新）");
        assertEquals(0, new BigDecimal("18000").compareTo(updated.getAmount()),
                "金额应按比例重算为 18000");
        assertEquals(2, updated.getVersion(), "version 应递增为 2");
        assertEquals(2, updated.getSourceType(), "sourceType 应为 2(变更生成)");
        assertEquals(0, updated.getStatus(), "status 应保持 0(待收)");
    }

    // ========================================================================
    // RRE-U-02: 租金变更-已推送应收红冲+新建
    // ========================================================================
    @Test
    @DisplayName("RRE-U-02: 租金变更-已推送应收红冲+新建")
    void execute_rentChange_pushedPlan_shouldVoidAndCreateNew() {
        // 准备：1笔已推送应收(pushStatus=1, amount=15000)
        String impactJson = """
                {"changeFields":{"newRentAmount":"18000","oldRentAmount":"15000"}}""";
        insertChange(92051, 91003, 92003, "2026-04-01", impactJson);
        insertChangeType(92051, 92051, "RENT");
        insertPlan(92051, 92003, "2026-04-01", "2026-04-30",
                new BigDecimal("15000"), 1, 1);

        OprContractChange change = changeMapper.selectById(92051L);
        engine.execute(change);

        // 验证：原记录被红冲（status=3 作废）
        // selectById 仅过滤 is_deleted=0，不过滤 status
        OprReceivablePlan voided = receivablePlanMapper.selectById(92051L);
        assertNotNull(voided, "原应收记录应仍存在");
        assertEquals(3, voided.getStatus(), "原记录应被红冲(status=3)");

        // 验证：新建记录 version=2, sourceType=2, amount=18000, pushStatus=0
        List<OprReceivablePlan> newPlans = receivablePlanMapper.selectList(
                new LambdaQueryWrapper<OprReceivablePlan>()
                        .eq(OprReceivablePlan::getLedgerId, 92003L)
                        .eq(OprReceivablePlan::getBillingStart, LocalDate.of(2026, 4, 1))
                        .eq(OprReceivablePlan::getStatus, 0)
                        .ne(OprReceivablePlan::getId, 92051L));
        assertFalse(newPlans.isEmpty(), "应新建一条应收记录");

        OprReceivablePlan newPlan = newPlans.get(0);
        assertEquals(0, new BigDecimal("18000").compareTo(newPlan.getAmount()),
                "新记录金额应为 18000");
        assertEquals(2, newPlan.getVersion(), "新记录 version 应为 原+1=2");
        assertEquals(2, newPlan.getSourceType(), "新记录 sourceType 应为 2(变更生成)");
        assertEquals(0, newPlan.getPushStatus(), "新记录 pushStatus 应为 0(待推送)");
    }

    // ========================================================================
    // RRE-U-03: 面积变更-按面积比例重算
    // ========================================================================
    @Test
    @DisplayName("RRE-U-03: 面积变更-按面积比例重算")
    void execute_areaChange_shouldRecalculateByAreaRatio() {
        // 准备：AREA 变更，newArea=120, oldArea=100
        String impactJson = """
                {"changeFields":{"newRentArea":"120","oldRentArea":"100"}}""";
        insertChange(92052, 91003, 92003, "2026-04-01", impactJson);
        insertChangeType(92052, 92052, "AREA");
        insertPlan(92052, 92003, "2026-04-01", "2026-04-30",
                new BigDecimal("15000"), 0, 1);

        OprContractChange change = changeMapper.selectById(92052L);
        engine.execute(change);

        // 验证：15000 × (120/100) = 18000
        OprReceivablePlan updated = receivablePlanMapper.selectById(92052L);
        assertNotNull(updated);
        assertEquals(0, new BigDecimal("18000.00").compareTo(updated.getAmount()),
                "金额应按面积比例重算为 18000 (15000×120/100)");
        assertEquals(2, updated.getVersion(), "version 应递增");
        assertEquals(2, updated.getSourceType(), "sourceType 应为 2(变更生成)");
    }

    // ========================================================================
    // RRE-U-04: 租期变更-作废超出部分
    // ========================================================================
    @Test
    @DisplayName("RRE-U-04: 租期变更-作废超出新合同期的应收")
    void execute_termChange_shouldVoidPlansAfterNewEnd() {
        // 准备：TERM 变更，newContractEnd=2026-06-30
        String impactJson = """
                {"changeFields":{"newContractEnd":"2026-06-30"}}""";
        insertChange(92053, 91003, 92003, "2026-04-01", impactJson);
        insertChangeType(92053, 92053, "TERM");

        // 插入 4~12 月应收（4~6月在合同期内，7~12月超出）
        for (int m = 4; m <= 12; m++) {
            long planId = 92060 + m;
            String start = String.format("2026-%02d-01", m);
            String end = String.format("2026-%02d-28", m);
            insertPlan(planId, 92003, start, end, new BigDecimal("15000"), 0, 1);
        }

        OprContractChange change = changeMapper.selectById(92053L);
        engine.execute(change);

        // 验证 4~6 月应收保持待收（billingStart <= 2026-06-30）
        for (int m = 4; m <= 6; m++) {
            OprReceivablePlan plan = receivablePlanMapper.selectById(92060L + m);
            assertNotNull(plan);
            assertEquals(0, plan.getStatus(),
                    m + "月应收(ID=" + (92060 + m) + ")应保持待收(status=0)");
        }

        // 验证 7~12 月应收全部作废（billingStart > 2026-06-30）
        for (int m = 7; m <= 12; m++) {
            OprReceivablePlan plan = receivablePlanMapper.selectById(92060L + m);
            assertNotNull(plan, "应收计划 " + (92060 + m) + " 应存在");
            assertEquals(3, plan.getStatus(),
                    m + "月应收(ID=" + (92060 + m) + ")应被作废(status=3)");
        }

        // 验证台账的合同结束日期被更新
        OprContractLedger ledger = ledgerMapper.selectById(92003L);
        assertEquals(LocalDate.of(2026, 6, 30), ledger.getContractEnd(),
                "台账合同结束日期应更新为 2026-06-30");
    }

    // ========================================================================
    // RRE-U-05: 品牌变更-不影响应收金额
    // ========================================================================
    @Test
    @DisplayName("RRE-U-05: 品牌变更-不影响应收金额")
    void execute_brandChange_shouldNotAffectAmount() {
        // 准备：BRAND 变更，无 impact_summary
        insertChange(92054, 91003, 92003, "2026-04-01", null);
        insertChangeType(92054, 92054, "BRAND");
        insertPlan(92054, 92003, "2026-04-01", "2026-04-30",
                new BigDecimal("15000"), 0, 1);

        OprContractChange change = changeMapper.selectById(92054L);
        engine.execute(change);

        // 验证：品牌变更后应收金额和版本均不变
        OprReceivablePlan plan = receivablePlanMapper.selectById(92054L);
        assertNotNull(plan);
        assertEquals(0, new BigDecimal("15000").compareTo(plan.getAmount()),
                "品牌变更不应影响应收金额");
        assertEquals(1, plan.getVersion(), "品牌变更不应递增 version");
        assertEquals(1, plan.getSourceType(), "品牌变更不应修改 sourceType");
    }

    // ========================================================================
    // RRE-U-06: 预览-返回影响笔数和差异
    // ========================================================================
    @Test
    @DisplayName("RRE-U-06: 预览-返回影响笔数和差异")
    void preview_rentChange_shouldReturnImpactSummary() {
        // 准备：effectiveDate=2026-04-01，新插入2笔待收应收各15000
        String impactJson = """
                {"changeFields":{"newRentAmount":"18000","oldRentAmount":"15000"}}""";
        insertChange(92055, 91003, 92003, "2026-04-01", impactJson);
        insertChangeType(92055, 92055, "RENT");
        insertPlan(92055, 92003, "2026-04-01", "2026-04-30",
                new BigDecimal("15000"), 0, 1);
        insertPlan(92056, 92003, "2026-05-01", "2026-05-31",
                new BigDecimal("15000"), 0, 1);

        OprContractChange change = changeMapper.selectById(92055L);
        ChangeImpactVO vo = engine.preview(change);

        assertNotNull(vo, "预览结果不应为空");
        // 受影响笔数：仅 billingStart >= 2026-04-01 的待收应收
        assertTrue(vo.getAffectedPlanCount() >= 2,
                "受影响应收计划应至少2笔，实际: " + vo.getAffectedPlanCount());
        // 差异：每笔增加 3000，2笔共增加 6000
        assertTrue(vo.getAmountDiff().compareTo(BigDecimal.ZERO) > 0,
                "租金上调，差额应为正数");
        assertNotNull(vo.getImpactDesc(), "影响说明文本不应为空");
        assertTrue(vo.getImpactDesc().contains("租金"),
                "影响说明应包含变更类型描述");
    }

    // ========================================================================
    // RRE-U-07: 变更前保存合同快照（snapshotType=1）
    // ========================================================================
    @Test
    @DisplayName("RRE-U-07: 变更前保存合同快照")
    void execute_shouldSaveContractSnapshot() {
        // 准备：RENT 变更（任意类型均会触发快照保存）
        String impactJson = """
                {"changeFields":{"newRentAmount":"18000","oldRentAmount":"15000"}}""";
        insertChange(92057, 91003, 92003, "2026-04-01", impactJson);
        insertChangeType(92057, 92057, "RENT");
        insertPlan(92057, 92003, "2026-04-01", "2026-04-30",
                new BigDecimal("15000"), 0, 1);

        OprContractChange change = changeMapper.selectById(92057L);
        engine.execute(change);

        // 验证：合同主表快照存在（snapshotType=1）
        List<OprContractChangeSnapshot> snapshots = snapshotMapper.selectList(
                new LambdaQueryWrapper<OprContractChangeSnapshot>()
                        .eq(OprContractChangeSnapshot::getChangeId, 92057L)
                        .eq(OprContractChangeSnapshot::getSnapshotType, 1));
        assertFalse(snapshots.isEmpty(), "应保存合同主表快照(snapshotType=1)");

        OprContractChangeSnapshot snapshot = snapshots.get(0);
        assertNotNull(snapshot.getSnapshotData(), "快照数据不应为空");
        assertTrue(snapshot.getSnapshotData().contains("91003")
                        || snapshot.getSnapshotData().contains("contract"),
                "快照数据应包含合同相关信息");
    }

    // ========================================================================
    // RRE-U-08: 变更前保存应收快照（snapshotType=3）
    // ========================================================================
    @Test
    @DisplayName("RRE-U-08: 变更前保存应收快照")
    void execute_shouldSaveReceivableSnapshot() {
        // 准备：RENT 变更
        String impactJson = """
                {"changeFields":{"newRentAmount":"18000","oldRentAmount":"15000"}}""";
        insertChange(92058, 91003, 92003, "2026-04-01", impactJson);
        insertChangeType(92058, 92058, "RENT");
        insertPlan(92058, 92003, "2026-04-01", "2026-04-30",
                new BigDecimal("15000"), 0, 1);

        OprContractChange change = changeMapper.selectById(92058L);
        engine.execute(change);

        // 验证：应收计划快照存在（snapshotType=3）
        List<OprContractChangeSnapshot> snapshots = snapshotMapper.selectList(
                new LambdaQueryWrapper<OprContractChangeSnapshot>()
                        .eq(OprContractChangeSnapshot::getChangeId, 92058L)
                        .eq(OprContractChangeSnapshot::getSnapshotType, 3));
        assertFalse(snapshots.isEmpty(), "应保存应收计划快照(snapshotType=3)");

        OprContractChangeSnapshot snapshot = snapshots.get(0);
        assertNotNull(snapshot.getSnapshotData(), "快照数据不应为空");
        // 快照保存了 ledger 92003 下所有活跃应收（包括永久数据 92004/92005 和测试数据 92058）
        assertTrue(snapshot.getSnapshotData().length() > 10,
                "快照 JSON 数据应包含应收计划详情");
    }
}
