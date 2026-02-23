package com.asset.investment.engine;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 两者取高计算策略（charge_type=4）
 * 公式：max(固定租金, 提成租金)
 * 固定租金 = 单价 × 面积 × 月数
 * 提成租金 = max(营业额 × 提成率%, 最低提成金额)
 */
@Component("higherOfStrategy")
public class HigherOfStrategy implements RentCalculateStrategy {

    @Override
    public BigDecimal calculate(RentCalculateContext context) {
        // 固定租金部分
        BigDecimal months = FixedRentStrategy.calcMonths(
                context.getStageStart(), context.getStageEnd());
        BigDecimal unitPrice = context.getUnitPrice() != null ? context.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal area = context.getArea() != null ? context.getArea() : BigDecimal.ZERO;
        BigDecimal fixedAmt = unitPrice.multiply(area).multiply(months);

        // 提成租金部分
        BigDecimal commissionAmt = FixedCommissionStrategy.calcCommission(
                context.getRevenue(),
                context.getCommissionRate(),
                context.getMinCommissionAmount()
        );

        return fixedAmt.max(commissionAmt).setScale(2, RoundingMode.HALF_UP);
    }
}
