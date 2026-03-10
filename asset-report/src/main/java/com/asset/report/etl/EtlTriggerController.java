package com.asset.report.etl;

import com.asset.common.model.R;
import com.asset.report.etl.aging.AgingAnalysisEtlJob;
import com.asset.report.etl.asset.AssetDailyEtlJob;
import com.asset.report.etl.finance.FinanceMonthlyEtlJob;
import com.asset.report.etl.investment.InvestmentDailyEtlJob;
import com.asset.report.etl.operation.OperationMonthlyEtlJob;
import com.xxl.job.core.biz.model.ReturnT;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ETL 手动触发接口
 * <p>
 * 用于 XXL-Job admin 未运行时手动触发 ETL 任务，或补跑历史数据。
 * 生产环境应限制此接口访问权限（admin 角色）。
 * </p>
 *
 * <h3>接口说明</h3>
 * <ul>
 *   <li>POST /rpt/etl/all       - 一键触发全部 ETL（资产日+招商日+财务月+营运月+账龄）</li>
 *   <li>POST /rpt/etl/asset     - 资产日汇总 ETL（statDate: yyyy-MM-dd，空=昨日）</li>
 *   <li>POST /rpt/etl/investment - 招商日汇总 ETL（statDate: yyyy-MM-dd，空=昨日）</li>
 *   <li>POST /rpt/etl/finance   - 财务月汇总 ETL（statMonth: yyyy-MM，空=上月）</li>
 *   <li>POST /rpt/etl/operation - 营运月汇总 ETL（statMonth: yyyy-MM，空=上月）</li>
 *   <li>POST /rpt/etl/aging     - 账龄分析 ETL（statDate: yyyy-MM-dd，空=昨日）</li>
 * </ul>
 */
@Slf4j
@Tag(name = "ETL 手动触发", description = "手动触发报表 ETL 数据聚合（替代 XXL-Job 调度）")
@RestController
@RequestMapping("/rpt/etl")
@RequiredArgsConstructor
public class EtlTriggerController {

    private final AssetDailyEtlJob      assetDailyEtlJob;
    private final InvestmentDailyEtlJob investmentDailyEtlJob;
    private final FinanceMonthlyEtlJob  financeMonthlyEtlJob;
    private final OperationMonthlyEtlJob operationMonthlyEtlJob;
    private final AgingAnalysisEtlJob   agingAnalysisEtlJob;

    /**
     * 一键触发全部 ETL
     *
     * @param statDate  日级统计日期（yyyy-MM-dd，空=昨日）
     * @param statMonth 月级统计月份（yyyy-MM，空=上月）
     */
    @Operation(summary = "一键触发全部 ETL")
    @PostMapping("/all")
    public R<String> triggerAll(
            @Parameter(description = "日级统计日期 yyyy-MM-dd，空=昨日")
            @RequestParam(required = false) String statDate,
            @Parameter(description = "月级统计月份 yyyy-MM，空=上月")
            @RequestParam(required = false) String statMonth) {

        log.info("[ETL-MANUAL] 触发全部 ETL，statDate={} statMonth={}", statDate, statMonth);
        StringBuilder sb = new StringBuilder();

        ReturnT<String> r1 = assetDailyEtlJob.execute(statDate);
        sb.append("asset=").append(r1.getCode() == ReturnT.SUCCESS_CODE ? "OK" : "FAIL").append("; ");

        ReturnT<String> r2 = investmentDailyEtlJob.execute(statDate);
        sb.append("investment=").append(r2.getCode() == ReturnT.SUCCESS_CODE ? "OK" : "FAIL").append("; ");

        ReturnT<String> r3 = financeMonthlyEtlJob.execute(statMonth);
        sb.append("finance=").append(r3.getCode() == ReturnT.SUCCESS_CODE ? "OK" : "FAIL").append("; ");

        ReturnT<String> r4 = operationMonthlyEtlJob.execute(statMonth);
        sb.append("operation=").append(r4.getCode() == ReturnT.SUCCESS_CODE ? "OK" : "FAIL").append("; ");

        ReturnT<String> r5 = agingAnalysisEtlJob.execute(statDate);
        sb.append("aging=").append(r5.getCode() == ReturnT.SUCCESS_CODE ? "OK" : "FAIL");

        boolean allOk = r1.getCode() == ReturnT.SUCCESS_CODE
                && r2.getCode() == ReturnT.SUCCESS_CODE
                && r3.getCode() == ReturnT.SUCCESS_CODE
                && r4.getCode() == ReturnT.SUCCESS_CODE
                && r5.getCode() == ReturnT.SUCCESS_CODE;

        String result = sb.toString();
        log.info("[ETL-MANUAL] 全部ETL结果: {}", result);
        return allOk ? R.ok(result) : R.fail(result);
    }

    /**
     * 资产日汇总 ETL
     */
    @Operation(summary = "资产日汇总 ETL")
    @PostMapping("/asset")
    public R<String> triggerAsset(
            @Parameter(description = "统计日期 yyyy-MM-dd，空=昨日")
            @RequestParam(required = false) String statDate) {
        ReturnT<String> r = assetDailyEtlJob.execute(statDate);
        return r.getCode() == ReturnT.SUCCESS_CODE ? R.ok(r.getMsg()) : R.fail(r.getMsg());
    }

    /**
     * 招商日汇总 ETL
     */
    @Operation(summary = "招商日汇总 ETL")
    @PostMapping("/investment")
    public R<String> triggerInvestment(
            @Parameter(description = "统计日期 yyyy-MM-dd，空=昨日")
            @RequestParam(required = false) String statDate) {
        ReturnT<String> r = investmentDailyEtlJob.execute(statDate);
        return r.getCode() == ReturnT.SUCCESS_CODE ? R.ok(r.getMsg()) : R.fail(r.getMsg());
    }

    /**
     * 财务月汇总 ETL
     */
    @Operation(summary = "财务月汇总 ETL")
    @PostMapping("/finance")
    public R<String> triggerFinance(
            @Parameter(description = "统计月份 yyyy-MM，空=上月")
            @RequestParam(required = false) String statMonth) {
        ReturnT<String> r = financeMonthlyEtlJob.execute(statMonth);
        return r.getCode() == ReturnT.SUCCESS_CODE ? R.ok(r.getMsg()) : R.fail(r.getMsg());
    }

    /**
     * 营运月汇总 ETL
     */
    @Operation(summary = "营运月汇总 ETL")
    @PostMapping("/operation")
    public R<String> triggerOperation(
            @Parameter(description = "统计月份 yyyy-MM，空=上月")
            @RequestParam(required = false) String statMonth) {
        ReturnT<String> r = operationMonthlyEtlJob.execute(statMonth);
        return r.getCode() == ReturnT.SUCCESS_CODE ? R.ok(r.getMsg()) : R.fail(r.getMsg());
    }

    /**
     * 账龄分析 ETL
     */
    @Operation(summary = "账龄分析 ETL")
    @PostMapping("/aging")
    public R<String> triggerAging(
            @Parameter(description = "统计日期 yyyy-MM-dd，空=昨日")
            @RequestParam(required = false) String statDate) {
        ReturnT<String> r = agingAnalysisEtlJob.execute(statDate);
        return r.getCode() == ReturnT.SUCCESS_CODE ? R.ok(r.getMsg()) : R.fail(r.getMsg());
    }
}
