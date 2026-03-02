package com.asset.report.vo.opr;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 浮动租金统计 VO（P1）
 * <p>
 * 按时间维度聚合浮动租金金额，支持同比/环比对比，用于浮动租金趋势图。
 * </p>
 */
@Data
@Accessors(chain = true)
public class OprFloatingRentVO {

    /** 时间维度标签（MONTH: yyyy-MM） */
    private String timeDim;

    /** 项目ID */
    private Long projectId;

    /** 业态类型（空串=全业态汇总） */
    private String formatType;

    /** 浮动租金总额（元） */
    private BigDecimal floatingRentAmount;

    /** 对比期浮动租金总额（同比/环比） */
    private BigDecimal prevFloatingRentAmount;

    /** 浮动租金增长率（%） */
    private BigDecimal growthRate;
}
