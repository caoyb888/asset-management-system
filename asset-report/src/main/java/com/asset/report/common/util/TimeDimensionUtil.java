package com.asset.report.common.util;

import com.asset.report.common.param.ReportQueryParam.TimeUnit;
import lombok.experimental.UtilityClass;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;

/**
 * 时间维度转换工具类
 * <p>
 * 支持 ISO 8601 周计算、日期格式化、时间范围生成、时间维度分组等操作。
 * ISO 8601 周：一年第一周必须包含该年第一个星期四，周一为第一天。
 * </p>
 */
@UtilityClass
public class TimeDimensionUtil {

    /** 标准日期格式 */
    public static final DateTimeFormatter DATE_FMT   = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /** 月份格式 */
    public static final DateTimeFormatter MONTH_FMT  = DateTimeFormatter.ofPattern("yyyy-MM");
    /** ISO 周格式（如 2026-W05） */
    public static final DateTimeFormatter WEEK_FMT   = DateTimeFormatter.ofPattern("yyyy-'W'ww");
    /** 年份格式 */
    public static final DateTimeFormatter YEAR_FMT   = DateTimeFormatter.ofPattern("yyyy");

    // ==================== ISO 8601 周计算 ====================

    /**
     * 获取日期所在的 ISO 8601 周数（1-53）
     *
     * @param date 日期
     * @return ISO 周数
     */
    public int isoWeekOfYear(LocalDate date) {
        return date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }

    /**
     * 获取日期所在的 ISO 周年（与日历年可能不同，如 2021-01-01 属于 2020W53）
     *
     * @param date 日期
     * @return ISO 周年
     */
    public int isoWeekYear(LocalDate date) {
        return date.get(IsoFields.WEEK_BASED_YEAR);
    }

