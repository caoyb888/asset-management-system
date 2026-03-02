package com.asset.report.vo.opr;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 合同变更统计 VO
 * <p>
 * 按时间维度聚合合同变更次数和租金影响额，用于变更趋势折线图和分布分析。
 * </p>
 */
@Data
@Accessors(chain = true)
public class OprContractChangeVO {

    /** 时间维度标签（MONTH: yyyy-MM） */
    private String timeDim;

    /** 项目ID */
    private Long projectId;

    /** 业态类型（空串=全业态汇总） */
    private String formatType;

    /** 合同变更次数 */
    private Integer changeCount;

    /** 变更租金影响额（元，正=涨租，负=降租） */
    private BigDecimal changeRentImpact;

    /** 对比期变更次数（同比/环比） */
    private Integer prevChangeCount;

    /** 变更次数增长率（%） */
    private BigDecimal changeCountGrowthRate;
}
