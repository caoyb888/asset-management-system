package com.asset.report.vo.fin;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 减免/调整统计报表 VO（P1）
 * <p>
 * 接口 GET /rpt/fin/deduction-adjustment 返回值，
 * 基于 rpt_finance_monthly，按月份/项目/费项维度统计减免金额、调整金额及占应收比例。
 * </p>
 */
@Data
@Accessors(chain = true)
public class FinDeductionAdjustmentVO {

    /** 时间维度（YYYY-MM） */
    private String timeDim;

    /** 项目ID */
    private Long projectId;

    /** 费项类型（null 表示全费项汇总） */
    private String feeItemType;

    /** 应收总额（元，用于计算减免率） */
    private BigDecimal receivableAmount;

    /** 减免总额（元） */
    private BigDecimal deductionAmount;

    /** 调整总额（元） */
    private BigDecimal adjustmentAmount;

    /** 减免率（%）= deduction / receivable × 100，Service 层计算 */
    private BigDecimal deductionRate;

    /** 调整率（%）= adjustment / receivable × 100，Service 层计算 */
    private BigDecimal adjustmentRate;

    // ==================== 同比/环比（Service 层计算后回填）====================

    /** 减免金额同比增长率（%） */
    private BigDecimal deductionYoY;

    /** 调整金额同比增长率（%） */
    private BigDecimal adjustmentYoY;
}
