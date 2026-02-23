package com.asset.investment.engine;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 阶梯提成计算策略（charge_type=3）
 * 分阶段计算，各阶段金额求和。
 *
 * <p>两种调用模式：
 * <ol>
 *   <li><b>多阶段模式</b>：{@code context.formulaParams} 中包含 {@code "stages"} 数组，
 *       按各阶段的 commission_rate / min_commission_amount / revenue 分别计算后求和。</li>
 *   <li><b>单阶段模式</b>：无 stages 数组时，直接使用 context 字段（与固定提成逻辑相同），
 *       适用于服务层按单个阶段循环调用的场景。</li>
 * </ol>
 *
 * <p>stages 数组元素格式：
 * <pre>{@code
 * {
 *   "commission_rate": 5.0,
 *   "min_commission_amount": 10000.00,
 *   "revenue": 200000.00        // 可选，无则使用 context.revenue
 * }
 * }</pre>
 */
@Component("stepCommissionStrategy")
public class StepCommissionStrategy implements RentCalculateStrategy {

    @Override
    public BigDecimal calculate(RentCalculateContext context) {
        JsonNode params = context.getFormulaParams();
        if (params != null && params.has("stages")) {
            return calcByStages(params.get("stages"), context.getRevenue());
        }
        // 单阶段降级：与固定提成计算完全相同
        return FixedCommissionStrategy.calcCommission(
                context.getRevenue(),
                context.getCommissionRate(),
                context.getMinCommissionAmount()
        );
    }

    /**
     * 遍历 stages 数组，逐阶段计算后求和
     */
    private BigDecimal calcByStages(JsonNode stages, BigDecimal defaultRevenue) {
        BigDecimal total = BigDecimal.ZERO;
        for (JsonNode stage : stages) {
            BigDecimal rate = decimalFrom(stage, "commission_rate", BigDecimal.ZERO);
            BigDecimal minAmt = decimalFrom(stage, "min_commission_amount", BigDecimal.ZERO);
            // 阶段营业额：优先使用 stage 自带的 revenue，否则回退到 context.revenue
            BigDecimal revenue = stage.has("revenue")
                    ? decimalFrom(stage, "revenue", BigDecimal.ZERO)
                    : (defaultRevenue != null ? defaultRevenue : BigDecimal.ZERO);
            total = total.add(FixedCommissionStrategy.calcCommission(revenue, rate, minAmt));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal decimalFrom(JsonNode node, String field, BigDecimal defaultVal) {
        return node.has(field) ? new BigDecimal(node.get(field).asText()) : defaultVal;
    }
}
