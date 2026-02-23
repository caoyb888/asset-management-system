package com.asset.investment.intention.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 意向协议-商铺关联表实体 - 对应 inv_intention_shop 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inv_intention_shop")
public class InvIntentionShop extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 意向协议ID */
    private Long intentionId;

    /** 商铺ID */
    private Long shopId;

    /** 楼栋ID */
    private Long buildingId;

    /** 楼层ID */
    private Long floorId;

    /** 业态 */
    private String formatType;

    /** 租赁面积(㎡) */
    private BigDecimal area;
}
