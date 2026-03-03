package com.asset.report.vo.fin;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 应收汇总报表 VO
 * <p>
 * 接口 GET /rpt/fin/receivable-summary 返回值，
 * 按月份/项目/费项类型维度汇总应收、已收、欠款、减免等金额。
 * </p>
 */
@Data
@Accessors(chain = true)
public class FinReceivableSummaryVO {

    /** 时间维度（YYYY-MM） */
    private String timeDim;

    /** 项目ID */
    private Long projectId;

    /** 费项类型（租金/物业费/推广费等，null 表示全费项汇总） */
    private String feeItemType;

    /** 应收总额（元） */
    private BigDecimal receivableAmount;

    /** 已收总额（元） */
    private BigDecimal receivedAmount;

    /** 欠款总额（元）= receivable - received */
    private BigDecimal outstandingAmount;

    /** 减免总额（元） */
    private BigDecimal deductionAmount;

    /** 调整总额（元） */
    private BigDecimal adjustmentAmount;

    /** 收缴率（%）= received / receivable × 100 */
    private BigDecimal collectionRate;

    // ==================== 同比/环比（Service 层计算后回填）====================

    /** 应收同比增长率（%） */
    private BigDecimal receivableYoY;

    /** 已收同比增长率（%） */
    private BigDecimal receivedYoY;

    /** 收缴率同比变化（百分点） */
    private BigDecimal collectionRateYoY;

    /** 应收环比增长率（%） */
    private BigDecimal receivableMoM;

    /** 收缴率环比变化（百分点） */
    private BigDecimal collectionRateMoM;
}
