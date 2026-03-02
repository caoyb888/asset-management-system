package com.asset.report.vo.opr;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

/**
 * 营运数据看板聚合 VO
 * <p>
 * 一次返回所有看板图表数据，减少 HTTP 请求数。包含：
 * <ul>
 *   <li>核心指标摘要（最新月份汇总）</li>
 *   <li>同比增长率（YoY）</li>
 *   <li>合同到期预警（30/60/90 天分档）</li>
 *   <li>近 12 个月营收 / 客流趋势</li>
 *   <li>项目地区业务对比</li>
 * </ul>
 * </p>
 */
@Data
@Accessors(chain = true)
public class OprDashboardVO {

    /** ETL 最新统计月份（yyyy-MM） */
    private String latestMonth;

    // ==================== 核心指标摘要 ====================

    /** 月营收总额（元） */
    private BigDecimal totalRevenue;

    /** 浮动租金总额（元） */
    private BigDecimal floatingRentAmount;

    /** 坪效（元/㎡） */
    private BigDecimal avgRevenuePerSqm;

    /** 月客流总量（人次） */
    private Long passengerFlow;

    /** 合同变更次数 */
    private Integer changeCount;

    /** 本月解约合同数 */
    private Integer terminatedContracts;

    // ==================== 同比增长率（YoY） ====================

    /** 营收同比增长率（%） */
    private BigDecimal revenueYoY;

    /** 客流同比增长率（%） */
    private BigDecimal passengerFlowYoY;

    /** 坪效同比增长率（%） */
    private BigDecimal avgRevenuePerSqmYoY;

    // ==================== 合同到期预警 ====================

    /** 30天内即将到期合同数 */
    private Integer expiringWithin30;

    /** 60天内即将到期合同数 */
    private Integer expiringWithin60;

    /** 90天内即将到期合同数 */
    private Integer expiringWithin90;

    // ==================== 趋势图数据（近12个月）====================

    /** 营收 12 个月趋势 */
    private List<OprTrendVO> revenueTrend;

    /** 客流 12 个月趋势 */
    private List<OprTrendVO> passengerTrend;

    // ==================== 项目业务对比 ====================

    /** 当前可见项目业务对比数据，用于雷达图/柱状图 */
    private List<OprRegionCompareVO> projectComparison;
}
