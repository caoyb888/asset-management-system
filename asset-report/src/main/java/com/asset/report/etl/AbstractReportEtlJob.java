package com.asset.report.etl;

import com.xxl.job.core.biz.model.ReturnT;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 报表 ETL 抽象基类
 * <p>
 * 提供统一的：
 * 1. 参数解析（日期/月份，默认T-1或上月）
 * 2. 失败自动重试（最多3次，指数退避）
 * 3. 执行日志（INFO + ERROR）
 * <p>
 * 子类只需实现：
 * - {@link #doEtl(LocalDate)} 或 {@link #doEtlMonthly(String)}
 * - {@link #getJobName()}
 */
@Slf4j
public abstract class AbstractReportEtlJob {

    protected static final int MAX_RETRY = 3;
    private static final DateTimeFormatter DATE_FMT  = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    // ==========================================
    // 模板方法：日级 ETL 入口
    // ==========================================

    /**
     * XXL-Job 回调入口（日级任务）
     * 参数格式：yyyy-MM-dd（留空默认昨日）
     */
    protected ReturnT<String> executeDaily(String param) {
        LocalDate statDate = parseDateParam(param);
        log.info("[ETL-DAILY] {} 开始，统计日期: {}", getJobName(), statDate);
        long begin = System.currentTimeMillis();
        try {
            doWithRetry(() -> doEtl(statDate), statDate.toString());
            long cost = System.currentTimeMillis() - begin;
            log.info("[ETL-DAILY] {} 成功，耗时: {}ms，日期: {}", getJobName(), cost, statDate);
            return new ReturnT<>(ReturnT.SUCCESS_CODE,
                    "OK date=" + statDate + " cost=" + cost + "ms");
        } catch (Exception e) {
            log.error("[ETL-DAILY] {} 最终失败，日期: {}", getJobName(), statDate, e);
            return new ReturnT<>(ReturnT.FAIL_CODE, "FAIL: " + e.getMessage());
        }
    }

    // ==========================================
    // 模板方法：月级 ETL 入口
    // ==========================================

    /**
     * XXL-Job 回调入口（月级任务）
     * 参数格式：yyyy-MM（留空默认上月）
     */
    protected ReturnT<String> executeMonthly(String param) {
        String statMonth = parseMonthParam(param);
        log.info("[ETL-MONTHLY] {} 开始，统计月份: {}", getJobName(), statMonth);
        long begin = System.currentTimeMillis();
        try {
            doWithRetry(() -> doEtlMonthly(statMonth), statMonth);
            long cost = System.currentTimeMillis() - begin;
            log.info("[ETL-MONTHLY] {} 成功，耗时: {}ms，月份: {}", getJobName(), cost, statMonth);
            return new ReturnT<>(ReturnT.SUCCESS_CODE,
                    "OK month=" + statMonth + " cost=" + cost + "ms");
        } catch (Exception e) {
            log.error("[ETL-MONTHLY] {} 最终失败，月份: {}", getJobName(), statMonth, e);
            return new ReturnT<>(ReturnT.FAIL_CODE, "FAIL: " + e.getMessage());
        }
    }

    // ==========================================
    // 重试机制
    // ==========================================

    /**
     * 带重试的任务执行（最多3次，指数退避：1s/2s/3s）
     */
    private void doWithRetry(EtlTask task, String context) throws Exception {
        Exception lastEx = null;
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                task.run();
                return;
            } catch (Exception e) {
                lastEx = e;
                if (attempt < MAX_RETRY) {
                    log.warn("[ETL] {} 第{}次失败 context={} error={}，{}s后重试",
                            getJobName(), attempt, context, e.getMessage(), attempt);
                    Thread.sleep(1000L * attempt);
                }
            }
        }
        throw lastEx;
    }

    @FunctionalInterface
    private interface EtlTask {
        void run() throws Exception;
    }

    // ==========================================
    // 参数解析
    // ==========================================

    /**
     * 解析日期参数，留空默认昨日
     */
    protected LocalDate parseDateParam(String param) {
        if (param == null || param.trim().isEmpty()) {
            return LocalDate.now().minusDays(1);
        }
        try {
            String s = param.trim();
            // 支持 yyyy-MM 格式（取当月最后一天）
            if (s.length() == 7) {
                return LocalDate.parse(s + "-01", DATE_FMT).withDayOfMonth(
                        java.time.YearMonth.parse(s, MONTH_FMT).lengthOfMonth());
            }
            return LocalDate.parse(s, DATE_FMT);
        } catch (DateTimeParseException e) {
            log.warn("[ETL] 参数解析失败 param={} 使用默认昨日", param);
            return LocalDate.now().minusDays(1);
        }
    }

    /**
     * 解析月份参数，留空默认上月
     */
    protected String parseMonthParam(String param) {
        if (param == null || param.trim().isEmpty()) {
            return LocalDate.now().minusMonths(1).format(MONTH_FMT);
        }
        String s = param.trim();
        // 如果传了完整日期 yyyy-MM-dd，取年月
        if (s.length() == 10) {
            return s.substring(0, 7);
        }
        // 验证格式
        try {
            java.time.YearMonth.parse(s, MONTH_FMT);
            return s;
        } catch (DateTimeParseException e) {
            log.warn("[ETL] 月份参数解析失败 param={} 使用默认上月", param);
            return LocalDate.now().minusMonths(1).format(MONTH_FMT);
        }
    }

    // ==========================================
    // 抽象方法（子类按需实现）
    // ==========================================

    /** 日级任务子类实现（默认抛异常，日级任务必须覆盖） */
    protected void doEtl(LocalDate statDate) throws Exception {
        throw new UnsupportedOperationException(getJobName() + " 未实现 doEtl(LocalDate)");
    }

    /** 月级任务子类实现（默认抛异常，月级任务必须覆盖） */
    protected void doEtlMonthly(String statMonth) throws Exception {
        throw new UnsupportedOperationException(getJobName() + " 未实现 doEtlMonthly(String)");
    }

    /** 返回任务名称（用于日志区分） */
    protected abstract String getJobName();

    // ==========================================
    // 工具方法
    // ==========================================

    /**
     * 计算比率，避免除零，保留2位小数
     */
    protected java.math.BigDecimal calcRate(java.math.BigDecimal numerator,
                                             java.math.BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(java.math.BigDecimal.ZERO) == 0) {
            return java.math.BigDecimal.ZERO;
        }
        return numerator.divide(denominator, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new java.math.BigDecimal("100"))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 安全取整，null 返回 0
     */
    protected int safeInt(Integer val) {
        return val == null ? 0 : val;
    }

    /**
     * 安全取 BigDecimal，null 返回 ZERO
     */
    protected java.math.BigDecimal safeDec(java.math.BigDecimal val) {
        return val == null ? java.math.BigDecimal.ZERO : val;
    }

    /**
     * 按批次处理列表，避免单次 SQL 过大
     */
    protected <T> java.util.List<java.util.List<T>> partition(java.util.List<T> list, int batchSize) {
        java.util.List<java.util.List<T>> result = new java.util.ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            result.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return result;
    }
}
