package com.asset.report.vo.opr;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 解约统计 VO（P1）
 * <p>
 * 按时间维度聚合解约合同数，支持同比/环比对比，用于解约趋势图和统计表格。
 * </p>
 */
@Data
@Accessors(chain = true)
public class OprTerminationStatsVO {

    /** 时间维度标签（MONTH: yyyy-MM） */
    private String timeDim;

    /** 项目ID */
    private Long projectId;

    /** 业态类型（空串=全业态汇总） */
    private String formatType;

    /** 解约合同数 */
    private Integer terminatedContracts;

    /** 对比期解约合同数（同比/环比） */
    private Integer prevTerminatedContracts;

    /** 解约数增长率（%） */
    private BigDecimal growthRate;
}
