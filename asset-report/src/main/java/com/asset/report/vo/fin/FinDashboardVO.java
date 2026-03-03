package com.asset.report.vo.fin;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

/**
 * 财务数据看板聚合 VO
 * <p>
 * 看板接口 GET /rpt/fin/dashboard 一次返回所有图表数据，减少 HTTP 请求数。
 * </p>
 */
@Data
@Accessors(chain = true)
public class FinDashboardVO {

    /** 最新统计月份（YYYY-MM） */
    private String latestMonth;

    // ==================== 核心指标卡片 ====================

    /** 应收总额（元） */
    private BigDecimal totalReceivable;

    /** 已收总额（元） */
    private BigDecimal totalReceived;

    /** 欠款总额（元） */
    private BigDecimal totalOutstanding;

    /** 逾期总额（元） */
    private BigDecimal totalOverdue;

    /** 平均收缴率（%） */
    private BigDecimal avgCollectionRate;

    /** 平均逾期率（%） */
    private BigDecimal avgOverdueRate;

    /** 保证金余额总计（元） */
    private BigDecimal totalDepositBalance;

    /** 预收款余额总计（元） */
    private BigDecimal totalPrepayBalance;

    // ==================== 同比（去年同月）====================

    /** 应收同比增长率（%） */
    private BigDecimal receivableYoY;

    /** 已收同比增长率（%） */
    private BigDecimal receivedYoY;

    /** 收缴率同比变化（百分点） */
    private BigDecimal collectionRateYoY;

    /** 逾期率同比变化（百分点） */
    private BigDecimal overdueRateYoY;

    // ==================== 近 12 个月趋势 ====================

    /** 财务趋势（应收/已收/欠款/收缴率 月度趋势） */
    private List<FinTrendVO> financeTrend;

    // ==================== 账龄分布（看板展示）====================

    /** 账龄分布汇总（最新日期的全项目汇总） */
    private FinAgingAnalysisVO agingSummary;

    // ==================== 欠款 TOP10 商家 ====================

    /** 欠款最多的商家列表（最新统计日期） */
    private List<FinAgingAnalysisVO> overdueTop10;
}
