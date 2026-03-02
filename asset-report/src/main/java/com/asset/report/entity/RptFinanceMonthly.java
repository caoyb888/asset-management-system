package com.asset.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 财务月汇总表（rpt_finance_monthly）
 * ETL每月T+1更新，汇总粒度：项目/费项
 */
@Data
@Accessors(chain = true)
@TableName("rpt_finance_monthly")
public class RptFinanceMonthly {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 统计月份（YYYY-MM格式） */
    private String statMonth;

    /** 项目ID */
    private Long projectId;

    /** 费项ID（0=所有费项汇总） */
    private Long feeItemId;

    /** 费项类型（租金/物业费等） */
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

    /** 逾期总额（元） */
    private BigDecimal overdueAmount;

    /** 逾期率（%） */
    private BigDecimal overdueRate;

    /** 保证金余额（元，月末快照） */
    private BigDecimal depositBalance;

    /** 预收款余额（元，月末余额） */
    private BigDecimal prepayBalance;

    /** 收缴率（%）= received/receivable*100 */
    private BigDecimal collectionRate;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
