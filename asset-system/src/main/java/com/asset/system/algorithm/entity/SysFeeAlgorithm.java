package com.asset.system.algorithm.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 租费算法规则表 sys_fee_algorithm */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_fee_algorithm", autoResultMap = true)
public class SysFeeAlgorithm extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 算法编码（唯一） */
    private String algoCode;

    /** 算法名称 */
    private String algoName;

    /**
     * 算法类型：
     * 1-租金算法  2-保证金算法  3-服务费算法  4-其他
     */
    private Integer algoType;

    /**
     * 计算方式：
     * 1-固定金额  2-比率计算  3-阶梯计算  4-自定义公式
     */
    private Integer calcMode;

    /**
     * 计算公式表达式，变量用英文名
     * 示例：unit_price * area * months
     * 支持 Math.max / Math.min
     */
    private String formula;

    /**
     * 变量定义列表（JSON Array）
     * [{key, label, unit, required, defaultVal}]
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode variables;

    /**
     * 固定参数（JSON Object），公式中的常量
     * 示例：{"tax_rate": 0.06}
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode params;

    /** 算法说明 */
    private String description;

    /** 状态：0停用 1启用 */
    private Integer status;
}
