package com.asset.report.vo.inv;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 租决政策执行报表 VO（P1）
 * <p>
 * 基于 rpt_investment_daily 汇总各项目/业态的租金执行水平，
 * 反映租决政策的落地执行情况（实际均价 vs. 历史均价对比）。
 * </p>
 */
@Data
@Accessors(chain = true)
public class PolicyExecutionVO {

    /** 项目ID */
    private Long projectId;

    /** 业态类型 */
    private String formatType;

    /** 合同数量 */
    private Integer contractCount;

    /** 签约面积（㎡） */
    private BigDecimal contractArea;

    /** 合同总金额（元） */
    private BigDecimal contractAmount;

    /** 实际平均租金单价（元/㎡/月） */
    private BigDecimal avgRentPrice;

    /** 意向转化率（%） */
    private BigDecimal conversionRate;

    /** 当期新增意向 */
    private Integer newIntention;

    /** 当期新增合同 */
    private Integer newContract;

    /** 同比平均租金（元/㎡/月） */
    private BigDecimal prevAvgRentPrice;

    /**
     * 租金执行偏差率（%）= (avgRentPrice - prevAvgRentPrice) / prevAvgRentPrice × 100
     * 正数=上涨，负数=下降，null=无基准期数据
     */
    private BigDecimal rentVarianceRate;
}
