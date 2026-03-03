package com.asset.report.vo.fin;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 收缴率统计报表 VO
 * <p>
 * 接口 GET /rpt/fin/collection-rate 返回值，
 * 按月份/项目/费项维度展示收缴率趋势，可附带同比/环比，
 * 可用于渲染月度收缴率折线图和项目对比柱状图。
 * </p>
 */
@Data
@Accessors(chain = true)
public class FinCollectionRateVO {

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

    /** 收缴率（%）= received / receivable × 100 */
    private BigDecimal collectionRate;

    // ==================== 同比/环比（Service 层计算后回填）====================

    /** 收缴率同比变化（百分点） */
    private BigDecimal collectionRateYoY;

    /** 收缴率环比变化（百分点） */
    private BigDecimal collectionRateMoM;
}
