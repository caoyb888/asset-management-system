package com.asset.report.vo.fin;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 账龄分析报表 VO
 * <p>
 * 接口 GET /rpt/fin/aging-analysis 返回值，
 * 基于 rpt_aging_analysis 预计算表，按商家/合同维度展示账龄分档欠款，
 * 可用于渲染账龄堆叠图和商家欠款排行榜。
 * </p>
 */
@Data
@Accessors(chain = true)
public class FinAgingAnalysisVO {

    /** 统计日期 */
    private LocalDate statDate;

    /** 项目ID */
    private Long projectId;

    /** 商家ID（汇总查询时为 null） */
    private Long merchantId;

    /** 合同ID（汇总查询时为 null） */
    private Long contractId;

    // ==================== 账龄分档金额 ====================

    /** 30 天内欠款（元） */
    private BigDecimal within30;

    /** 31-60 天欠款（元） */
    private BigDecimal days3160;

    /** 61-90 天欠款（元） */
    private BigDecimal days6190;

    /** 91-180 天欠款（元） */
    private BigDecimal days91180;

    /** 181-365 天欠款（元） */
    private BigDecimal days181365;

    /** 365 天以上欠款（元） */
    private BigDecimal over365;

    /** 欠款合计（元） */
    private BigDecimal totalOutstanding;

    // ==================== 各分档占比（Service 层计算后回填）====================

    /** 30 天内欠款占比（%） */
    private BigDecimal within30Rate;

    /** 31-60 天欠款占比（%） */
    private BigDecimal days3160Rate;

    /** 61-90 天欠款占比（%） */
    private BigDecimal days6190Rate;

    /** 91-180 天欠款占比（%） */
    private BigDecimal days91180Rate;

    /** 181-365 天欠款占比（%） */
    private BigDecimal days181365Rate;

    /** 365 天以上欠款占比（%） */
    private BigDecimal over365Rate;
}
