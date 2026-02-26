package com.asset.operation.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 解约清算引擎（策略模式）
 * 支持三种解约类型：到期自然终止 / 提前解约 / 重签解约
 *
 * TODO 阶段五（第5周后半）完整实现：
 *   - NaturalTerminationStrategy：到期解约，清算剩余未收费用
 *   - EarlyTerminationStrategy：提前解约，计算违约金（按日折算当期租金），清算未收费用
 *   - RenewalTerminationStrategy：重签解约，关联新合同ID，清空原合同剩余应收
 *   - 通用：生成 opr_termination_settlement 明细（正数应收/负数应退）
 *   - execute()：事务性执行（opr_contract_ledger + inv_lease_contract + biz_shop + opr_receivable_plan 原子更新）
 */
@Slf4j
@Component
public class TerminationSettlementEngine {

    /**
     * 计算解约清算金额（桩，阶段五实现）
     * @param terminationId 解约单ID
     */
    public void calculateSettlement(Long terminationId) {
        // TODO 阶段五实现
        log.info("[解约清算引擎] terminationId={} - 待实现", terminationId);
        throw new UnsupportedOperationException("TerminationSettlementEngine 待阶段五实现");
    }
}
