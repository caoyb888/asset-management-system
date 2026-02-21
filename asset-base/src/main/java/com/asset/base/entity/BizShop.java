package com.asset.base.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商铺实体 - 对应 biz_shop 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_shop")
public class BizShop extends BaseEntity {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属项目ID */
    private Long projectId;

    /** 所属楼栋ID */
    private Long buildingId;

    /** 所在楼层ID */
    private Long floorId;

    /** 铺位号 */
    private String shopCode;

    /**
     * 商铺类型
     * 1-临街 2-内铺 3-专柜
     */
    private Integer shopType;

    /** 计租面积(㎡) */
    private BigDecimal rentArea;

    /** 实测面积(㎡) */
    private BigDecimal measuredArea;

    /** 建筑面积(㎡) */
    private BigDecimal buildingArea;

    /** 经营面积(㎡) */
    private BigDecimal operatingArea;

    /**
     * 商铺状态
     * 0-空置 1-在租 2-自用 3-预留
     */
    private Integer shopStatus;

    /** 计入招商率：0否 1是 */
    private Integer countLeasingRate;

    /** 计入出租率：0否 1是 */
    private Integer countRentalRate;

    /** 计入开业率：0否 1是 */
    private Integer countOpeningRate;

    /** 签约业态 */
    private String signedFormat;

    /** 规划业态 */
    private String plannedFormat;

    /** 业主名称 */
    private String ownerName;

    /** 业主联系人 */
    private String ownerContact;

    /** 业主电话 */
    private String ownerPhone;
}
