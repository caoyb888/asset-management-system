package com.asset.report.investment;

import com.asset.report.common.param.ReportQueryParam;
import com.asset.report.vo.inv.*;

import java.util.List;

/**
 * 招商类报表 Service 接口
 * <p>
 * 对应 8 个 API：
 * <ul>
 *   <li>P0: dashboard / intentionStats / funnel / contractStats / performance</li>
 *   <li>P1: rentLevel / policyExecution / brandRanking</li>
 * </ul>
 * 所有方法通过 {@link com.asset.report.common.permission.ReportPermissionContext} 读取当前用户可见项目范围。
 * </p>
 */
public interface ReportInvestmentService {

    /**
     * 招商数据看板（聚合接口）
     * <p>
     * 一次返回核心指标摘要 + 漏斗数据 + 近 30 天趋势 + 同比/环比 + 项目业绩对比，
     * 减少前端多次 HTTP 请求。响应时间目标 &lt; 3s。
     * </p>
     *
     * @param param 查询参数（projectId/startDate/endDate/compareMode）
     * @return 招商看板聚合数据
     */
    InvDashboardVO dashboard(ReportQueryParam param);

    /**
     * 意向客户统计（支持趋势、同比/环比）
     *
     * @param param 查询参数（projectId/formatType/managerId/startDate/endDate/timeUnit/compareMode）
     * @return 时间序列意向统计数据点列表
     */
    List<IntentionStatsVO> intentionStats(ReportQueryParam param);

    /**
     * 客户跟进漏斗数据
     * <p>
     * 返回三个漏斗阶段：意向登记 → 已签意向 → 已签合同，含各阶段转化率。
     * statDate 不传时使用最新统计日期。
     * </p>
     *
     * @param param 查询参数（projectId/statDate）
     * @return 漏斗阶段列表（固定 3 条，按阶段顺序）
     */
    List<FunnelVO> funnel(ReportQueryParam param);

    /**
     * 合同租赁情况（支持趋势、同比/环比）
     *
     * @param param 查询参数（projectId/formatType/managerId/startDate/endDate/timeUnit/compareMode）
     * @return 时间序列合同统计数据点列表
     */
    List<ContractStatsVO> contractStats(ReportQueryParam param);

    /**
     * 招商业绩显差看板
     * <p>
     * 按项目或招商负责人维度对比业绩数据。
     * statDate 不传时使用最新统计日期。
     * </p>
     *
     * @param param 查询参数（projectId/investmentManagerId/statDate）
     * @return 业绩数据列表
     */
    List<PerformanceVO> performance(ReportQueryParam param);

    /**
     * 租金水平分析（P1）
     * <p>
     * 按项目+业态维度展示平均租金单价，含同比增长率。
     * statDate 不传时使用最新统计日期。
     * </p>
     *
     * @param param 查询参数（projectId/investmentManagerId/statDate）
     * @return 租金水平列表（含同比）
     */
    List<RentLevelVO> rentLevel(ReportQueryParam param);

    /**
     * 租决政策执行报表（P1）
     * <p>
     * 按项目+业态展示实际租金执行水平，对比历史同期，反映政策落地情况。
     * statDate 不传时使用最新统计日期。
     * </p>
     *
     * @param param 查询参数（projectId/statDate）
     * @return 政策执行列表（含偏差率）
     */
    List<PolicyExecutionVO> policyExecution(ReportQueryParam param);

    /**
     * 品牌签约排行（P1）
     * <p>
     * 按业态分组统计签约数据，按合同数量排行，含面积占比和数量占比。
     * statDate 不传时使用最新统计日期。
     * </p>
     *
     * @param param 查询参数（projectId/statDate）
     * @return 品牌签约排行列表（含占比）
     */
    List<BrandRankingVO> brandRanking(ReportQueryParam param);
}
