package com.asset.report.vo.fin;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 欠款统计报表 VO
 * <p>
 * 接口 GET /rpt/fin/outstanding-summary 返回值，
 * 按月份/项目维度汇总欠款总额、逾期金额及账龄分布，
 * 可用于渲染欠款汇总表和账龄分布堆叠柱状图。
 * </p>
 */
@Data
@Accessors(chain = true)
public class FinOutstandingSummaryVO {

    /** 时间维度（YYYY-MM） */
    private String timeDim;

    /** 项目ID */
    private Long projectId;

    /** 费项类型（null 表示全费项汇总） */
    private String feeItemType;

    /** 应收总额（元） */
    private BigDecimal receivableAmount;

    /** 已收总额（元） */
    private BigDecimal receivedAmount;

    /** 欠款总额（元） */
    private BigDecimal outstandingAmount;

    /** 逾期总额（元） */
    private BigDecimal overdueAmount;

    /** 逾期率（%） */
    private BigDecimal overdueRate;

    /** 减免总额（元） */
    private BigDecimal deductionAmount;

    /** 调整总额（元） */
    private BigDecimal adjustmentAmount;

    // ==================== 同比/环比（Service 层计算后回填）====================

    /** 欠款同比增长率（%） */
    private BigDecimal outstandingYoY;

    /** 逾期率同比变化（百分点） */
    private BigDecimal overdueRateYoY;
}
