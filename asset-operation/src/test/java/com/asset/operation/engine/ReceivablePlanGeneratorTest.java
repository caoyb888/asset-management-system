package com.asset.operation.engine;

import com.asset.common.exception.BizException;
import com.asset.operation.OperationApplication;
import com.asset.operation.ledger.entity.OprReceivablePlan;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 应收计划生成引擎单元测试（RPG-U）
 * 框架：@SpringBootTest（需真实 JdbcTemplate 查询 inv_* 表）
 *
 * 测试数据依赖（永久数据，不随 @Transactional 回滚）：
 * - 台账 92001：contractId=91003, receivableStatus=0（待生成）
 * - 台账 92002：contractId=91003, receivableStatus=1（已生成）
 * - 合同 91003：12 个月 billing（91001~91012），每月 15000
 * - 费项 91001：item_name='测试租金'
 * - 合同商铺 91002：contractId=91003 → shopId=90001
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = OperationApplication.class
)
@ActiveProfiles("test")
@Transactional
@DisplayName("应收计划生成引擎（RPG-U）")
class ReceivablePlanGeneratorTest {

    @Autowired
    private ReceivablePlanGenerator generator;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private com.asset.operation.ledger.mapper.OprReceivablePlanMapper receivablePlanMapper;

    // ========================================================================
    // RPG-U-01: 从合同账期生成应收计划-12个月
    // ========================================================================
    @Test
    @DisplayName("RPG-U-01: 从合同账期生成应收计划-12个月")
    void generate_normalContract_shouldCreate12Plans() {
        // 台账92001：contractId=91003, receivableStatus=0（待生成）
        int count = generator.generate(92001L);

        // 预期：返回12条
        assertEquals(12, count, "应生成12条应收计划");

        // 验证DB中实际插入的记录
        LambdaQueryWrapper<OprReceivablePlan> qw = new LambdaQueryWrapper<>();
        qw.eq(OprReceivablePlan::getLedgerId, 92001L)
          .eq(OprReceivablePlan::getIsDeleted, 0);
        List<OprReceivablePlan> plans = receivablePlanMapper.selectList(qw);

        assertEquals(12, plans.size(), "DB中应有12条应收计划");

        // 验证每条记录的关键字段
        for (OprReceivablePlan plan : plans) {
            assertEquals(91003L, plan.getContractId(), "contractId 应为 91003");
            assertEquals(92001L, plan.getLedgerId(), "ledgerId 应为 92001");
            assertEquals(1, plan.getSourceType(), "sourceType 应为 1（合同生成）");
            assertEquals(1, plan.getVersion(), "version 应为 1");
            assertEquals(0, plan.getPushStatus(), "pushStatus 应为 0（未推送）");
            assertEquals(0, plan.getStatus(), "status 应为 0（待收）");
            assertEquals(0, plan.getReceivedAmount().compareTo(BigDecimal.ZERO),
                    "receivedAmount 应为 0");
        }
    }

    // ========================================================================
    // RPG-U-02: 跳过零金额账期（免租期）
    // ========================================================================
    @Test
    @DisplayName("RPG-U-02: 跳过零金额账期（免租期）")
    void generate_withZeroAmountBilling_shouldSkipFreeMonths() {
        // 将合同91003的1月和2月billing金额设为0（模拟免租期）
        jdbcTemplate.update(
                "UPDATE inv_lease_contract_billing SET amount = 0 WHERE id IN (91001, 91002)");

        int count = generator.generate(92001L);

        // 12个月中2个免租 → 应返回10
        assertEquals(10, count, "跳过2个免租月，应生成10条");

        // 验证DB中无0金额记录
        LambdaQueryWrapper<OprReceivablePlan> qw = new LambdaQueryWrapper<>();
        qw.eq(OprReceivablePlan::getLedgerId, 92001L)
          .eq(OprReceivablePlan::getIsDeleted, 0);
        List<OprReceivablePlan> plans = receivablePlanMapper.selectList(qw);
        assertEquals(10, plans.size());

        for (OprReceivablePlan plan : plans) {
            assertTrue(plan.getAmount().compareTo(BigDecimal.ZERO) > 0,
                    "不应包含金额为0的应收计划");
        }
    }

