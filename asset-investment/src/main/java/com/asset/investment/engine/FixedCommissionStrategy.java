package com.asset.investment.engine;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 固定提成计算策略（charge_type=2）
 * 公式：max(营业额 × 提成率%, 最低提成金额)
 * 当提成金额低于保底金额时，按保底金额计收
 */
@Component("fixedCommissionStrategy")
public class FixedCommissionStrategy implements RentCalculateStrategy {

    @Override
    public BigDecimal calculate(RentCalculateContext context) {
        return calcCommission(
                context.getRevenue(),
                context.getCommissionRate(),
                context.getMinCommissionAmount()
        );
    }

    /**
     * 单阶段提成计算（可被 StepCommissionStrategy / HigherOfStrategy 复用）
     *
     * @param revenue             营业额
     * @param commissionRate      提成比例（%）
     * @param minCommissionAmount 最低保底金额（null 视为 0）
     */
    static BigDecimal calcCommission(BigDecimal revenue,
                                     BigDecimal commissionRate,
                                     BigDecimal minCommissionAmount) {
        BigDecimal rev = revenue != null ? revenue : BigDecimal.ZERO;
        BigDecimal rate = commissionRate != null ? commissionRate : BigDecimal.ZERO;
        BigDecimal commission = rev.multiply(rate)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal minAmt = minCommissionAmount != null ? minCommissionAmount : BigDecimal.ZERO;
        return commission.max(minAmt).setScale(2, RoundingMode.HALF_UP);
    }
}