    /**
     * 获取指定 ISO 周年/周数 对应的周一日期
     *
     * @param isoWeekYear ISO 周年
     * @param isoWeek     ISO 周数
     * @return 该周的周一
     */
    public LocalDate isoWeekStart(int isoWeekYear, int isoWeek) {
        return LocalDate.now()
                .with(IsoFields.WEEK_BASED_YEAR, isoWeekYear)
                .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, isoWeek)
                .with(DayOfWeek.MONDAY);
    }

    /**
     * 获取指定 ISO 周年/周数 对应的周日日期
     *
     * @param isoWeekYear ISO 周年
     * @param isoWeek     ISO 周数
     * @return 该周的周日
     */
    public LocalDate isoWeekEnd(int isoWeekYear, int isoWeek) {
        return isoWeekStart(isoWeekYear, isoWeek).plusDays(6);
    }

    /**
     * 获取日期所在 ISO 周的周一
     */
    public LocalDate weekStart(LocalDate date) {
        return date.with(DayOfWeek.MONDAY);
    }

    /**
     * 获取日期所在 ISO 周的周日
     */
    public LocalDate weekEnd(LocalDate date) {
        return date.with(DayOfWeek.SUNDAY);
    }

    // ==================== 月/年范围计算 ====================

    /**
     * 获取指定月份的第一天
     *
     * @param statMonth YYYY-MM 格式
     * @return 该月第一天
     */
    public LocalDate monthStart(String statMonth) {
        return YearMonth.parse(statMonth).atDay(1);
    }

    /**
     * 获取指定月份的最后一天
     *
     * @param statMonth YYYY-MM 格式
     * @return 该月最后一天
     */
    public LocalDate monthEnd(String statMonth) {
        return YearMonth.parse(statMonth).atEndOfMonth();
    }

    /**
     * 获取日期所在月份的第一天和最后一天
     *
     * @return [monthStart, monthEnd]
     */
    public LocalDate[] monthRange(LocalDate date) {
        LocalDate start = date.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate end   = date.with(TemporalAdjusters.lastDayOfMonth());
        return new LocalDate[]{start, end};
    }

    /**
     * 获取日期所在年份的第一天和最后一天
     *
     * @return [yearStart, yearEnd]
     */
    public LocalDate[] yearRange(LocalDate date) {
        LocalDate start = date.with(TemporalAdjusters.firstDayOfYear());
        LocalDate end   = date.with(TemporalAdjusters.lastDayOfYear());
        return new LocalDate[]{start, end};
    }

    // ==================== 时间维度 Key 格式化 ====================

    /**
     * 将日期格式化为时间维度 Key，用于图表横轴标签
     * <p>
     * DAY  → "2026-03-02"
     * WEEK → "2026-W09"（ISO 8601 周）
     * MONTH → "2026-03"
     * YEAR  → "2026"
     * </p>
     *
     * @param date     日期
     * @param timeUnit 时间维度
     * @return 时间 key 字符串
     */
    public String formatKey(LocalDate date, TimeUnit timeUnit) {
        return switch (timeUnit) {
            case DAY   -> date.format(DATE_FMT);
            case WEEK  -> String.format("%d-W%02d", isoWeekYear(date), isoWeekOfYear(date));
            case MONTH -> date.format(MONTH_FMT);
            case YEAR  -> date.format(YEAR_FMT);
        };
    }

    /**
     * 将月份字符串格式化为指定时间维度的 key
     *
     * @param statMonth YYYY-MM
     * @param timeUnit  时间维度（MONTH/YEAR，其他维度请用 formatKey(LocalDate)）
     * @return 时间 key 字符串
     */
    public String formatMonthKey(String statMonth, TimeUnit timeUnit) {
        YearMonth ym = YearMonth.parse(statMonth);
        return switch (timeUnit) {
            case MONTH -> statMonth;
            case YEAR  -> String.valueOf(ym.getYear());
            default    -> statMonth;
        };
    }

    // ==================== 时间范围枚举生成 ====================

    /**
     * 生成指定日期范围内、按时间维度的所有时间 key 列表（用于补全图表中无数据的时间点）
     *
     * @param startDate 起始日期（含）
     * @param endDate   截止日期（含）
     * @param timeUnit  时间维度
     * @return 时间 key 列表（有序）
     */
    public List<String> generateTimeKeys(LocalDate startDate, LocalDate endDate, TimeUnit timeUnit) {
        List<String> keys = new ArrayList<>();
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return keys;
        }
        Set<String> seen = new LinkedHashSet<>();

        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            seen.add(formatKey(cursor, timeUnit));
            cursor = switch (timeUnit) {
                case DAY   -> cursor.plusDays(1);
                case WEEK  -> cursor.plusWeeks(1);
                case MONTH -> cursor.plusMonths(1).withDayOfMonth(1);
                case YEAR  -> cursor.plusYears(1).withDayOfYear(1);
            };
        }
        keys.addAll(seen);
        return keys;
    }

    /**
     * 生成指定月份范围内所有月份 key 列表（YYYY-MM）
     *
     * @param startMonth 起始月份（含，YYYY-MM）
     * @param endMonth   截止月份（含，YYYY-MM）
     * @return 月份列表（有序）
     */
    public List<String> generateMonthKeys(String startMonth, String endMonth) {
        List<String> keys = new ArrayList<>();
        if (startMonth == null || endMonth == null) return keys;
        YearMonth cursor = YearMonth.parse(startMonth);
        YearMonth end    = YearMonth.parse(endMonth);
        while (!cursor.isAfter(end)) {
            keys.add(cursor.toString());
            cursor = cursor.plusMonths(1);
        }
        return keys;
    }

    // ==================== SQL 辅助 ====================

    /**
     * 将 TimeUnit 转为 MySQL DATE_FORMAT 格式字符串（用于 SQL GROUP BY 中的 DATE_FORMAT）
     * <p>
     * DAY   → "%Y-%m-%d"
     * WEEK  → "%x-W%v"（ISO 8601）
     * MONTH → "%Y-%m"
     * YEAR  → "%Y"
     * </p>
     *
     * @param timeUnit 时间维度
     * @return MySQL DATE_FORMAT 格式字符串
     */
    public String toMysqlDateFormat(TimeUnit timeUnit) {
        return switch (timeUnit) {
            case DAY   -> "%Y-%m-%d";
            case WEEK  -> "%x-W%v";
            case MONTH -> "%Y-%m";
            case YEAR  -> "%Y";
        };
    }
}
