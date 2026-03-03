package com.asset.report.vo.fin;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 财务趋势通用 VO
 * <p>
 * 用于应收/收款/欠款/逾期的月度趋势图，多个接口复用此类型。
 * </p>
 */
@Data
@Accessors(chain = true)
public class FinTrendVO {

    /** 时间维度标签（YYYY-MM） */
    private String timeDim;

    /** 项目ID（不聚合时有值，全局汇总时为 null） */
    private Long projectId;

    /** 应收总额（元） */
    private BigDecimal receivableAmount;

    /** 已收总额（元） */
    private BigDecimal receivedAmount;

    /** 欠款总额（元） */
    private BigDecimal outstandingAmount;

    /** 逾期总额（元） */
    private BigDecimal overdueAmount;

    /** 减免总额（元） */
    private BigDecimal deductionAmount;

    /** 调整总额（元） */
    private BigDecimal adjustmentAmount;

    /** 收缴率（%）= received / receivable × 100 */
    private BigDecimal collectionRate;

    /** 逾期率（%）= overdue / receivable × 100 */
    private BigDecimal overdueRate;

    /** 保证金余额（元，月末快照） */
    private BigDecimal depositBalance;

    /** 预收款余额（元，月末余额） */
    private BigDecimal prepayBalance;

    // ==================== 同比/环比（由 Service 层计算后回填）====================

    /** 应收同比增长率（%） */
    private BigDecimal receivableYoY;

    /** 已收同比增长率（%） */
    private BigDecimal receivedYoY;

    /** 收缴率同比增长（百分点） */
    private BigDecimal collectionRateYoY;

    /** 逾期率同比增长（百分点） */
    private BigDecimal overdueRateYoY;
}
