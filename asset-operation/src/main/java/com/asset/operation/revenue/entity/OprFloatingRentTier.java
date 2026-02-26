package com.asset.operation.revenue.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

/** 浮动租金阶梯明细表（多档累进提成，支持计算过程审计）- 对应 opr_floating_rent_tier */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("opr_floating_rent_tier")
public class OprFloatingRentTier extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 浮动租金记录ID */
    private Long floatingRentId;
    /** 阶梯档位序号（从1开始） */
    private Integer tierNo;
    /** 本档起始营业额（NULL表示从0起） */
    private BigDecimal revenueFrom;
    /** 本档终止营业额（NULL表示无上限） */
    private BigDecimal revenueTo;
    /** 本档提成比例（%） */
    private BigDecimal rate;
    /** 本档计算提成金额 */
    private BigDecimal tierAmount;
}
