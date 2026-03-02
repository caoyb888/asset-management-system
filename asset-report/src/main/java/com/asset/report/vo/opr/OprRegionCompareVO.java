package com.asset.report.vo.opr;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 地区业务对比 VO（雷达图格式）
 * <p>
 * 按项目维度返回多维运营指标，供前端渲染 ECharts 雷达图或多项目柱状图对比。
 * 各指标均归一化为 0-100 的评分（百分位数方式），便于多维雷达图展示。
 * </p>
 */
@Data
@Accessors(chain = true)
public class OprRegionCompareVO {

    /** 项目ID */
    private Long projectId;

    /** 统计月份（yyyy-MM） */
    private String statMonth;

    /** 月营收总额（元） */
    private BigDecimal revenueAmount;

    /** 月客流总量（人次） */
    private Long passengerFlow;

    /** 合同变更次数 */
    private Integer changeCount;

    /** 变更租金影响额（元） */
    private BigDecimal changeRentImpact;

    /** 本月解约合同数 */
    private Integer terminatedContracts;

    /** 坪效（元/㎡） */
    private BigDecimal avgRevenuePerSqm;

    /** 即将到期合同数（90天内） */
    private Integer expiringContracts;

    // ===== 归一化评分（0-100，用于雷达图轴） =====

    /** 营收评分（百分位） */
    private BigDecimal revenueScore;

    /** 客流评分（百分位） */
    private BigDecimal passengerScore;

    /** 坪效评分（百分位） */
    private BigDecimal avgRevenueScore;
}
