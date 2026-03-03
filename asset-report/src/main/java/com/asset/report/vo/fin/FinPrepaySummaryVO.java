package com.asset.report.vo.fin;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 预收款汇总报表 VO（P1）
 * <p>
 * 接口 GET /rpt/fin/prepay-summary 返回值，
 * 基于 rpt_finance_monthly 中的 prepay_balance 字段，
 * 按月份/项目维度展示预收款余额趋势。
 * </p>
 */
@Data
@Accessors(chain = true)
public class FinPrepaySummaryVO {

    /** 时间维度（YYYY-MM） */
    private String timeDim;

    /** 项目ID */
    private Long projectId;

    /** 预收款余额（元，月末余额） */
    private BigDecimal prepayBalance;

    // ==================== 同比/环比（Service 层计算后回填）====================

    /** 预收款余额同比增长率（%） */
    private BigDecimal prepayBalanceYoY;

    /** 预收款余额环比增长率（%） */
    private BigDecimal prepayBalanceMoM;
}