    // ========================================================================
    // RPG-U-03: 费项名称正确映射
    // ========================================================================
    @Test
    @DisplayName("RPG-U-03: 费项名称正确映射")
    void generate_feeItemMapping_shouldSetFeeNameFromCfgFeeItem() {
        // 先查出 cfg_fee_item 91001 的 item_name
        String expectedFeeName = jdbcTemplate.queryForObject(
                "SELECT item_name FROM cfg_fee_item WHERE id = 91001", String.class);
        assertNotNull(expectedFeeName, "cfg_fee_item 91001 应存在");

        generator.generate(92001L);

        // 验证生成的应收计划中 feeName 来自 cfg_fee_item
        LambdaQueryWrapper<OprReceivablePlan> qw = new LambdaQueryWrapper<>();
        qw.eq(OprReceivablePlan::getLedgerId, 92001L)
          .eq(OprReceivablePlan::getFeeItemId, 91001L)
          .eq(OprReceivablePlan::getIsDeleted, 0);
        List<OprReceivablePlan> plans = receivablePlanMapper.selectList(qw);

        assertFalse(plans.isEmpty(), "应有 feeItemId=91001 的应收计划");
        for (OprReceivablePlan plan : plans) {
            assertEquals(expectedFeeName, plan.getFeeName(),
                    "feeName 应从 cfg_fee_item 映射而来");
        }
    }

    // ========================================================================
    // RPG-U-04: 商铺ID正确回填
    // ========================================================================
    @Test
    @DisplayName("RPG-U-04: 商铺ID正确回填")
    void generate_shopMapping_shouldSetShopIdFromContractShop() {
        generator.generate(92001L);

        // inv_lease_contract_shop 中 contractId=91003 的第一个 shopId=90001
        LambdaQueryWrapper<OprReceivablePlan> qw = new LambdaQueryWrapper<>();
        qw.eq(OprReceivablePlan::getLedgerId, 92001L)
          .eq(OprReceivablePlan::getIsDeleted, 0);
        List<OprReceivablePlan> plans = receivablePlanMapper.selectList(qw);

        assertFalse(plans.isEmpty());
        for (OprReceivablePlan plan : plans) {
            assertEquals(90001L, plan.getShopId(),
                    "shopId 应从 inv_lease_contract_shop 回填为 90001");
        }
    }

    // ========================================================================
    // RPG-U-05: 台账不存在抛异常
    // ========================================================================
    @Test
    @DisplayName("RPG-U-05: 台账不存在抛异常")
    void generate_ledgerNotExist_shouldThrowBizException() {
        BizException ex = assertThrows(BizException.class,
                () -> generator.generate(999999L));

        assertTrue(ex.getMessage().contains("台账不存在"),
                "异常消息应包含'台账不存在'，实际: " + ex.getMessage());
    }

    // ========================================================================
    // RPG-U-06: 重复生成抛异常
    // ========================================================================
    @Test
    @DisplayName("RPG-U-06: 重复生成抛异常（receivableStatus=1）")
    void generate_alreadyGenerated_shouldThrowBizException() {
        // 台账92002：receivableStatus=1（已生成应收）
        BizException ex = assertThrows(BizException.class,
                () -> generator.generate(92002L));

        assertTrue(ex.getMessage().contains("已生成"),
                "异常消息应包含'已生成'，实际: " + ex.getMessage());
    }

    // ========================================================================
    // RPG-U-07: 合同无账期数据抛异常
    // ========================================================================
    @Test
    @DisplayName("RPG-U-07: 合同无账期数据抛异常")
    void generate_noBillingData_shouldThrowBizException() {
        // 创建一个临时台账，关联一个没有 billing 数据的合同ID
        jdbcTemplate.update("""
                INSERT INTO opr_contract_ledger
                    (id, ledger_code, contract_id, project_id, merchant_id, brand_id,
                     contract_type, contract_start, contract_end,
                     double_sign_status, receivable_status, audit_status, status,
                     is_deleted, created_by, created_at, updated_by, updated_at)
                VALUES
                    (92099, 'TZ-RPG-TEST-07', 99999, 90001, 90002, 90001,
                     1, '2026-01-01', '2026-12-31',
                     0, 0, 0, 0,
                     0, '90001', NOW(), '90001', NOW())
                """);

        BizException ex = assertThrows(BizException.class,
                () -> generator.generate(92099L));

        assertTrue(ex.getMessage().contains("账期") || ex.getMessage().contains("为空"),
                "异常消息应提示账期数据为空，实际: " + ex.getMessage());
    }
}
