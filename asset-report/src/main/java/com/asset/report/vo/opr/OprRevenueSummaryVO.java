package com.asset.report.vo.opr;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 营收填报汇总 VO
 * <p>
 * 按时间维度聚合营收相关指标，用于趋势图和统计表格。
 * compareMode=YOY/MOM 时附带同比/环比增长率。
 * </p>
 */
@Data
@Accessors(chain = true)
public class OprRevenueSummaryVO {

    /** 时间维度标签（MONTH: yyyy-MM / YEAR: yyyy） */
    private String timeDim;

    /** 项目ID */
    private Long projectId;

    /** 业态类型（空串=全业态汇总） */
    private String formatType;

    /** 月营收总额（元） */
    private BigDecimal revenueAmount;

    /** 浮动租金总额（元） */
    private BigDecimal floatingRentAmount;

    /** 坪效（元/㎡） */
    private BigDecimal avgRevenuePerSqm;

    /** 对比期营收总额（同比/环比） */
    private BigDecimal prevRevenueAmount;

    /** 营收增长率（%） */
    private BigDecimal revenueGrowthRate;
}
