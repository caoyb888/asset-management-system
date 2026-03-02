package com.asset.report.vo.asset;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 区域归属报表 VO（P1）
 * <p>
 * 按省/市维度汇总资产指标，通过关联 biz_project 获取地理信息。
 * </p>
 */
@Data
@Accessors(chain = true)
public class RegionSummaryVO {

    /** 省份（如：广东省） */
    private String province;

    /** 城市（如：深圳市） */
    private String city;

    /** 该区域内项目数量 */
    private Integer projectCount;

    /** 商铺总数（该区域汇总） */
    private Integer totalShops;

    /** 已租商铺数 */
    private Integer rentedShops;

    /** 空置商铺数 */
    private Integer vacantShops;

    /** 总面积（㎡） */
    private BigDecimal totalArea;

    /** 已租面积（㎡） */
    private BigDecimal rentedArea;

    /** 空置面积（㎡） */
    private BigDecimal vacantArea;

    /** 出租率（% = 该区域平均）*/
    private BigDecimal rentalRate;

    /** 空置率（% = 该区域平均） */
    private BigDecimal vacancyRate;
}
