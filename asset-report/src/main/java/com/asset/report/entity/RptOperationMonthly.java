package com.asset.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 营运月汇总表（rpt_operation_monthly）
 * ETL每月T+1更新，汇总粒度：项目/楼栋/业态
 */
@Data
@Accessors(chain = true)
@TableName("rpt_operation_monthly")
public class RptOperationMonthly {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 统计月份（YYYY-MM格式） */
    private String statMonth;

    /** 项目ID */
    private Long projectId;

    /** 楼栋ID（0=项目级汇总） */
    private Long buildingId;

    /** 业态类型（空串=全业态汇总） */
    private String formatType;

    /** 月营收总额（元） */
    private BigDecimal revenueAmount;

    /** 浮动租金总额（元） */
    private BigDecimal floatingRentAmount;

    /** 坪效（元/㎡） */
    private BigDecimal avgRevenuePerSqm;

    /** 月客流总量（人次） */
    private Long passengerFlow;

    /** 日均客流（人次） */
    private Integer avgDailyPassenger;

    /** 合同变更次数 */
    private Integer changeCount;

    /** 变更租金影响额（元） */
    private BigDecimal changeRentImpact;

    /** 即将到期合同数（90天内） */
    private Integer expiringContracts;

    /** 本月解约合同数 */
    private Integer terminatedContracts;

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
