package com.asset.investment.intention.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 意向协议-账期表实体 - 对应 inv_intention_billing 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inv_intention_billing")
public class InvIntentionBilling extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 意向协议ID */
    private Long intentionId;

    /** 收款项目ID */
    private Long feeItemId;

    /** 账期开始 */
    private LocalDate billingStart;

    /** 账期结束 */
    private LocalDate billingEnd;

    /** 应收日期 */
    private LocalDate dueDate;

    /** 应收金额 */
    private BigDecimal amount;

    /**
     * 账期类型
     * 1首账期/2正常账期
     */
    private Integer billingType;

    /**
     * 收款状态
     * 0未收/1部分/2已收
     */
    private Integer status;
}
