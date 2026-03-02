package com.asset.report.vo.asset;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 品牌/业态分布报表 VO
 * <p>
 * 按 format_type（业态）分组统计，返回各业态的商铺数量、面积及占比，
 * 用于渲染热力图/树状图。
 * </p>
 */
@Data
@Accessors(chain = true)
public class BrandDistributionVO {

    /** 业态类型（如：零售/餐饮/娱乐/服务/其他） */
    private String formatType;

    /** 该业态商铺总数 */
    private Integer totalShops;

    /** 已租商铺数 */
    private Integer rentedShops;

    /** 空置商铺数 */
    private Integer vacantShops;

    /** 已开业商铺数 */
    private Integer openedShops;

    /** 总面积（㎡） */
    private BigDecimal totalArea;

    /** 已租面积（㎡） */
    private BigDecimal rentedArea;

    /** 出租率（%） */
    private BigDecimal rentalRate;

    /** 空置率（%） */
    private BigDecimal vacancyRate;

    /** 商铺数占比（% = 本业态商铺数 / 全部商铺总数 × 100） */
    private BigDecimal shopPercentage;

    /** 面积占比（% = 本业态总面积 / 全部总面积 × 100） */
    private BigDecimal areaPercentage;
}
