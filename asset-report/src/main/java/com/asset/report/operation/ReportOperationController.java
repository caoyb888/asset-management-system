package com.asset.report.operation;

import com.asset.common.model.R;
import com.asset.report.common.param.ReportQueryParam;
import com.asset.report.common.permission.RptDataScope;
import com.asset.report.vo.opr.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 营运类报表接口
 * <p>
 * 路径前缀：/rpt/opr（不含 /api，由网关/代理统一加前缀）
 * Knife4j 分组：03-营运类报表
 * 所有接口均通过 {@link RptDataScope} 注解自动注入数据权限。
 * 数据来源：{@code rpt_operation_monthly}（ETL T+1 月汇总）
 * </p>
 */
@Tag(name = "营运类报表", description = "基于 rpt_operation_monthly 的营收/变更/到期/客流等营运分析接口")
@RestController
@RequestMapping("/rpt/opr")
@RequiredArgsConstructor
public class ReportOperationController {

    private final ReportOperationService oprService;

    // ==================== P0 接口 ====================

    /**
     * 营运数据看板（聚合接口）
     * <p>
     * 一次返回：核心指标摘要 + 同比 + 合同到期预警 + 近12月营收/客流趋势 + 项目对比。
     * 响应时间目标 &lt; 3s。
     * </p>
     */
    @Operation(summary = "营运数据看板",
            description = "聚合接口，一次返回所有看板图表数据（核心指标/同比/到期预警/趋势/项目对比），减少 HTTP 请求数")
    @GetMapping("/dashboard")
    @RptDataScope
    public R<OprDashboardVO> dashboard(
            @Parameter(description = "查询参数（projectId/statMonth）")
            ReportQueryParam param) {
        return R.ok(oprService.dashboard(param));
    }

    /**
     * 营收填报汇总（支持时间聚合 + 同比/环比）
     * <p>
     * 按月份维度返回营收总额、浮动租金、坪效趋势，
     * compareMode=YOY/MOM 时附带增长率。
     * </p>
     */
    @Operation(summary = "营收填报汇总",
            description = "按月份维度返回营收/浮动租金/坪效趋势，compareMode=YOY/MOM 时附带增长率，支持业态细分")
    @GetMapping("/revenue-summary")
    @RptDataScope
    public R<List<OprRevenueSummaryVO>> revenueSummary(
            @Parameter(description = "查询参数（projectId/formatType/startMonth/endMonth/compareMode）")
            ReportQueryParam param) {
        return R.ok(oprService.revenueSummary(param));
    }

    /**
     * 合同变更统计（支持同比/环比）
     * <p>
     * 按月份维度返回合同变更次数和变更租金影响额，
     * compareMode=YOY/MOM 时附带变更次数增长率。
     * </p>
     */
    @Operation(summary = "合同变更统计",
            description = "按月份维度返回变更次数和租金影响额，compareMode=YOY/MOM 时附带增长率")
    @GetMapping("/contract-changes")
    @RptDataScope
    public R<List<OprContractChangeVO>> contractChanges(
            @Parameter(description = "查询参数（projectId/formatType/startMonth/endMonth/compareMode）")
            ReportQueryParam param) {
        return R.ok(oprService.contractChanges(param));
    }

    /**
     * 租金变更分析（支持同比/环比）
     * <p>
     * 按月份维度返回变更租金影响额汇总和单次均值，
     * compareMode=YOY/MOM 时附带增长率。
     * </p>
     */
    @Operation(summary = "租金变更分析",
            description = "按月份维度返回变更租金影响额和单次均值，compareMode=YOY/MOM 时附带增长率")
    @GetMapping("/rent-changes")
    @RptDataScope
    public R<List<OprRentChangeVO>> rentChanges(
            @Parameter(description = "查询参数（projectId/formatType/startMonth/endMonth/compareMode）")
            ReportQueryParam param) {
        return R.ok(oprService.rentChanges(param));
    }

    /**
     * 合同到期预警
     * <p>
     * 返回各项目 30/60/90 天内即将到期合同数，statMonth 不传时取最新统计月份。
     * 精确天数分档需实时查询 inv_lease_contract（本接口基于预计算近似值）。
     * </p>
     */
    @Operation(summary = "合同到期预警",
            description = "返回各项目30/60/90天内到期合同数（分档预警），statMonth 不传取最新月份")
    @GetMapping("/expiring-contracts")
    @RptDataScope
    public R<List<OprExpiringContractVO>> expiringContracts(
            @Parameter(description = "查询参数（projectId/statMonth）")
            ReportQueryParam param) {
        return R.ok(oprService.expiringContracts(param));
    }

    /**
     * 地区业务对比
     * <p>
     * 按项目维度返回多维运营指标（营收/客流/坪效/变更/解约/到期），
     * 含百分位归一化评分，供前端渲染 ECharts 雷达图或多项目柱状图对比。
     * statMonth 不传时取最新统计月份。
     * </p>
     */
    @Operation(summary = "地区业务对比",
            description = "按项目维度返回多维运营指标（含雷达图评分），statMonth 不传取最新月份")
    @GetMapping("/region-compare")
    @RptDataScope
    public R<List<OprRegionCompareVO>> regionCompare(
            @Parameter(description = "查询参数（projectId/statMonth）")
            ReportQueryParam param) {
        return R.ok(oprService.regionCompare(param));
    }

    // ==================== P1 接口 ====================

    /**
     * 客流数据分析（P1）
     * <p>
     * 按月份维度返回客流量和日均客流，
     * compareMode=YOY/MOM 时附带增长率。
     * </p>
     */
    @Operation(summary = "客流数据分析（P1）",
            description = "按月份维度返回客流量/日均客流，compareMode=YOY/MOM 时附带增长率")
    @GetMapping("/passenger-flow")
    @RptDataScope
    public R<List<OprPassengerFlowVO>> passengerFlow(
            @Parameter(description = "查询参数（projectId/startMonth/endMonth/compareMode）")
            ReportQueryParam param) {
        return R.ok(oprService.passengerFlow(param));
    }

    /**
     * 解约统计（P1）
     * <p>
     * 按月份维度返回解约合同数，
     * compareMode=YOY/MOM 时附带增长率。
     * </p>
     */
    @Operation(summary = "解约统计（P1）",
            description = "按月份维度返回解约合同数，compareMode=YOY/MOM 时附带增长率，支持业态细分")
    @GetMapping("/termination-stats")
    @RptDataScope
    public R<List<OprTerminationStatsVO>> terminationStats(
            @Parameter(description = "查询参数（projectId/formatType/startMonth/endMonth/compareMode）")
            ReportQueryParam param) {
        return R.ok(oprService.terminationStats(param));
    }

    /**
     * 浮动租金统计（P1）
     * <p>
     * 按月份维度返回浮动租金金额，
     * compareMode=YOY/MOM 时附带增长率。
     * </p>
     */
    @Operation(summary = "浮动租金统计（P1）",
            description = "按月份维度返回浮动租金金额，compareMode=YOY/MOM 时附带增长率，支持业态细分")
    @GetMapping("/floating-rent")
    @RptDataScope
    public R<List<OprFloatingRentVO>> floatingRent(
            @Parameter(description = "查询参数（projectId/formatType/startMonth/endMonth/compareMode）")
            ReportQueryParam param) {
        return R.ok(oprService.floatingRent(param));
    }
}
