package com.asset.investment.config.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 计租方案配置实体 - 对应 cfg_rent_scheme 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "cfg_rent_scheme", autoResultMap = true)
public class CfgRentScheme extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 方案编码 */
    private String schemeCode;

    /** 方案名称 */
    private String schemeName;

    /**
     * 默认收费方式
     * 1固定/2固定提成/3阶梯提成/4取高/5一次性
     */
    private Integer chargeType;

    /**
     * 默认支付周期
     * 1月付/2两月付/3季付/4四月付/5半年付/6年付
     */
    private Integer paymentCycle;

    /**
     * 默认账期模式
     * 1预付/2当期/3后付
     */
    private Integer billingMode;

    /**
     * 租金计算公式配置(JSON格式)
     * 示例: {"type":"fixed","params":["unit_price","area","months"]}
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode formulaJson;

    /** 策略Bean名称（用于Spring策略路由） */
    private String strategyBeanName;

    /** 状态: 1启用/0停用 */
    private Integer status;

    /** 方案说明 */
    private String description;
}
