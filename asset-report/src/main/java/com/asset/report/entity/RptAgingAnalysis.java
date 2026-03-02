package com.asset.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 账龄分析表（rpt_aging_analysis）
 * ETL每日T+1预计算，粒度：项目/商家/合同/费项
 */
@Data
@Accessors(chain = true)
@TableName("rpt_aging_analysis")
public class RptAgingAnalysis {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 统计日期 */
    private LocalDate statDate;

    /** 项目ID */
    private Long projectId;

    /** 商家ID */
    private Long merchantId;

    /** 合同ID */
    private Long contractId;

    /** 费项ID（0=所有费项汇总） */
    private Long feeItemId;

    /** 30天内欠款（元） */
    private BigDecimal within30;

    /** 31-60天欠款（元） */
    private BigDecimal days3160;

    /** 61-90天欠款（元） */
    private BigDecimal days6190;

    /** 91-180天欠款（元） */
    private BigDecimal days91180;

    /** 181-365天欠款（元） */
    private BigDecimal days181365;

    /** 365天以上欠款（元） */
    private BigDecimal over365;

    /** 欠款合计（元） */
    private BigDecimal totalOutstanding;

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
