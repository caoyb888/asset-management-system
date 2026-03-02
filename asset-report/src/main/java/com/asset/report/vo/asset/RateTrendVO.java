package com.asset.report.vo.asset;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 资产指标趋势数据点 VO
 * <p>
 * 用于空置率/出租率/开业率的时间序列返回，
 * 同时携带同比/环比对比数据（compareMode != NONE 时有值）。
 * </p>
 */
@Data
@Accessors(chain = true)
public class RateTrendVO {

    /** 时间维度标签（DAY: YYYY-MM-DD / WEEK: YYYY-Www / MONTH: YYYY-MM / YEAR: YYYY） */
    private String timeDim;

    /** 当期值（%） */
    private BigDecimal value;

    /** 上期值（同比/环比时有值，compareMode=NONE 时为 null） */
    private BigDecimal prevValue;

    /** 增长率（%），上期为 0 时返回 null（无穷大无意义） */
    private BigDecimal growthRate;

    /** 商铺总数（面积类趋势中携带，其他场景可为 null） */
    private Integer totalShops;

    /** 已租商铺数 */
    private Integer rentedShops;

    /** 总面积（㎡） */
    private BigDecimal totalArea;

    /** 已租面积（㎡） */
    private BigDecimal rentedArea;
}
