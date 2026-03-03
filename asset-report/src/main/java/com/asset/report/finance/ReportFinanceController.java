package com.asset.report.finance;

import com.asset.common.model.R;
import com.asset.report.common.param.ReportQueryParam;
import com.asset.report.common.permission.RptDataScope;
import com.asset.report.vo.fin.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 财务类报表接口
 * <p>
 * 路径前缀：/rpt/fin（不含 /api，由网关/代理统一加前缀）
 * Knife4j 分组：04-财务类报表
 * 所有接口均通过 {@link RptDataScope} 注解自动注入数据权限。
 * 数据来源：
 * <ul>
 *   <li>{@code rpt_finance_monthly}（ETL T+1 月汇总）- P0/P1 大部分接口</li>
 *   <li>{@code rpt_aging_analysis}（ETL T+1 日预计算）- 账龄分析接口</li>
 *   <li>{@code fin_voucher}（业务表实时查询）- 凭证统计接口（P1）</li>
 * </ul>
 * </p>
 */
@Tag(name = "财务类报表", description = "基于 rpt_finance_monthly / rpt_aging_analysis 的应收/收款/欠款/账龄/收缴率等财务分析接口")
@RestController
@RequestMapping("/rpt/fin")
@RequiredArgsConstructor
public class ReportFinanceController {

    private final ReportFinanceService finService;

    // ==================== P0 接口 ====================

    /**
     * 财务数据看板（聚合接口）
     * <p>
     * 一次返回：核心指标摘要 + 同比 + 近12月趋势 + 账龄分布 + 欠款TOP10商家。
     * 响应时间目标 &lt; 3s。
     * </p>
     */
    @Operation(summary = "财务数据看板",
            description = "聚合接口，一次返回所有看板图表数据（应收/已收/欠款/逾期指标 + 近12月趋势 + 账龄分布 + 欠款TOP10）")
    @GetMapping("/dashboard")
    @RptDataScope
    public R<FinDashboardVO> dashboard(
            @Parameter(description = "查询参数（projectId/statMonth）")
            ReportQueryParam param) {
        return R.ok(finService.dashboard(param));
    }

    /**
     * 应收汇总报表（支持时间趋势、同比/环比）
     * <p>
     * 按月份/项目/费项维度汇总应收、已收、欠款、减免金额及收缴率，
     * compareMode=YOY/MOM 时附带增长率。
     * feeItemType 不传时返回全费项汇总行。
     * </p>
     */
    @Operation(summary = "应收汇总报表",
            description = "按月份/项目/费项维度汇总应收/已收/欠款/减免/收缴率，compareMode=YOY/MOM 时附带增长率，feeItemType 不传取全费项汇总")
    @GetMapping("/receivable-summary")
    @RptDataScope
    public R<List<FinReceivableSummaryVO>> receivableSummary(
            @Parameter(description = "查询参数（projectId/feeItemType/startMonth/endMonth/compareMode）")
            ReportQueryParam param) {
        return R.ok(finService.receivableSummary(param));
    }

    /**
     * 收款汇总报表（支持时间趋势、同比/环比）
     * <p>
     * 按月份/项目/费项维度汇总已收金额和收缴率趋势，
     * compareMode=YOY/MOM 时附带增长率，feeItemType 不传时返回全费项汇总行。
     * </p>
     */
    @Operation(summary = "收款汇总报表",
            description = "按月份/项目/费项维度汇总已收金额和收缴率，compareMode=YOY/MOM 时附带增长率，feeItemType 不传取全费项汇总")
    @GetMapping("/receipt-summary")
    @RptDataScope
    public R<List<FinReceiptSummaryVO>> receiptSummary(
            @Parameter(description = "查询参数（projectId/feeItemType/startMonth/endMonth/compareMode）")
            ReportQueryParam param) {
        return R.ok(finService.receiptSummary(param));
    }

    /**
     * 欠款统计报表（支持时间趋势、同比/环比）
     * <p>
     * 按月份/项目/费项维度汇总欠款、逾期金额及逾期率，
     * compareMode=YOY/MOM 时附带增长率。
     * 可用于渲染欠款汇总表和账龄分布堆叠柱状图。
     * </p>
     */
    @Operation(summary = "欠款统计报表",
            description = "按月份/项目/费项维度汇总欠款/逾期金额及逾期率，compareMode=YOY/MOM 时附带增长率，可用于堆叠柱状图")
    @GetMapping("/outstanding-summary")
    @RptDataScope
    public R<List<FinOutstandingSummaryVO>> outstandingSummary(
            @Parameter(description = "查询参数（projectId/feeItemType/startMonth/endMonth/compareMode）")
            ReportQueryParam param) {
        return R.ok(finService.outstandingSummary(param));
    }

