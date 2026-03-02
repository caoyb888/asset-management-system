package com.asset.report.vo.inv;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 意向客户统计 VO
 * <p>
 * 按时间维度聚合意向相关指标，用于趋势图/统计表格。
 * </p>
 */
@Data
@Accessors(chain = true)
public class IntentionStatsVO {

    /** 时间维度标签（DAY: yyyy-MM-dd / WEEK: yyyy-ww / MONTH: yyyy-MM / YEAR: yyyy） */
    private String timeDim;

    /** 意向协议数（累计有效） */
    private Integer intentionCount;

    /** 已签意向数（已缴意向金） */
    private Integer intentionSigned;

    /** 当期新增意向 */
    private Integer newIntention;

    /** 意向签约率（%）= intentionSigned / intentionCount */
    private BigDecimal signedRate;

    /** 对比期意向数（同比/环比） */
    private Integer prevIntentionCount;

    /** 意向数增长率（%） */
    private BigDecimal growthRate;
}
