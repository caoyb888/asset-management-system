package com.asset.operation.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 应收计划生成器
 * 根据合同费项、计租周期、账期规则自动生成 opr_receivable_plan 明细
 * 支持月付/季付/年付，处理跨月/跨年/闰年边界
 *
 * TODO 阶段一（第1-2周）完整实现：
 *   - 参照 investment 模块 BillingGenerator 的账期生成逻辑
 *   - 支持 source_type=1（合同生成），首账期特殊处理
 *   - 所有金额计算使用 BigDecimal，禁止 double
 */
@Slf4j
@Component
public class ReceivablePlanGenerator {

    /**
     * 根据合同台账ID生成应收计划列表（桩，阶段一实现）
     * @param ledgerId 合同台账ID
     */
    public void generate(Long ledgerId) {
        // TODO 阶段一实现
        log.info("[应收计划生成器] ledgerId={} - 待实现", ledgerId);
        throw new UnsupportedOperationException("ReceivablePlanGenerator 待阶段一实现");
    }
}
