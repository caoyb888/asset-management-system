package com.asset.report.vo.fin;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 收款汇总报表 VO
 * <p>
 * 接口 GET /rpt/fin/receipt-summary 返回值，
 * 按月份/项目/费项维度汇总已收金额和收缴率趋势。
 * </p>
 */
@Data
@Accessors(chain = true)
public class FinReceiptSummaryVO {

    /** 时间维度（YYYY-MM） */
    private String timeDim;

    /** 项目ID */
    private Long projectId;

    /** 费项类型（null 表示全费项汇总） */
    private String feeItemType;

    /** 应收总额（元，用于计算收缴率） */
    private BigDecimal receivableAmount;

    /** 已收总额（元） */
    private BigDecimal receivedAmount;

    /** 欠款总额（元） */
    private BigDecimal outstandingAmount;

    /** 收缴率（%） */
    private BigDecimal collectionRate;

    // ==================== 同比/环比（Service 层计算后回填）====================

    /** 已收同比增长率（%） */
    private BigDecimal receivedYoY;

    /** 收缴率同比变化（百分点） */
    private BigDecimal collectionRateYoY;

    /** 已收环比增长率（%） */
    private BigDecimal receivedMoM;

    /** 收缴率环比变化（百分点） */
    private BigDecimal collectionRateMoM;
}
