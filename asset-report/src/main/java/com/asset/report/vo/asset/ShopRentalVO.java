package com.asset.report.vo.asset;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 商铺租赁信息报表 VO
 * <p>
 * 基于 rpt_asset_daily 楼栋/楼层粒度数据，返回多维度的租赁状态统计。
 * 支持按项目/楼栋/楼层/业态筛选，支持分页。
 * </p>
 */
@Data
@Accessors(chain = true)
public class ShopRentalVO {

    /** 统计日期 */
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

    /** 空置率（%） */
    private BigDecimal vacancyRate;

    /** 出租率（%） */
    private BigDecimal rentalRate;

    /** 开业率（%） */
    private BigDecimal openingRate;
}
