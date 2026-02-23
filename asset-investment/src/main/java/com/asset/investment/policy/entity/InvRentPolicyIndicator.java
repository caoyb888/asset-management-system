package com.asset.investment.policy.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

/** 租决政策-分类指标表实体 - 对应 inv_rent_policy_indicator 表 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inv_rent_policy_indicator")
public class InvRentPolicyIndicator extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long policyId;
    /** 商铺类别: 1主力店/2次主力店/3一般商铺 */
    private Integer shopCategory;
    private BigDecimal rentPrice;
    private BigDecimal propertyFeePrice;
    private String formatType;
    private BigDecimal rentGrowthRate;
    private BigDecimal feeGrowthRate;
    private Integer freeRentMonths;
    private Integer depositMonths;
}
