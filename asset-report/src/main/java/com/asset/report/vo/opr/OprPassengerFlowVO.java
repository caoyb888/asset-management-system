package com.asset.report.vo.opr;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 客流数据分析 VO（P1）
 * <p>
 * 按时间维度聚合客流量，支持同比/环比对比，用于客流趋势分析图表。
 * </p>
 */
@Data
@Accessors(chain = true)
public class OprPassengerFlowVO {

    /** 时间维度标签（MONTH: yyyy-MM / YEAR: yyyy） */
    private String timeDim;

    /** 项目ID */
    private Long projectId;

    /** 月客流总量（人次） */
    private Long passengerFlow;

    /** 日均客流（人次） */
    private Integer avgDailyPassenger;

    /** 对比期客流量（同比/环比） */
    private Long prevPassengerFlow;

    /** 客流增长率（%） */
    private BigDecimal growthRate;
}
