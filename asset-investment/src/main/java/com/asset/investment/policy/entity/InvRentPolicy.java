package com.asset.investment.policy.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

/** 租决政策主表实体 - 对应 inv_rent_policy 表 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inv_rent_policy")
public class InvRentPolicy extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String policyCode;
    private Long projectId;
    private Integer policyType;
    private BigDecimal year1Rent;
    private BigDecimal year2Rent;
    private BigDecimal year1PropertyFee;
    private BigDecimal year2PropertyFee;
    private String shopAttr;
    private String formatType;
    private Integer minLeaseTerm;
    private Integer maxLeaseTerm;
    private BigDecimal rentGrowthRate;
    private BigDecimal feeGrowthRate;
    private Integer freeRentPeriod;
    private Integer depositMonths;
    private Integer paymentCycle;
    /** 状态: 0草稿/1审批中/2通过/3驳回 */
    private Integer status;
    private String approvalId;
}
