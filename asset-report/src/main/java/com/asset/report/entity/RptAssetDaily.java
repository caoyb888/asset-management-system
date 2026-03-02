package com.asset.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产日汇总表（rpt_asset_daily）
 * ETL每日T+1更新，汇总粒度：项目/楼栋/楼层/业态
 */
@Data
@Accessors(chain = true)
@TableName("rpt_asset_daily")
public class RptAssetDaily {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 统计日期（ETL日期=T-1） */
    private LocalDate statDate;

    /** 项目ID */
    private Long projectId;

    /** 楼栋ID（0=项目级汇总） */
    private Long buildingId;

    /** 楼层ID（0=楼栋级汇总） */
    private Long floorId;

    /** 业态类型（空串=全业态汇总） */
    private String formatType;

    /** 商铺总数 */
    private Integer totalShops;

    /** 已租商铺数 */
    private Integer rentedShops;

    /** 空置商铺数 */
    private Integer vacantShops;

    /** 装修中商铺数 */
    private Integer decoratingShops;

    /** 已开业商铺数 */
    private Integer openedShops;

    /** 总面积（㎡） */
    private BigDecimal totalArea;

    /** 已租面积（㎡） */
    private BigDecimal rentedArea;

    /** 空置面积（㎡） */
    private BigDecimal vacantArea;

    /** 装修中面积（㎡） */
    private BigDecimal decorationArea;

    /** 空置率（%）= vacant_area/total_area*100 */
    private BigDecimal vacancyRate;

    /** 出租率（%）= rented_area/total_area*100 */
    private BigDecimal rentalRate;

    /** 开业率（%）= opened_shops/total_shops*100 */
    private BigDecimal openingRate;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
