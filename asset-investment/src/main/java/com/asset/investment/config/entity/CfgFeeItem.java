package com.asset.investment.config.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 收款项目配置实体 - 对应 cfg_fee_item 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cfg_fee_item")
public class CfgFeeItem extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 项目编码 */
    private String itemCode;

    /** 项目名称(租金/保证金/物管费等) */
    private String itemName;

    /**
     * 类型
     * 1租金类/2保证金类/3服务费类
     */
    private Integer itemType;

    /** 是否必填: 0否/1是 */
    private Integer isRequired;

    /** 排序 */
    private Integer sortOrder;

    /** 启用状态: 1启用/0停用 */
    private Integer status;
}
