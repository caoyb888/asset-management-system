package com.asset.operation.revenue.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

/** 浮动租金表（月维度浮动租金计算结果）- 对应 opr_floating_rent */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("opr_floating_rent")
public class OprFloatingRent extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 合同ID */
    private Long contractId;
    /** 商铺ID */
    private Long shopId;
    /** 计算月份（YYYY-MM） */
    private String calcMonth;
    /** 月营业额 */
    private BigDecimal monthlyRevenue;
    /** 固定租金（用于取高比较） */
    private BigDecimal fixedRent;
    /** 提成比例（%） */
    private BigDecimal commissionRate;
    /** 提成金额 */
    private BigDecimal commissionAmount;
    /** 浮动租金（取高后差额或提成结果） */
    private BigDecimal floatingRent;
    /** 计算公式说明（便于业务人员理解） */
    private String calcFormula;
    /** 关联生成的应收记录ID */
    private Long receivableId;
}
