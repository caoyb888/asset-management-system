package com.asset.report.vo.opr;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 合同台账变动 VO（P1）
 * <p>
 * 按时间维度汇总台账变动情况：合同变更次数、变更租金影响额、解约数、即将到期数，
 * 支持同比/环比对比，用于台账变动趋势图和汇总表格。
 * 数据来源：{@code rpt_operation_monthly}（ETL T+1 月汇总）
 * </p>
 */
@Data
@Accessors(chain = true)
public class OprLedgerChangeVO {

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

    /** 本月解约合同数 */
    private Integer terminatedContracts;

    /** 即将到期合同数（90天内，预计算近似值） */
    private Integer expiringContracts;

    /** 对比期合同变更次数（同比/环比） */
    private Integer prevChangeCount;

    /** 合同变更次数增长率（%） */
    private BigDecimal changeCountGrowthRate;
}
