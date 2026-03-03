package com.asset.report.vo.fin;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 逾期率统计报表 VO
 * <p>
 * 接口 GET /rpt/fin/overdue-rate 返回值，
 * 按月份/项目维度展示逾期金额和逾期率趋势，可附带同比/环比。
 * </p>
 */
@Data
@Accessors(chain = true)
public class FinOverdueRateVO {

    /** 时间维度（YYYY-MM） */
    private String timeDim;

    /** 项目ID */
    private Long projectId;

    /** 应收总额（元，逾期率计算基础） */
    private BigDecimal receivableAmount;

    /** 逾期总额（元） */
    private BigDecimal overdueAmount;

    /** 逾期率（%）= overdue / receivable × 100 */
    private BigDecimal overdueRate;

    // ==================== 同比/环比（Service 层计算后回填）====================

    /** 逾期率同比变化（百分点） */
    private BigDecimal overdueRateYoY;

    /** 逾期率环比变化（百分点） */
    private BigDecimal overdueRateMoM;

    /** 逾期金额同比增长率（%） */
    private BigDecimal overdueAmountYoY;
}
