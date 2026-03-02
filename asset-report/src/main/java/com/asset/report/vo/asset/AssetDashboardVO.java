package com.asset.report.vo.asset;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 资产数据看板聚合 VO
 * <p>
 * 一次返回所有图表数据，减少 HTTP 请求数。包含：
 * <ul>
 *   <li>核心指标摘要（最新日期汇总）</li>
 *   <li>同比/环比增长率</li>
 *   <li>30 天趋势折线图数据</li>
 *   <li>项目对比柱状图数据</li>
 * </ul>
 * </p>
 */
@Data
@Accessors(chain = true)
public class AssetDashboardVO {

    /** ETL 最新统计日期 */
    private LocalDate latestDate;

    // ==================== 核心指标摘要 ====================

    /** 商铺总数（项目范围内汇总） */
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

    /** 空置率（%） */
    private BigDecimal vacancyRate;

    /** 出租率（%） */
    private BigDecimal rentalRate;

    /** 开业率（%） */
    private BigDecimal openingRate;

    // ==================== 同比增长率（YoY） ====================

    /** 空置率同比（%），无法计算时为 null */
    private BigDecimal vacancyRateYoY;

    /** 出租率同比（%） */
    private BigDecimal rentalRateYoY;

    /** 开业率同比（%） */
    private BigDecimal openingRateYoY;

    // ==================== 环比增长率（MoM） ====================

    /** 空置率环比（%），无法计算时为 null */
    private BigDecimal vacancyRateMoM;

    /** 出租率环比（%） */
    private BigDecimal rentalRateMoM;

    /** 开业率环比（%） */
    private BigDecimal openingRateMoM;

    // ==================== 趋势图数据（最近 30 天，日粒度） ====================

    /** 空置率 30 天趋势 */
    private List<RateTrendVO> vacancyTrend;

    /** 出租率 30 天趋势 */
    private List<RateTrendVO> rentalTrend;

    /** 开业率 30 天趋势 */
    private List<RateTrendVO> openingTrend;

    // ==================== 项目对比（看板柱状图） ====================

    /** 当前可见项目列表，各项目最新指标，用于项目对比柱状图 */
    private List<ProjectCompareVO> projectComparison;
}
