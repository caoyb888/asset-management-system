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
 * 意向协议-分铺计租阶段表实体 - 对应 inv_intention_fee_stage 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inv_intention_fee_stage")
public class InvIntentionFeeStage extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 费项明细ID */
    private Long intentionFeeId;

    /** 商铺ID */
    private Long shopId;

    /** 阶段开始日期 */
    private LocalDate stageStart;

    /** 阶段结束日期 */
    private LocalDate stageEnd;

    /** 该阶段单价 */
    private BigDecimal unitPrice;

    /** 提成比例(%) */
    private BigDecimal commissionRate;

    /** 最低提成金额 */
    private BigDecimal minCommissionAmount;

    /** 该阶段金额 */
    private BigDecimal amount;
}
