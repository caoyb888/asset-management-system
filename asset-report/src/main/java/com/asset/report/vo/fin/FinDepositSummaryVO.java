package com.asset.report.vo.fin;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 保证金汇总报表 VO（P1）
 * <p>
 * 接口 GET /rpt/fin/deposit-summary 返回值，
 * 基于 rpt_finance_monthly 中的 deposit_balance 字段，
 * 按月份/项目维度展示保证金余额趋势。
 * </p>
 */
@Data
@Accessors(chain = true)
public class FinDepositSummaryVO {

    /** 时间维度（YYYY-MM） */
    private String timeDim;

    /** 项目ID */
    private Long projectId;

    /** 保证金余额（元，月末快照） */
    private BigDecimal depositBalance;

    // ==================== 同比/环比（Service 层计算后回填）====================

    /** 保证金余额同比增长率（%） */
    private BigDecimal depositBalanceYoY;

    /** 保证金余额环比增长率（%） */
    private BigDecimal depositBalanceMoM;
}
