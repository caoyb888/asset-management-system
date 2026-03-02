package com.asset.report.vo.inv;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 招商数据看板聚合 VO
 * <p>
 * 一次返回所有图表数据，减少 HTTP 请求数。包含：
 * <ul>
 *   <li>核心指标摘要（最新日期汇总）</li>
 *   <li>同比增长率（YoY）</li>
 *   <li>客户跟进漏斗数据</li>
 *   <li>近 30 天新增趋势（意向 / 合同）</li>
 *   <li>项目业绩对比数据</li>
 * </ul>
 * </p>
 */
@Data
@Accessors(chain = true)
public class InvDashboardVO {

    /** ETL 最新统计日期 */
    private LocalDate latestDate;

    // ==================== 核心指标摘要 ====================

    /** 意向协议数（累计有效） */
    private Integer intentionCount;

    /** 已签意向数（已缴意向金） */
    private Integer intentionSigned;

    /** 当日新增意向 */
    private Integer newIntentionToday;

    /** 租赁合同数（累计有效） */
    private Integer contractCount;

    /** 合同总金额（元） */
    private BigDecimal contractAmount;

    /** 签约面积（㎡） */
    private BigDecimal contractArea;

    /** 当日新增合同 */
    private Integer newContractToday;

    /** 意向转化率（%）= contract_count / intention_count */
    private BigDecimal conversionRate;

    /** 平均租金单价（元/㎡/月） */
    private BigDecimal avgRentPrice;

    // ==================== 同比增长率（YoY） ====================

    /** 合同数同比（%），无法计算时为 null */
    private BigDecimal contractCountYoY;

    /** 合同金额同比（%） */
    private BigDecimal contractAmountYoY;

    /** 平均租金同比（%） */
    private BigDecimal avgRentPriceYoY;

    /** 转化率同比（%） */
    private BigDecimal conversionRateYoY;

    // ==================== 漏斗数据 ====================

    /**
     * 客户跟进漏斗各阶段数据
     * 阶段：意向登记 → 已签意向 → 已签合同
     */
    private List<FunnelVO> funnel;

    // ==================== 趋势图数据（最近 30 天，日粒度） ====================

    /** 意向新增 30 天趋势 */
    private List<InvTrendVO> intentionTrend;

    /** 合同新增 30 天趋势 */
    private List<InvTrendVO> contractTrend;

    // ==================== 项目对比（看板柱状图） ====================

    /** 当前可见项目业绩列表，用于项目对比柱状图 */
    private List<PerformanceVO> projectComparison;
}
