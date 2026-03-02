package com.asset.report.vo.inv;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 合同租赁情况统计 VO
 * <p>
 * 按时间维度聚合合同相关指标，用于趋势图/统计表格。
 * </p>
 */
@Data
@Accessors(chain = true)
public class ContractStatsVO {

    /** 时间维度标签（DAY: yyyy-MM-dd / WEEK: yyyy-ww / MONTH: yyyy-MM / YEAR: yyyy） */
    private String timeDim;

    /** 租赁合同数（累计有效） */
    private Integer contractCount;

    /** 合同总金额（元） */
    private BigDecimal contractAmount;

    /** 签约面积（㎡） */
    private BigDecimal contractArea;

    /** 当期新增合同 */
    private Integer newContract;

    /** 意向转化率（%）= contractCount / intentionCount */
    private BigDecimal conversionRate;

    /** 平均租金单价（元/㎡/月） */
    private BigDecimal avgRentPrice;

    /** 对比期合同数（同比/环比） */
    private Integer prevContractCount;

    /** 合同数增长率（%） */
    private BigDecimal growthRate;
}
