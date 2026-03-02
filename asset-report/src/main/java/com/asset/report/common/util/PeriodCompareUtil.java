package com.asset.report.common.util;

import com.asset.report.common.param.ReportQueryParam.TimeUnit;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.IsoFields;

/**
 * 同比/环比计算工具类
 * <p>
 * 支持以下对比模式：
 * <ul>
 *   <li>YoY（Year-on-Year，同比）：与去年同期对比</li>
 *   <li>MoM（Month-on-Month，环比）：与上一周期对比</li>
 * </ul>
 * 时间维度：日 / 周（ISO 8601）/ 月 / 年
 * </p>
 */
@UtilityClass
public class PeriodCompareUtil {

    private static final int SCALE = 2;

    // ==================== 日期偏移计算 ====================

    /**
     * 计算同比（YoY）的上一年同期起始日期
     *
     * @param date     当前日期
     * @param timeUnit 时间维度
     * @return 上一年同期的对应日期
     */
    public LocalDate previousYearPeriod(LocalDate date, TimeUnit timeUnit) {
        return switch (timeUnit) {
            case DAY   -> date.minusYears(1);
            case WEEK  -> date.minusWeeks(52); // ISO 8601: 去年同周（近似 52 周）
            case MONTH -> date.minusYears(1).withDayOfMonth(1);
            case YEAR  -> date.minusYears(1).withDayOfYear(1);
        };
    }

    /**
     * 计算环比（MoM）的上一周期起始日期
     *
     * @param date     当前日期
     * @param timeUnit 时间维度
     * @return 上一周期的对应日期
     */
    public LocalDate previousPeriod(LocalDate date, TimeUnit timeUnit) {
        return switch (timeUnit) {
            case DAY   -> date.minusDays(1);
            case WEEK  -> date.minusWeeks(1);
            case MONTH -> date.minusMonths(1).withDayOfMonth(1);
            case YEAR  -> date.minusYears(1).withDayOfYear(1);
        };
    }

    /**
     * 计算同比（YoY）的上一年同期月份（YYYY-MM 格式）
     *
     * @param statMonth 当前月份（YYYY-MM）
     * @return 上一年同月（如 "2026-03" → "2025-03"）
     */
    public String previousYearMonth(String statMonth) {
        YearMonth ym = YearMonth.parse(statMonth);
        return ym.minusYears(1).toString();
    }

    /**
     * 计算环比（MoM）的上一个月（YYYY-MM 格式）
     *
     * @param statMonth 当前月份（YYYY-MM）
     * @return 上一月（如 "2026-03" → "2026-02"）
     */
    public String previousMonth(String statMonth) {
        YearMonth ym = YearMonth.parse(statMonth);
        return ym.minusMonths(1).toString();
    }

    /**
     * 计算同比上一年的 ISO 周（YYYY-Www 格式）
     *
     * @param year ISO 周年
     * @param week ISO 周数
     * @return [year-1, week] 对应的日期（近似取同周数）
     */
    public LocalDate previousYearWeekStart(int year, int week) {
        return LocalDate.now()
                .withYear(year - 1)
                .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, week)
                .with(java.time.DayOfWeek.MONDAY);
    }

    // ==================== 增长率计算 ====================

    /**
     * 计算增长率（%）= (current - previous) / |previous| * 100
     * <p>
     * 特殊情况：
     * <ul>
     *   <li>previous = 0, current > 0：返回 {@code null}（无穷大）</li>
     *   <li>previous = 0, current = 0：返回 0.00%</li>
     *   <li>previous = 0, current < 0：返回 {@code null}（无法计算）</li>
     * </ul>
     * </p>
     *
     * @param current  本期值
     * @param previous 上期值
     * @return 增长率百分比（两位小数），无法计算时返回 null
     */
    public BigDecimal calcGrowthRate(BigDecimal current, BigDecimal previous) {
        if (current == null) current = BigDecimal.ZERO;
        if (previous == null) previous = BigDecimal.ZERO;
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : null;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous.abs(), SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 计算同比增长率（Year-on-Year）
     *
     * @param currentValue  本期值
     * @param previousValue 去年同期值
     * @return 同比增长率（%），无法计算时返回 null
     */
    public BigDecimal calcYoY(BigDecimal currentValue, BigDecimal previousValue) {
        return calcGrowthRate(currentValue, previousValue);
    }

    /**
     * 计算环比增长率（Month-on-Month / Period-on-Period）
     *
     * @param currentValue  本期值
     * @param previousValue 上期值
     * @return 环比增长率（%），无法计算时返回 null
     */
    public BigDecimal calcMoM(BigDecimal currentValue, BigDecimal previousValue) {
        return calcGrowthRate(currentValue, previousValue);
    }

    /**
     * 安全增长率：无法计算时（被除数为0）返回 BigDecimal.ZERO 而非 null
     *
     * @param current  本期值
     * @param previous 上期值
     * @return 增长率（%），始终非 null
     */
    public BigDecimal calcGrowthRateSafe(BigDecimal current, BigDecimal previous) {
        BigDecimal rate = calcGrowthRate(current, previous);
        return rate != null ? rate : BigDecimal.ZERO;
    }

    /**
     * 计算占比（%）= value / total * 100，安全防止除零
     *
     * @param value 分子
     * @param total 分母
     * @return 占比（两位小数），total 为 0 时返回 BigDecimal.ZERO
     */
    public BigDecimal calcPercentage(BigDecimal value, BigDecimal total) {
        if (value == null) value = BigDecimal.ZERO;
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return value.multiply(BigDecimal.valueOf(100))
                .divide(total, SCALE, RoundingMode.HALF_UP);
    }
}
