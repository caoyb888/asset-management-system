package com.asset.report.investment;

import com.asset.report.common.param.ReportQueryParam;
import com.asset.report.common.param.ReportQueryParam.CompareMode;
import com.asset.report.common.permission.ReportPermissionContext;
import com.asset.report.common.util.PeriodCompareUtil;
import com.asset.report.config.ReportCacheConfig;
import com.asset.report.entity.RptInvestmentDaily;
import com.asset.report.mapper.rpt.RptInvestmentDailyMapper;
import com.asset.report.vo.inv.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 招商类报表 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportInvestmentServiceImpl implements ReportInvestmentService {

    private final RptInvestmentDailyMapper invDailyMapper;

    // ==================== 看板（P0） ====================

    @Override
    @Cacheable(
        cacheManager = "reportCacheManager",
        value = ReportCacheConfig.CACHE_DASHBOARD,
        key = "'inv:' + (#param.projectId ?: 'ALL') + ':' + T(com.asset.report.common.permission.ReportPermissionContext).getCacheKey()",
        condition = "!T(com.asset.report.common.permission.ReportPermissionContext).hasNoPermission()",
        unless = "#result == null"
    )
    public InvDashboardVO dashboard(ReportQueryParam param) {
        List<Long> permIds = ReportPermissionContext.get();
        if (ReportPermissionContext.hasNoPermission()) {
            return new InvDashboardVO();
        }

        // 1. 确定最新统计日期
        LocalDate latestDate = invDailyMapper.selectMaxStatDate(param.getProjectId(), permIds);
        if (latestDate == null) {
            log.warn("[InvDashboard] rpt_investment_daily 无数据，返回空看板");
            return new InvDashboardVO();
        }

        // 2. 并行执行 5 个子查询
        LocalDate trendStart = latestDate.minusDays(29);
        LocalDate yoyDate    = latestDate.minusYears(1);
        Long      projectId  = param.getProjectId();

        CompletableFuture<List<RptInvestmentDaily>> cfSummary    = CompletableFuture.supplyAsync(
                () -> invDailyMapper.selectProjectSummaryByDate(latestDate, projectId, permIds));
        CompletableFuture<List<RptInvestmentDaily>> cfYoy        = CompletableFuture.supplyAsync(
                () -> invDailyMapper.selectProjectSummaryByDate(yoyDate, projectId, permIds));
        CompletableFuture<List<FunnelVO>>           cfFunnel     = CompletableFuture.supplyAsync(
                () -> buildFunnel(latestDate, projectId, permIds));
        CompletableFuture<List<InvTrendVO>>         cfTrend      = CompletableFuture.supplyAsync(
                () -> invDailyMapper.selectInvTrend(projectId, null, null, trendStart, latestDate, "DAY", permIds));
        CompletableFuture<List<PerformanceVO>>      cfPerformance = CompletableFuture.supplyAsync(
                () -> invDailyMapper.selectPerformance(latestDate, projectId, null, permIds));

        CompletableFuture.allOf(cfSummary, cfYoy, cfFunnel, cfTrend, cfPerformance).join();

        // 3. 组装核心指标
        InvDashboardVO vo = new InvDashboardVO().setLatestDate(latestDate);
        List<RptInvestmentDaily> summaries = cfSummary.join();
        if (!summaries.isEmpty()) {
            aggregateSummaryToVO(summaries, vo);
        }

        // 4. 同比（YoY）
        List<RptInvestmentDaily> yoySummaries = cfYoy.join();
        if (!yoySummaries.isEmpty()) {
            RptInvestmentDaily yoy = aggregateSummary(yoySummaries);
            vo.setContractCountYoY(PeriodCompareUtil.calcYoY(
                    BigDecimal.valueOf(vo.getContractCount() == null ? 0 : vo.getContractCount()),
                    BigDecimal.valueOf(yoy.getContractCount() == null ? 0 : yoy.getContractCount())));
            vo.setContractAmountYoY(PeriodCompareUtil.calcYoY(vo.getContractAmount(), yoy.getContractAmount()));
            vo.setAvgRentPriceYoY(PeriodCompareUtil.calcYoY(vo.getAvgRentPrice(), yoy.getAvgRentPrice()));
            vo.setConversionRateYoY(PeriodCompareUtil.calcYoY(vo.getConversionRate(), yoy.getConversionRate()));
        }

        // 5. 漏斗 + 趋势 + 业绩对比
        vo.setFunnel(cfFunnel.join());

        List<InvTrendVO> allTrend = cfTrend.join();
        if (allTrend == null) allTrend = Collections.emptyList();
        vo.setIntentionTrend(allTrend);
        vo.setContractTrend(allTrend);

        List<PerformanceVO> comparison = cfPerformance.join();
        vo.setProjectComparison(comparison != null ? comparison : Collections.emptyList());

        return vo;
    }

    // ==================== 意向统计（P0） ====================

    @Override
    public List<IntentionStatsVO> intentionStats(ReportQueryParam param) {
        List<Long> permIds = ReportPermissionContext.get();
        if (ReportPermissionContext.hasNoPermission()) {
            return Collections.emptyList();
        }

        LocalDate startDate = param.getStartDate();
        LocalDate endDate = resolveEndDate(param, permIds);
        if (endDate == null) return Collections.emptyList();
        if (startDate == null) startDate = endDate.minusDays(29);

        String timeUnit = param.getTimeUnit() != null ? param.getTimeUnit().name() : "DAY";
        List<IntentionStatsVO> current = invDailyMapper.selectIntentionStats(
                param.getProjectId(), param.getFormatType(), param.getInvestmentManagerId(),
                startDate, endDate, timeUnit, permIds);
        if (current == null) current = Collections.emptyList();

        // 同比/环比对比
        if (param.getCompareMode() == CompareMode.NONE || param.getCompareMode() == null) {
            return current;
        }

        LocalDate prevStart;
        LocalDate prevEnd;
        if (param.getCompareMode() == CompareMode.YOY) {
            prevStart = PeriodCompareUtil.previousYearPeriod(startDate, param.getTimeUnit());
            prevEnd   = PeriodCompareUtil.previousYearPeriod(endDate, param.getTimeUnit());
        } else {
            prevStart = PeriodCompareUtil.previousPeriod(startDate, param.getTimeUnit());
            prevEnd   = PeriodCompareUtil.previousPeriod(endDate, param.getTimeUnit());
        }

        List<IntentionStatsVO> previous = invDailyMapper.selectIntentionStats(
                param.getProjectId(), param.getFormatType(), param.getInvestmentManagerId(),
                prevStart, prevEnd, timeUnit, permIds);

        if (previous != null && !previous.isEmpty()) {
            Map<String, Integer> prevMap = previous.stream()
                    .filter(v -> v.getTimeDim() != null && v.getIntentionCount() != null)
                    .collect(Collectors.toMap(IntentionStatsVO::getTimeDim,
                            IntentionStatsVO::getIntentionCount, (a, b) -> a));
            current.forEach(v -> {
                Integer prev = prevMap.get(v.getTimeDim());
                if (prev != null) {
                    v.setPrevIntentionCount(prev);
                    v.setGrowthRate(PeriodCompareUtil.calcGrowthRate(
                            BigDecimal.valueOf(v.getIntentionCount() == null ? 0 : v.getIntentionCount()),
                            BigDecimal.valueOf(prev)));
                }
            });
        }
        return current;
    }

    // ==================== 漏斗数据（P0） ====================

    @Override
    public List<FunnelVO> funnel(ReportQueryParam param) {
        List<Long> permIds = ReportPermissionContext.get();
        if (ReportPermissionContext.hasNoPermission()) {
            return Collections.emptyList();
        }

        LocalDate statDate = param.getStatDate() != null
                ? param.getStatDate()
                : invDailyMapper.selectMaxStatDate(param.getProjectId(), permIds);
        if (statDate == null) return Collections.emptyList();

        return buildFunnel(statDate, param.getProjectId(), permIds);
    }

    // ==================== 合同统计（P0） ====================

    @Override
    public List<ContractStatsVO> contractStats(ReportQueryParam param) {
        List<Long> permIds = ReportPermissionContext.get();
        if (ReportPermissionContext.hasNoPermission()) {
            return Collections.emptyList();
        }

        LocalDate startDate = param.getStartDate();
        LocalDate endDate = resolveEndDate(param, permIds);
        if (endDate == null) return Collections.emptyList();
        if (startDate == null) startDate = endDate.minusDays(29);

        String timeUnit = param.getTimeUnit() != null ? param.getTimeUnit().name() : "DAY";
        List<ContractStatsVO> current = invDailyMapper.selectContractStats(
                param.getProjectId(), param.getFormatType(), param.getInvestmentManagerId(),
                startDate, endDate, timeUnit, permIds);
        if (current == null) current = Collections.emptyList();

        if (param.getCompareMode() == CompareMode.NONE || param.getCompareMode() == null) {
            return current;
        }

        LocalDate prevStart;
        LocalDate prevEnd;
        if (param.getCompareMode() == CompareMode.YOY) {
            prevStart = PeriodCompareUtil.previousYearPeriod(startDate, param.getTimeUnit());
            prevEnd   = PeriodCompareUtil.previousYearPeriod(endDate, param.getTimeUnit());
        } else {
            prevStart = PeriodCompareUtil.previousPeriod(startDate, param.getTimeUnit());
            prevEnd   = PeriodCompareUtil.previousPeriod(endDate, param.getTimeUnit());
        }

        List<ContractStatsVO> previous = invDailyMapper.selectContractStats(
                param.getProjectId(), param.getFormatType(), param.getInvestmentManagerId(),
                prevStart, prevEnd, timeUnit, permIds);

        if (previous != null && !previous.isEmpty()) {
            Map<String, Integer> prevMap = previous.stream()
                    .filter(v -> v.getTimeDim() != null && v.getContractCount() != null)
                    .collect(Collectors.toMap(ContractStatsVO::getTimeDim,
                            ContractStatsVO::getContractCount, (a, b) -> a));
            current.forEach(v -> {
                Integer prev = prevMap.get(v.getTimeDim());
                if (prev != null) {
                    v.setPrevContractCount(prev);
                    v.setGrowthRate(PeriodCompareUtil.calcGrowthRate(
                            BigDecimal.valueOf(v.getContractCount() == null ? 0 : v.getContractCount()),
                            BigDecimal.valueOf(prev)));
                }
            });
        }
        return current;
    }

    // ==================== 业绩显差（P0） ====================

    @Override
    public List<PerformanceVO> performance(ReportQueryParam param) {
        List<Long> permIds = ReportPermissionContext.get();
        if (ReportPermissionContext.hasNoPermission()) {
            return Collections.emptyList();
        }

        LocalDate statDate = resolveStatDate(param, permIds);
        if (statDate == null) return Collections.emptyList();

        List<PerformanceVO> list = invDailyMapper.selectPerformance(
                statDate, param.getProjectId(), param.getInvestmentManagerId(), permIds);
        return list != null ? list : Collections.emptyList();
    }

    // ==================== P1 接口 ====================

    @Override
    public List<RentLevelVO> rentLevel(ReportQueryParam param) {
        List<Long> permIds = ReportPermissionContext.get();
        if (ReportPermissionContext.hasNoPermission()) {
            return Collections.emptyList();
        }

        LocalDate statDate = resolveStatDate(param, permIds);
        if (statDate == null) return Collections.emptyList();

        List<RentLevelVO> current = invDailyMapper.selectRentLevel(
                statDate, param.getProjectId(), param.getInvestmentManagerId(), permIds);
        if (current == null || current.isEmpty()) return Collections.emptyList();

        // 计算同比（去年同日）
        LocalDate yoyDate = statDate.minusYears(1);
        List<RentLevelVO> yoyList = invDailyMapper.selectRentLevel(
                yoyDate, param.getProjectId(), param.getInvestmentManagerId(), permIds);
        if (yoyList != null && !yoyList.isEmpty()) {
            // 以 projectId + formatType 为 key
            Map<String, BigDecimal> yoyMap = yoyList.stream()
                    .filter(v -> v.getProjectId() != null && v.getFormatType() != null)
                    .collect(Collectors.toMap(
                            v -> v.getProjectId() + "_" + v.getFormatType(),
                            RentLevelVO::getAvgRentPrice,
                            (a, b) -> a));
            current.forEach(v -> {
                BigDecimal prev = yoyMap.get(v.getProjectId() + "_" + v.getFormatType());
                if (prev != null) {
                    v.setPrevAvgRentPrice(prev);
                    v.setAvgRentPriceYoY(PeriodCompareUtil.calcYoY(v.getAvgRentPrice(), prev));
                }
            });
        }
        return current;
    }

    @Override
    public List<PolicyExecutionVO> policyExecution(ReportQueryParam param) {
        List<Long> permIds = ReportPermissionContext.get();
        if (ReportPermissionContext.hasNoPermission()) {
            return Collections.emptyList();
        }

        LocalDate statDate = resolveStatDate(param, permIds);
        if (statDate == null) return Collections.emptyList();

        List<PolicyExecutionVO> current = invDailyMapper.selectPolicyExecution(
                statDate, param.getProjectId(), permIds);
        if (current == null || current.isEmpty()) return Collections.emptyList();

        // 获取去年同日数据，计算租金偏差率
        LocalDate yoyDate = statDate.minusYears(1);
        List<PolicyExecutionVO> yoyList = invDailyMapper.selectPolicyExecution(
                yoyDate, param.getProjectId(), permIds);
        if (yoyList != null && !yoyList.isEmpty()) {
            Map<String, BigDecimal> yoyMap = yoyList.stream()
                    .filter(v -> v.getProjectId() != null && v.getFormatType() != null)
                    .collect(Collectors.toMap(
                            v -> v.getProjectId() + "_" + v.getFormatType(),
                            PolicyExecutionVO::getAvgRentPrice,
                            (a, b) -> a));
            current.forEach(v -> {
                BigDecimal prev = yoyMap.get(v.getProjectId() + "_" + v.getFormatType());
                if (prev != null) {
                    v.setPrevAvgRentPrice(prev);
                    v.setRentVarianceRate(PeriodCompareUtil.calcGrowthRate(v.getAvgRentPrice(), prev));
                }
            });
        }
        return current;
    }

    @Override
    public List<BrandRankingVO> brandRanking(ReportQueryParam param) {
        List<Long> permIds = ReportPermissionContext.get();
        if (ReportPermissionContext.hasNoPermission()) {
            return Collections.emptyList();
        }

        LocalDate statDate = resolveStatDate(param, permIds);
        if (statDate == null) return Collections.emptyList();

        List<BrandRankingVO> list = invDailyMapper.selectBrandRanking(
                statDate, param.getProjectId(), permIds);
        if (list == null || list.isEmpty()) return Collections.emptyList();

        // 计算占比并编排名次
        int totalCount = list.stream()
                .mapToInt(v -> v.getContractCount() == null ? 0 : v.getContractCount()).sum();
        BigDecimal totalArea = list.stream()
                .map(v -> v.getContractArea() == null ? BigDecimal.ZERO : v.getContractArea())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (int i = 0; i < list.size(); i++) {
            BrandRankingVO v = list.get(i);
            v.setRank(i + 1);
            v.setCountPercentage(PeriodCompareUtil.calcPercentage(
                    BigDecimal.valueOf(v.getContractCount() == null ? 0 : v.getContractCount()),
                    BigDecimal.valueOf(totalCount)));
            v.setAreaPercentage(PeriodCompareUtil.calcPercentage(v.getContractArea(), totalArea));
        }
        return list;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建漏斗数据（三阶段）
     */
    private List<FunnelVO> buildFunnel(LocalDate statDate, Long projectId, List<Long> permIds) {
        RptInvestmentDaily summary = invDailyMapper.selectFunnelSummary(statDate, projectId, permIds);
        if (summary == null) return Collections.emptyList();

        int intentionTotal = summary.getIntentionCount() == null ? 0 : summary.getIntentionCount();
        int intentionSigned = summary.getIntentionSigned() == null ? 0 : summary.getIntentionSigned();
        int contractCount = summary.getContractCount() == null ? 0 : summary.getContractCount();

        List<FunnelVO> result = new ArrayList<>(3);

        // 阶段1：意向登记
        FunnelVO stage1 = new FunnelVO()
                .setStage("INTENTION_TOTAL")
                .setStageName("意向登记")
                .setCount(intentionTotal)
                .setConversionRate(BigDecimal.valueOf(100))
                .setOverallConversionRate(BigDecimal.valueOf(100));
        result.add(stage1);

        // 阶段2：已签意向
        BigDecimal signedRate = PeriodCompareUtil.calcPercentage(
                BigDecimal.valueOf(intentionSigned), BigDecimal.valueOf(intentionTotal));
        FunnelVO stage2 = new FunnelVO()
                .setStage("INTENTION_SIGNED")
                .setStageName("已签意向")
                .setCount(intentionSigned)
                .setConversionRate(signedRate)
                .setOverallConversionRate(signedRate);
        result.add(stage2);

        // 阶段3：已签合同
        BigDecimal contractFromSignedRate = PeriodCompareUtil.calcPercentage(
                BigDecimal.valueOf(contractCount), BigDecimal.valueOf(intentionSigned));
        BigDecimal contractOverallRate = PeriodCompareUtil.calcPercentage(
                BigDecimal.valueOf(contractCount), BigDecimal.valueOf(intentionTotal));
        FunnelVO stage3 = new FunnelVO()
                .setStage("CONTRACT_SIGNED")
                .setStageName("已签合同")
                .setCount(contractCount)
                .setAmount(summary.getContractAmount())
                .setArea(summary.getContractArea())
                .setConversionRate(contractFromSignedRate)
                .setOverallConversionRate(contractOverallRate);
        result.add(stage3);

        return result;
    }

    /**
     * 聚合多项目汇总数据到看板 VO
     */
    private void aggregateSummaryToVO(List<RptInvestmentDaily> summaries, InvDashboardVO vo) {
        int intentionCount = 0, intentionSigned = 0, newIntention = 0;
        int contractCount = 0, newContract = 0;
        BigDecimal contractAmount = BigDecimal.ZERO;
        BigDecimal contractArea = BigDecimal.ZERO;

        for (RptInvestmentDaily r : summaries) {
            intentionCount  += r.getIntentionCount() == null ? 0 : r.getIntentionCount();
            intentionSigned += r.getIntentionSigned() == null ? 0 : r.getIntentionSigned();
            newIntention    += r.getNewIntention() == null ? 0 : r.getNewIntention();
            contractCount   += r.getContractCount() == null ? 0 : r.getContractCount();
            newContract     += r.getNewContract() == null ? 0 : r.getNewContract();
            contractAmount = contractAmount.add(r.getContractAmount() != null
                    ? r.getContractAmount() : BigDecimal.ZERO);
            contractArea = contractArea.add(r.getContractArea() != null
                    ? r.getContractArea() : BigDecimal.ZERO);
        }

        vo.setIntentionCount(intentionCount)
                .setIntentionSigned(intentionSigned)
                .setNewIntentionToday(newIntention)
                .setContractCount(contractCount)
                .setNewContractToday(newContract)
                .setContractAmount(contractAmount)
                .setContractArea(contractArea);

        // 转化率（意向→合同）
        vo.setConversionRate(PeriodCompareUtil.calcPercentage(
                BigDecimal.valueOf(contractCount), BigDecimal.valueOf(intentionCount)));

        // 加权平均租金：合同总金额 / 合同总面积 / 12个月
        if (contractArea.compareTo(BigDecimal.ZERO) > 0) {
            vo.setAvgRentPrice(contractAmount.divide(contractArea, 2, java.math.RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(12), 2, java.math.RoundingMode.HALF_UP));
        } else {
            vo.setAvgRentPrice(BigDecimal.ZERO);
        }
    }

    /**
     * 聚合多项目列表为单条汇总（供同比/环比用）
     */
    private RptInvestmentDaily aggregateSummary(List<RptInvestmentDaily> summaries) {
        InvDashboardVO tmpVo = new InvDashboardVO();
        aggregateSummaryToVO(summaries, tmpVo);
        RptInvestmentDaily agg = new RptInvestmentDaily();
        agg.setContractCount(tmpVo.getContractCount());
        agg.setContractAmount(tmpVo.getContractAmount());
        agg.setContractArea(tmpVo.getContractArea());
        agg.setConversionRate(tmpVo.getConversionRate());
        agg.setAvgRentPrice(tmpVo.getAvgRentPrice());
        return agg;
    }

    /**
     * 解析统计结束日期：优先用 param.endDate，否则查最新日期
     */
    private LocalDate resolveEndDate(ReportQueryParam param, List<Long> permIds) {
        if (param.getEndDate() != null) return param.getEndDate();
        return invDailyMapper.selectMaxStatDate(param.getProjectId(), permIds);
    }

    /**
     * 解析统计日期：优先用 param.statDate，否则查最新日期
     */
    private LocalDate resolveStatDate(ReportQueryParam param, List<Long> permIds) {
        if (param.getStatDate() != null) return param.getStatDate();
        return invDailyMapper.selectMaxStatDate(param.getProjectId(), permIds);
    }
}
