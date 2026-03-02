package com.asset.report.investment;

import com.asset.common.model.R;
import com.asset.report.common.param.ReportQueryParam;
import com.asset.report.common.permission.RptDataScope;
import com.asset.report.vo.inv.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 招商类报表接口
 * <p>
 * 路径前缀：/rpt/inv（不含 /api，由网关/代理统一加前缀）
 * Knife4j 分组：02-招商类报表
 * 所有接口均通过 {@link RptDataScope} 注解自动注入数据权限。
 * 数据来源：{@code rpt_investment_daily}（ETL T+1 日汇总）
 * </p>
 */
@Tag(name = "招商类报表", description = "基于 rpt_investment_daily 的意向统计/漏斗/合同/业绩等招商分析接口")
@RestController
@RequestMapping("/rpt/inv")
@RequiredArgsConstructor
public class ReportInvestmentController {

    private final ReportInvestmentService invService;

    // ==================== P0 接口 ====================

    /**
     * 招商数据看板（聚合接口）
     * <p>
     * 一次返回：核心指标摘要 + 同比 + 漏斗数据 + 近 30 天新增趋势 + 项目业绩对比。
     * 响应时间目标 &lt; 3s。
     * </p>
     */
    @Operation(summary = "招商数据看板",
            description = "聚合接口，一次返回所有看板图表数据（核心指标/漏斗/趋势/业绩对比），减少 HTTP 请求数")
    @GetMapping("/dashboard")
    @RptDataScope
    public R<InvDashboardVO> dashboard(
            @Parameter(description = "查询参数（projectId/startDate/endDate/compareMode）")
            ReportQueryParam param) {
        return R.ok(invService.dashboard(param));
    }

    /**
     * 意向客户统计（支持时间趋势、同比/环比）
     * <p>
     * 按时间维度（DAY/WEEK/MONTH/YEAR）返回意向协议数、已签意向数、新增意向量趋势，
     * compareMode=YOY/MOM 时附带增长率。
     * </p>
     */
    @Operation(summary = "意向客户统计",
            description = "按时间维度返回意向数量趋势及签约率，compareMode=YOY/MOM 时附带同比/环比增长率")
    @GetMapping("/intention-stats")
    @RptDataScope
    public R<List<IntentionStatsVO>> intentionStats(
            @Parameter(description = "查询参数（projectId/formatType/investmentManagerId/startDate/endDate/timeUnit/compareMode）")
            ReportQueryParam param) {
        return R.ok(invService.intentionStats(param));
    }

    /**
     * 客户跟进漏斗数据
     * <p>
     * 返回三阶段漏斗：意向登记 → 已签意向 → 已签合同，含各阶段数量和转化率。
     * statDate 不传时取最新统计日期。
     * </p>
     */
    @Operation(summary = "客户跟进漏斗数据",
            description = "返回意向登记→已签意向→已签合同三阶段漏斗数据及转化率，statDate 不传时取最新日期")
    @GetMapping("/funnel")
    @RptDataScope
    public R<List<FunnelVO>> funnel(
            @Parameter(description = "查询参数（projectId/statDate）")
            ReportQueryParam param) {
        return R.ok(invService.funnel(param));
    }

    /**
     * 合同租赁情况（支持时间趋势、同比/环比）
     * <p>
     * 按时间维度返回合同数量、签约面积、合同金额、新增合同趋势，
     * compareMode=YOY/MOM 时附带增长率。
     * </p>
     */
    @Operation(summary = "合同租赁情况",
            description = "按时间维度返回合同数量/签约面积/金额/转化率趋势，compareMode=YOY/MOM 时附带增长率")
    @GetMapping("/contract-stats")
    @RptDataScope
    public R<List<ContractStatsVO>> contractStats(
            @Parameter(description = "查询参数（projectId/formatType/investmentManagerId/startDate/endDate/timeUnit/compareMode）")
            ReportQueryParam param) {
        return R.ok(invService.contractStats(param));
    }

    /**
     * 招商业绩显差看板
     * <p>
     * 按项目或招商负责人维度对比意向数、合同数、转化率、签约面积等业绩指标。
     * 传入 investmentManagerId 可查询指定人员业绩；不传则返回各项目整体业绩。
     * statDate 不传时取最新统计日期。
     * </p>
     */
    @Operation(summary = "招商业绩显差看板",
            description = "按项目/招商负责人维度对比业绩，传 investmentManagerId 查指定人员，不传取项目整体，statDate 不传取最新日期")
    @GetMapping("/performance")
    @RptDataScope
    public R<List<PerformanceVO>> performance(
            @Parameter(description = "查询参数（projectId/investmentManagerId/statDate）")
            ReportQueryParam param) {
        return R.ok(invService.performance(param));
    }

    // ==================== P1 接口 ====================

    /**
     * 租金水平分析（P1）
     * <p>
     * 按项目+业态维度展示平均租金单价，含同比增长率，
     * 可用于渲染均价热力图和楼层/业态分组柱状图。
     * statDate 不传时取最新统计日期。
     * </p>
     */
    @Operation(summary = "租金水平分析（P1）",
            description = "按项目+业态展示平均租金单价及同比增长，可用于均价热力图/业态柱状图，statDate 不传取最新日期")
    @GetMapping("/rent-level")
    @RptDataScope
    public R<List<RentLevelVO>> rentLevel(
            @Parameter(description = "查询参数（projectId/investmentManagerId/statDate）")
            ReportQueryParam param) {
        return R.ok(invService.rentLevel(param));
    }

    /**
     * 租决政策执行报表（P1）
     * <p>
     * 按项目+业态展示实际平均租金执行情况，对比历史同期数据，
     * 计算偏差率（正数=上涨，负数=下降），反映租决政策落地效果。
     * statDate 不传时取最新统计日期。
     * </p>
     */
    @Operation(summary = "租决政策执行报表（P1）",
            description = "按项目+业态展示实际租金执行情况及偏差率（与去年同期对比），statDate 不传取最新日期")
    @GetMapping("/policy-execution")
    @RptDataScope
    public R<List<PolicyExecutionVO>> policyExecution(
            @Parameter(description = "查询参数（projectId/statDate）")
            ReportQueryParam param) {
        return R.ok(invService.policyExecution(param));
    }

    /**
     * 品牌签约排行（P1）
     * <p>
     * 按业态分组统计签约数量/面积/金额，按合同数量降序排行，
     * 含签约面积占比和合同数量占比，用于看板品牌排行榜。
     * statDate 不传时取最新统计日期。
     * </p>
     */
    @Operation(summary = "品牌签约排行（P1）",
            description = "按业态分组统计签约数据并排名，含面积占比和数量占比，statDate 不传取最新日期")
    @GetMapping("/brand-ranking")
    @RptDataScope
    public R<List<BrandRankingVO>> brandRanking(
            @Parameter(description = "查询参数（projectId/statDate）")
            ReportQueryParam param) {
        return R.ok(invService.brandRanking(param));
    }
}
