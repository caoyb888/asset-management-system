package com.asset.operation.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 浮动租金计算引擎（策略模式）
 * 支持三种计费方式：固定提成 / 阶梯累进提成 / 两者取高
 * 阶梯明细写入 opr_floating_rent_tier，支持审计回溯
 *
 * TODO 阶段三（第4周）完整实现：
 *   - FixedCommissionStrategy：固定提成率计算
 *   - TieredCommissionStrategy：阶梯累进，每档明细写 opr_floating_rent_tier
 *   - HigherOfStrategy：取固定租金与提成两者较高值
 *   - 前置校验：月度填报完整性检查（不完整拒绝计算）
 *   - 计算结果：写 opr_floating_rent + 生成 opr_receivable_plan（source_type=3）
 */
@Slf4j
@Component
public class FloatingRentCalculator {

    /**
     * 计算指定合同指定月度的浮动租金（桩，阶段三实现）
     * @param contractId 合同ID
     * @param calcMonth  计算月份（YYYY-MM）
     */
    public void calculate(Long contractId, String calcMonth) {
        // TODO 阶段三实现
        log.info("[浮动租金计算] contractId={}, calcMonth={} - 待实现", contractId, calcMonth);
        throw new UnsupportedOperationException("FloatingRentCalculator 待阶段三实现");
    }
}
