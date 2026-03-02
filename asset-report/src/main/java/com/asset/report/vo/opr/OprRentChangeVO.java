package com.asset.report.vo.opr;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 租金变更分析 VO
 * <p>
 * 按时间维度聚合变更租金影响额，用于租金调整趋势图和影响量分析。
 * </p>
 */
@Data
@Accessors(chain = true)
public class OprRentChangeVO {

    /** 时间维度标签（MONTH: yyyy-MM） */
    private String timeDim;

    /** 项目ID */
    private Long projectId;

    /** 业态类型（空串=全业态汇总） */
    private String formatType;

    /** 变更租金影响额合计（元） */
    private BigDecimal changeRentImpact;

    /** 变更次数 */
    private Integer changeCount;

    /** 平均每次变更影响额（元） */
    private BigDecimal avgChangeImpact;

    /** 对比期变更租金影响额（同比/环比） */
    private BigDecimal prevChangeRentImpact;

    /** 变更租金影响额增长率（%） */
    private BigDecimal changeRentGrowthRate;
}
