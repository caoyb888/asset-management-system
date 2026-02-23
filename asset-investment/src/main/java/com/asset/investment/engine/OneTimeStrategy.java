package com.asset.investment.engine;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 一次性收费计算策略（charge_type=5）
 * 优先从 formulaParams.amount 取一次性金额；
 * 若未提供则退化为 单价 × 面积（用于保证金等按面积收取的一次性费用）。
 *
 * <p>formulaParams 示例：{@code {"amount": 50000.00}}
 */
@Component("oneTimeStrategy")
public class OneTimeStrategy implements RentCalculateStrategy {

    @Override
    public BigDecimal calculate(RentCalculateContext context) {
        JsonNode params = context.getFormulaParams();
        if (params != null && params.has("amount")) {
            return new BigDecimal(params.get("amount").asText())
                    .setScale(2, RoundingMode.HALF_UP);
        }
        // 退化模式：单价 × 面积（适用于按面积比例收取的一次性费用）
        BigDecimal unitPrice = context.getUnitPrice() != null ? context.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal area = context.getArea() != null ? context.getArea() : BigDecimal.ZERO;
        return unitPrice.multiply(area).setScale(2, RoundingMode.HALF_UP);
    }
}
