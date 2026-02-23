package com.asset.investment.intention.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 意向协议-费项明细表实体 - 对应 inv_intention_fee 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "inv_intention_fee", autoResultMap = true)
public class InvIntentionFee extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 意向协议ID */
    private Long intentionId;

    /** 收款项目ID */
    private Long feeItemId;

    /** 费项名称 */
    private String feeName;

    /**
     * 收费方式
     * 1固定/2固定提成/3阶梯提成/4取高/5一次性
     */
    private Integer chargeType;

    /** 单价(元/㎡/月) */
    private BigDecimal unitPrice;

    /** 面积(㎡) */
    private BigDecimal area;

    /** 金额(元) */
    private BigDecimal amount;

    /** 费项开始日期 */
    private LocalDate startDate;

    /** 费项结束日期 */
    private LocalDate endDate;

    /** 租期阶段序号（拆分租期用） */
    private Integer periodIndex;

    /**
     * 计算公式参数(JSON)
     * 含 commission_rate、min_commission_amount 等动态参数
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode formulaParams;
}
