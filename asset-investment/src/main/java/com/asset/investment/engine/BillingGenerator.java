package com.asset.investment.engine;

import com.asset.investment.common.enums.BillingMode;
import com.asset.investment.common.enums.PaymentCycle;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 账期生成器（任务3.2）
 * 根据合同起止日期、支付周期、账期模式生成完整账期列表。
 *
 * <p>生成规则：
 * <ol>
 *   <li>账期长度由 paymentCycle 决定（月付=1月，季付=3月，年付=12月等）</li>
 *   <li>最后一个账期可能因合同结束日而被截断（产生不足整周期的尾账期）</li>
 *   <li>首账期 billingType=1，后续均为 2</li>
 *   <li>应收日期（dueDate）由 billingMode 决定：预付/当期=账期开始，后付=账期结束</li>
 * </ol>
 *
 * <p>日期边界处理：使用 {@code LocalDate.plusMonths()} 自动处理大小月/闰年（如 Jan31+1M=Feb28/29）
 */
@Component
public class BillingGenerator {

    /**
     * 生成账期列表
     *
     * @param start 合同开始日期（含）
     * @param end   合同结束日期（含）
     * @param cycle 支付周期
     * @param mode  账期模式
     * @return 按时间升序排列的账期列表，首账期 billingType=1
     */
    public List<BillingPeriod> generate(LocalDate start, LocalDate end,
                                        PaymentCycle cycle, BillingMode mode) {
        List<BillingPeriod> result = new ArrayList<>();
        LocalDate current = start;
        boolean isFirst = true;
        int cycleMonths = cycle.getMonths();

        while (!current.isAfter(end)) {
            // 本账期结束 = 下一周期开始前一天；若超出合同结束日则截断
            LocalDate nextStart = current.plusMonths(cycleMonths);
            LocalDate periodEnd = nextStart.minusDays(1);
            if (periodEnd.isAfter(end)) {
                periodEnd = end;
            }

            LocalDate dueDate = calcDueDate(current, periodEnd, mode);
            result.add(BillingPeriod.builder()
                    .billingStart(current)
                    .billingEnd(periodEnd)
                    .dueDate(dueDate)
                    .billingType(isFirst ? 1 : 2)
                    .build());

            current = periodEnd.plusDays(1);
            isFirst = false;
        }
        return result;
    }

    /**
     * 计算应收日期
     * 预付/当期：账期开始日（期初一次性缴清）
     * 后付：账期结束日（期末结算）
     */
    private LocalDate calcDueDate(LocalDate periodStart, LocalDate periodEnd, BillingMode mode) {
        return switch (mode) {
            case PREPAY, CURRENT -> periodStart;
            case POSTPAY -> periodEnd;
        };
    }
}
