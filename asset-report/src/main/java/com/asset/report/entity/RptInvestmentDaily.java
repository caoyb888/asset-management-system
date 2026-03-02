package com.asset.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 招商日汇总表（rpt_investment_daily）
 * ETL每日T+1更新，汇总粒度：项目/业态/招商负责人
 */
@Data
@Accessors(chain = true)
@TableName("rpt_investment_daily")
public class RptInvestmentDaily {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 统计日期 */
    private LocalDate statDate;

    /** 项目ID */
    private Long projectId;

    /** 业态类型（空串=全业态汇总） */
    private String formatType;

    /** 招商负责人ID（0=全员汇总） */
    private Long investmentManagerId;

    /** 意向协议数（累计有效） */
    private Integer intentionCount;

    /** 已签意向数（已缴意向金） */
    private Integer intentionSigned;

    /** 当日新增意向 */
    private Integer newIntention;

    /** 租赁合同数（累计有效） */
    private Integer contractCount;

    /** 合同总金额（元） */
    private BigDecimal contractAmount;

    /** 签约面积（㎡） */
    private BigDecimal contractArea;

    /** 当日新增合同 */
    private Integer newContract;

    /** 意向转化率（%） */
    private BigDecimal conversionRate;

    /** 平均租金单价（元/㎡/月） */
    private BigDecimal avgRentPrice;

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
