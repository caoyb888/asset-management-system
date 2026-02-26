package com.asset.operation.revenue.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 营收填报表（按日录入营业额）- 对应 opr_revenue_report */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("opr_revenue_report")
public class OprRevenueReport extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 项目ID */
    private Long projectId;
    /** 合同ID */
    private Long contractId;
    /** 商铺ID */
    private Long shopId;
    /** 商家ID */
    private Long merchantId;
    /** 填报日期（具体某天） */
    private LocalDate reportDate;
    /** 填报月份（YYYY-MM） */
    private String reportMonth;
    /** 营业额 */
    private BigDecimal revenueAmount;
    /** 状态（0待确认/1已确认） */
    private Integer status;
}