    /**
     * 账龄分析报表
     * <p>
     * 基于 rpt_aging_analysis 预计算表，按商家/合同维度展示欠款账龄分档及占比，
     * statDate 不传时取最新统计日期；merchantId 不传时返回所有商家。
     * 钻取至明细层需调用 fin-service 的 /fin/receivables 接口。
     * </p>
     */
    @Operation(summary = "账龄分析报表",
            description = "按商家/合同维度展示欠款账龄分档（30/60/90/180/365天）及各档占比，statDate 不传取最新日期，merchantId 不传返回所有商家")
    @GetMapping("/aging-analysis")
    @RptDataScope
    public R<List<FinAgingAnalysisVO>> agingAnalysis(
            @Parameter(description = "查询参数（projectId/merchantId/statDate）")
            ReportQueryParam param) {
        return R.ok(finService.agingAnalysis(param));
    }

    /**
     * 逾期率统计（支持时间趋势、同比/环比）
     * <p>
     * 按月份/项目维度展示逾期金额和逾期率趋势，
     * compareMode=YOY/MOM 时附带增长率，可用于渲染逾期率折线图。
     * </p>
     */
    @Operation(summary = "逾期率统计",
            description = "按月份/项目维度展示逾期金额和逾期率趋势，compareMode=YOY/MOM 时附带增长率")
    @GetMapping("/overdue-rate")
    @RptDataScope
    public R<List<FinOverdueRateVO>> overdueRate(
            @Parameter(description = "查询参数（projectId/startMonth/endMonth/compareMode）")
            ReportQueryParam param) {
        return R.ok(finService.overdueRate(param));
    }

    /**
     * 收缴率统计（支持时间趋势、同比/环比）
     * <p>
     * 按月份/项目/费项维度展示收缴率趋势，
     * compareMode=YOY/MOM 时附带增长率，
     * 可用于渲染月度收缴率折线图和项目对比柱状图。
     * </p>
     */
    @Operation(summary = "收缴率统计",
            description = "按月份/项目/费项维度展示收缴率趋势，compareMode=YOY/MOM 时附带增长率，可用于折线图和项目对比柱状图")
    @GetMapping("/collection-rate")
    @RptDataScope
    public R<List<FinCollectionRateVO>> collectionRate(
            @Parameter(description = "查询参数（projectId/feeItemType/startMonth/endMonth/compareMode）")
            ReportQueryParam param) {
        return R.ok(finService.collectionRate(param));
    }

    // ==================== P1 接口 ====================

    /**
     * 凭证统计（P1）
     * <p>
     * 直接查询 fin_voucher 业务表，按月份/项目维度汇总凭证总数、各状态数量、
     * 借贷方合计，可用于查看凭证生成和审核进度。
     * </p>
     */
    @Operation(summary = "凭证统计（P1）",
            description = "按月份/项目维度汇总凭证总数、待审核/已审核/已上传数量及借贷方合计，直接查询 fin_voucher 实时数据")
    @GetMapping("/voucher-stats")
    @RptDataScope
    public R<List<FinVoucherStatsVO>> voucherStats(
            @Parameter(description = "查询参数（projectId/startMonth/endMonth）")
            ReportQueryParam param) {
        return R.ok(finService.voucherStats(param));
    }

    /**
     * 保证金汇总（P1）
     * <p>
     * 基于 rpt_finance_monthly 的 deposit_balance 字段，
     * 按月份/项目维度展示保证金余额趋势，compareMode=YOY/MOM 时附带增长率。
     * </p>
     */
    @Operation(summary = "保证金汇总（P1）",
            description = "按月份/项目维度展示保证金余额月末快照趋势，compareMode=YOY/MOM 时附带增长率")
    @GetMapping("/deposit-summary")
    @RptDataScope
    public R<List<FinDepositSummaryVO>> depositSummary(
            @Parameter(description = "查询参数（projectId/startMonth/endMonth/compareMode）")
            ReportQueryParam param) {
        return R.ok(finService.depositSummary(param));
    }

    /**
     * 预收款汇总（P1）
     * <p>
     * 基于 rpt_finance_monthly 的 prepay_balance 字段，
     * 按月份/项目维度展示预收款余额趋势，compareMode=YOY/MOM 时附带增长率。
     * </p>
     */
    @Operation(summary = "预收款汇总（P1）",
            description = "按月份/项目维度展示预收款余额月末趋势，compareMode=YOY/MOM 时附带增长率")
    @GetMapping("/prepay-summary")
    @RptDataScope
    public R<List<FinPrepaySummaryVO>> prepaySummary(
            @Parameter(description = "查询参数（projectId/startMonth/endMonth/compareMode）")
            ReportQueryParam param) {
        return R.ok(finService.prepaySummary(param));
    }

    /**
     * 减免/调整统计（P1）
     * <p>
     * 按月份/项目/费项维度统计减免金额、调整金额及其占应收比例，
     * compareMode=YOY/MOM 时附带增长率，反映优惠政策执行情况。
     * </p>
     */
    @Operation(summary = "减免/调整统计（P1）",
            description = "按月份/项目/费项维度统计减免额和调整额及占应收比，compareMode=YOY/MOM 时附带增长率")
    @GetMapping("/deduction-adjustment")
    @RptDataScope
    public R<List<FinDeductionAdjustmentVO>> deductionAdjustment(
            @Parameter(description = "查询参数（projectId/feeItemType/startMonth/endMonth/compareMode）")
            ReportQueryParam param) {
        return R.ok(finService.deductionAdjustment(param));
    }
}
