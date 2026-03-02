package com.asset.report.vo.asset;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 项目对比数据 VO（看板聚合接口中使用）
 * <p>
 * 返回各项目最新日期的核心资产指标，供前端渲染柱状图/雷达图对比。
 * </p>
 */
@Data
@Accessors(chain = true)
public class ProjectCompareVO {

    /** 项目ID */
    private Long projectId;

    /** 空置率（%） */
    private BigDecimal vacancyRate;

    /** 出租率（%） */
    private BigDecimal rentalRate;

    /** 开业率（%） */
    private BigDecimal openingRate;

    /** 总面积（㎡） */
    private BigDecimal totalArea;

    /** 已租面积（㎡） */
    private BigDecimal rentedArea;

    /** 空置面积（㎡） */
    private BigDecimal vacantArea;

    /** 商铺总数 */
    private Integer totalShops;

    /** 已租商铺数 */
    private Integer rentedShops;

    /** 空置商铺数 */
    private Integer vacantShops;

    /** 已开业商铺数 */
    private Integer openedShops;
}
