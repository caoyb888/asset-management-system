package com.asset.report.vo.inv;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 招商趋势数据点 VO
 * <p>
 * 用于意向新增趋势、合同新增趋势等折线图数据。
 * </p>
 */
@Data
@Accessors(chain = true)
public class InvTrendVO {

    /** 时间维度标签（DAY: yyyy-MM-dd / WEEK: yyyy-ww / MONTH: yyyy-MM / YEAR: yyyy） */
    private String timeDim;

    /** 意向协议数（累计） */
    private Integer intentionCount;

    /** 已签意向数 */
    private Integer intentionSigned;

    /** 当期新增意向 */
    private Integer newIntention;

    /** 租赁合同数（累计） */
    private Integer contractCount;

    /** 合同总金额（元） */
    private BigDecimal contractAmount;

    /** 签约面积（㎡） */
    private BigDecimal contractArea;

    /** 当期新增合同 */
    private Integer newContract;

    /** 意向转化率（%） */
    private BigDecimal conversionRate;

    /** 平均租金单价（元/㎡/月） */
    private BigDecimal avgRentPrice;

    /** 对比期数值（同比/环比，用于折线图叠加） */
    private BigDecimal prevValue;

    /** 增长率（%） */
    private BigDecimal growthRate;
}
