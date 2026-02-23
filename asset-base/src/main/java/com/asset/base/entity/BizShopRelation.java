package com.asset.base.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商铺拆分/合并关联记录 - 对应 biz_shop_relation 表
 */
@Data
@TableName("biz_shop_relation")
public class BizShopRelation implements Serializable {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 源商铺ID */
    private Long sourceShopId;

    /** 目标商铺ID */
    private Long targetShopId;

    /**
     * 关联类型
     * 1-拆分 2-合并
     */
    private Integer relationType;

    /** 变更前面积(㎡) */
    private BigDecimal areaBefore;

    /** 变更后面积(㎡) */
    private BigDecimal areaAfter;

    /** 备注 */
    private String remark;

    /** 创建人ID */
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
