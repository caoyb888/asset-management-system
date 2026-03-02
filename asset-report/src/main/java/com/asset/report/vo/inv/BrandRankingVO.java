package com.asset.report.vo.inv;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 品牌签约排行 VO（P1）
 * <p>
 * 按业态（format_type）分组统计签约数据，按合同数量或签约面积排行，
 * 用于招商看板的品牌签约排行榜。
 * </p>
 */
@Data
@Accessors(chain = true)
public class BrandRankingVO {

    /** 排名（前端可通过列表顺序确定，此字段为预计算排名便于直接展示） */
    private Integer rank;

    /** 业态类型（如：零售/餐饮/娱乐/服务） */
    private String formatType;

    /** 项目ID（按项目维度时使用） */
    private Long projectId;

    /** 合同数量 */
    private Integer contractCount;

    /** 签约面积（㎡） */
    private BigDecimal contractArea;

    /** 合同总金额（元） */
    private BigDecimal contractAmount;

    /** 平均租金单价（元/㎡/月） */
    private BigDecimal avgRentPrice;

    /** 意向转化率（%） */
    private BigDecimal conversionRate;

    /** 签约面积占总签约面积百分比（%） */
    private BigDecimal areaPercentage;

    /** 合同数量占总合同数百分比（%） */
    private BigDecimal countPercentage;
}
