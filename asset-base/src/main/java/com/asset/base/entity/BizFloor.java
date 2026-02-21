package com.asset.base.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 楼层实体 - 对应 biz_floor 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_floor")
public class BizFloor extends BaseEntity {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属项目ID */
    private Long projectId;

    /** 所属楼栋ID */
    private Long buildingId;

    /** 楼层编码 */
    private String floorCode;

    /** 楼层名称 */
    private String floorName;

    /**
     * 状态
     * 0-停用 1-启用
     */
    private Integer status;

    /** 建筑面积(㎡) */
    private BigDecimal buildingArea;

    /** 经营面积(㎡) */
    private BigDecimal operatingArea;

    /** 备注 */
    private String remark;

    /** 楼层平面图URL */
    private String imageUrl;
}
