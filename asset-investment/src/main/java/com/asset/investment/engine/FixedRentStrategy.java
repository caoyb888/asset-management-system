package com.asset.investment.engine;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 固定租金计算策略（charge_type=1）
 * 公式：单价 × 面积 × 月数
 * 月数由 stageStart ~ stageEnd 决定，不足整月按当月实际天数比例折算
 */
@Component("fixedRentStrategy")
public class FixedRentStrategy implements RentCalculateStrategy {

    @Override
    public BigDecimal calculate(RentCalculateContext context) {
        BigDecimal months = calcMonths(context.getStageStart(), context.getStageEnd());
        return context.getUnitPrice()
                .multiply(context.getArea())
                .multiply(months)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算月数（精确到小数，处理闰年/大小月边界）
     * 算法：
     *   1. 计算完整整月数（使用 ChronoUnit.MONTHS，自动处理大小月）
     *   2. 计算余下不足整月的天数，按余下月的实际天数比例折算
     *
     * @param start 开始日期（含）
     * @param end   结束日期（含）
     */
    public static BigDecimal calcMonths(LocalDate start, LocalDate end) {
        // plusDays(1) 将"含尾"转换为"半开区间"，方便 ChronoUnit.MONTHS 精确计算
        LocalDate exclusiveEnd = end.plusDays(1);
        long fullMonths = ChronoUnit.MONTHS.between(start, exclusiveEnd);
        LocalDate afterFull = start.plusMonths(fullMonths);
        long remainDays = ChronoUnit.DAYS.between(afterFull, exclusiveEnd);

        if (remainDays == 0) {
            return BigDecimal.valueOf(fullMonths);
        }
        // 不足整月部分：余下天数 / 余下那个自然月的实际天数
        int daysInPartialMonth = afterFull.lengthOfMonth();
        return BigDecimal.valueOf(fullMonths)
                .add(BigDecimal.valueOf(remainDays)
                        .divide(BigDecimal.valueOf(daysInPartialMonth), 10, RoundingMode.HALF_UP));
    }
}
