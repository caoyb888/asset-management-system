package com.asset.report.vo.opr;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 营运趋势数据点 VO
 * <p>
 * 用于营收趋势、客流趋势等折线图的通用数据点结构。
 * </p>
 */
@Data
@Accessors(chain = true)
public class OprTrendVO {

    /** 时间维度标签（MONTH: yyyy-MM / YEAR: yyyy） */
    private String timeDim;

    /** 月营收总额（元） */
    private BigDecimal revenueAmount;

    /** 浮动租金总额（元） */
    private BigDecimal floatingRentAmount;

    /** 坪效（元/㎡） */
    private BigDecimal avgRevenuePerSqm;

    /** 月客流总量（人次） */
    private Long passengerFlow;

    /** 日均客流（人次） */
    private Integer avgDailyPassenger;

    /** 合同变更次数 */
    private Integer changeCount;

    /** 变更租金影响额（元） */
    private BigDecimal changeRentImpact;

    /** 即将到期合同数 */
    private Integer expiringContracts;

    /** 解约合同数 */
    private Integer terminatedContracts;

    /** 对比期营收（同比/环比，用于折线图叠加） */
    private BigDecimal prevRevenueAmount;

    /** 营收增长率（%） */
    private BigDecimal revenueGrowthRate;
}
