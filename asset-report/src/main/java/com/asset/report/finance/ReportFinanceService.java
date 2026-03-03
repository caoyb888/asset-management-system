package com.asset.report.finance;

import com.asset.report.common.param.ReportQueryParam;
import com.asset.report.vo.fin.*;

import java.util.List;

/**
 * 财务类报表 Service 接口
 * <p>
 * 数据来源：{@code rpt_finance_monthly}（T+1 月汇总）和 {@code rpt_aging_analysis}（T+1 日预计算）。
 * </p>
 */
public interface ReportFinanceService {

    // ==================== P0 接口 ====================

    /**
     * 财务数据看板（聚合接口）
     * <p>
     * 一次返回：核心指标摘要 + 同比 + 近12月趋势 + 账龄分布 + 欠款TOP10商家。
     * 响应时间目标 &lt; 3s。
     * </p>
     */
    FinDashboardVO dashboard(ReportQueryParam param);

    /**
     * 应收汇总报表（支持时间趋势、同比/环比）
     * <p>
     * 按月份/项目/费项维度汇总应收、已收、欠款、减免金额及收缴率，
     * compareMode=YOY/MOM 时附带增长率。
     * </p>
     */
    List<FinReceivableSummaryVO> receivableSummary(ReportQueryParam param);

    /**
     * 收款汇总报表（支持时间趋势、同比/环比）
     * <p>
     * 按月份/项目/费项维度汇总已收金额和收缴率趋势，
     * compareMode=YOY/MOM 时附带增长率。
     * </p>
     */
    List<FinReceiptSummaryVO> receiptSummary(ReportQueryParam param);

    /**
     * 欠款统计报表（支持时间趋势、同比/环比）
     * <p>
     * 按月份/项目/费项维度汇总欠款、逾期金额及逾期率，
     * compareMode=YOY/MOM 时附带增长率。
     * </p>
     */
    List<FinOutstandingSummaryVO> outstandingSummary(ReportQueryParam param);

    /**
     * 账龄分析报表
     * <p>
     * 基于 rpt_aging_analysis 预计算表，按商家维度展示欠款账龄分档，
     * 含各分档占比，statDate 不传时取最新统计日期。
     * </p>
     */
    List<FinAgingAnalysisVO> agingAnalysis(ReportQueryParam param);

    /**
     * 逾期率统计（支持时间趋势、同比/环比）
     * <p>
     * 按月份/项目维度展示逾期金额和逾期率趋势，
     * compareMode=YOY/MOM 时附带增长率。
     * </p>
     */
    List<FinOverdueRateVO> overdueRate(ReportQueryParam param);

    /**
     * 收缴率统计（支持时间趋势、同比/环比）
     * <p>
     * 按月份/项目/费项维度展示收缴率趋势，
     * compareMode=YOY/MOM 时附带增长率。
     * </p>
     */
    List<FinCollectionRateVO> collectionRate(ReportQueryParam param);

    // ==================== P1 接口 ====================

    /**
     * 凭证统计（P1）
     * <p>
     * 直接查询 fin_voucher 业务表，按月份/项目维度汇总凭证数量分布和金额。
     * </p>
     */
    List<FinVoucherStatsVO> voucherStats(ReportQueryParam param);

    /**
     * 保证金汇总（P1）
     * <p>
     * 基于 rpt_finance_monthly 的 deposit_balance 字段，
     * 按月份/项目展示保证金余额趋势，compareMode=YOY/MOM 时附带增长率。
     * </p>
     */
    List<FinDepositSummaryVO> depositSummary(ReportQueryParam param);

    /**
     * 预收款汇总（P1）
     * <p>
     * 基于 rpt_finance_monthly 的 prepay_balance 字段，
     * 按月份/项目展示预收款余额趋势，compareMode=YOY/MOM 时附带增长率。
     * </p>
     */
    List<FinPrepaySummaryVO> prepaySummary(ReportQueryParam param);

    /**
     * 减免/调整统计（P1）
     * <p>
     * 按月份/项目/费项维度统计减免和调整金额及其占应收比例，
     * compareMode=YOY/MOM 时附带增长率。
     * </p>
     */
    List<FinDeductionAdjustmentVO> deductionAdjustment(ReportQueryParam param);
}
