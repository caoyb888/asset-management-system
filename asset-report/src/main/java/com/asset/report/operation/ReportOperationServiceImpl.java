package com.asset.report.operation;

import com.asset.report.common.param.ReportQueryParam;
import com.asset.report.common.param.ReportQueryParam.CompareMode;
import com.asset.report.common.permission.ReportPermissionContext;
import com.asset.report.common.util.PeriodCompareUtil;
import com.asset.report.entity.RptOperationMonthly;
import com.asset.report.mapper.rpt.RptOperationMonthlyMapper;
import com.asset.report.vo.opr.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 营运类报表 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportOperationServiceImpl implements ReportOperationService {

    private final RptOperationMonthlyMapper oprMapper;

    // ==================== 看板（P0）====================

    @Override
    public OprDashboardVO dashboard(ReportQueryParam param) {
        List<Long> permIds = ReportPermissionContext.get();
        if (ReportPermissionContext.hasNoPermission()) {
            return new OprDashboardVO();
        }

        // 1. 确定最新统计月份
        String latestMonth = oprMapper.selectMaxStatMonth(param.getProjectId(), permIds);
        if (latestMonth == null) {
            log.warn("[OprDashboard] rpt_operation_monthly 无数据，返回空看板");
            return new OprDashboardVO();
        }

        // 2. 查询最新月份项目级汇总
        List<RptOperationMonthly> summaries = oprMapper.selectProjectSummaryByMonth(
                latestMonth, param.getProjectId(), permIds);

        OprDashboardVO vo = new OprDashboardVO().setLatestMonth(latestMonth);

        if (!summaries.isEmpty()) {
            aggregateSummaryToVO(summaries, vo);
        }

        // 3. 同比（YoY）：去年同月
        String yoyMonth = prevYearMonth(latestMonth);
        List<RptOperationMonthly> yoySummaries = oprMapper.selectProjectSummaryByMonth(
                yoyMonth, param.getProjectId(), permIds);
        if (!yoySummaries.isEmpty()) {
            BigDecimal prevRevenue = sumRevenue(yoySummaries);
            BigDecimal prevPassenger = BigDecimal.valueOf(sumPassenger(yoySummaries));
            BigDecimal prevSqm = avgSqm(yoySummaries);
            vo.setRevenueYoY(PeriodCompareUtil.calcYoY(vo.getTotalRevenue(), prevRevenue));
            vo.setPassengerFlowYoY(PeriodCompareUtil.calcYoY(
                    BigDecimal.valueOf(vo.getPassengerFlow() == null ? 0 : vo.getPassengerFlow()), prevPassenger));
            vo.setAvgRevenuePerSqmYoY(PeriodCompareUtil.calcYoY(vo.getAvgRevenuePerSqm(), prevSqm));
        }

        // 4. 到期预警
        List<OprExpiringContractVO> expiring = oprMapper.selectExpiringContracts(
                latestMonth, param.getProjectId(), permIds);
        if (expiring != null && !expiring.isEmpty()) {
            int w30 = expiring.stream().mapToInt(e -> e.getExpiringWithin30() == null ? 0 : e.getExpiringWithin30()).sum();
            int w60 = expiring.stream().mapToInt(e -> e.getExpiringWithin60() == null ? 0 : e.getExpiringWithin60()).sum();
            int w90 = expiring.stream().mapToInt(e -> e.getExpiringWithin90() == null ? 0 : e.getExpiringWithin90()).sum();
            vo.setExpiringWithin30(w30).setExpiringWithin60(w60).setExpiringWithin90(w90);
        }

        // 5. 近12个月趋势
        String trendStart = prevMonths(latestMonth, 11);
        List<OprTrendVO> trend = oprMapper.selectRevenueTrend(
                param.getProjectId(), null, trendStart, latestMonth, permIds);
        vo.setRevenueTrend(trend != null ? trend : Collections.emptyList());
        vo.setPassengerTrend(trend != null ? trend : Collections.emptyList());

        // 6. 项目业务对比
        List<OprRegionCompareVO> comparison = oprMapper.selectRegionCompare(
                latestMonth, param.getProjectId(), permIds);
        if (comparison != null && !comparison.isEmpty()) {
            calcRegionScores(comparison);
        }
        vo.setProjectComparison(comparison != null ? comparison : Collections.emptyList());

        return vo;
    }

    // ==================== 营收汇总（P0）====================

    @Override
    public List<OprRevenueSummaryVO> revenueSummary(ReportQueryParam param) {
        List<Long> permIds = ReportPermissionContext.get();
        if (ReportPermissionContext.hasNoPermission()) return Collections.emptyList();

        String endMonth = resolveEndMonth(param, permIds);
        if (endMonth == null) return Collections.emptyList();
        String startMonth = param.getStartMonth() != null ? param.getStartMonth() : prevMonths(endMonth, 11);

        List<OprRevenueSummaryVO> current = oprMapper.selectRevenueSummary(
                param.getProjectId(), param.getFormatType(), startMonth, endMonth, permIds);
        if (current == null) current = Collections.emptyList();

        if (param.getCompareMode() == CompareMode.NONE || param.getCompareMode() == null) {
            return current;
        }

        // 获取对比期数据
        String prevStart = shiftMonths(startMonth, param.getCompareMode() == CompareMode.YOY ? -12 : -1);
        String prevEnd   = shiftMonths(endMonth,   param.getCompareMode() == CompareMode.YOY ? -12 : -1);
        List<OprRevenueSummaryVO> previous = oprMapper.selectRevenueSummary(
                param.getProjectId(), param.getFormatType(), prevStart, prevEnd, permIds);

        if (previous != null && !previous.isEmpty()) {
            // key: timeDim_projectId_formatType
            Map<String, BigDecimal> prevMap = previous.stream()
                    .filter(v -> v.getTimeDim() != null)
                    .collect(Collectors.toMap(
                            v -> v.getTimeDim() + "_" + v.getProjectId() + "_" + v.getFormatType(),
                            OprRevenueSummaryVO::getRevenueAmount,
                            (a, b) -> a));
            current.forEach(v -> {
                BigDecimal prev = prevMap.get(v.getTimeDim() + "_" + v.getProjectId() + "_" + v.getFormatType());
                if (prev != null) {
                    v.setPrevRevenueAmount(prev);
                    v.setRevenueGrowthRate(PeriodCompareUtil.calcGrowthRate(v.getRevenueAmount(), prev));
                }
            });
        }
        return current;
    }

    // ==================== 合同变更统计（P0）====================

    @Override
    public List<OprContractChangeVO> contractChanges(ReportQueryParam param) {
        List<Long> permIds = ReportPermissionContext.get();
        if (ReportPermissionContext.hasNoPermission()) return Collections.emptyList();

        String endMonth = resolveEndMonth(param, permIds);
        if (endMonth == null) return Collections.emptyList();
        String startMonth = param.getStartMonth() != null ? param.getStartMonth() : prevMonths(endMonth, 11);

        List<OprContractChangeVO> current = oprMapper.selectContractChanges(
                param.getProjectId(), param.getFormatType(), startMonth, endMonth, permIds);
        if (current == null) current = Collections.emptyList();

        if (param.getCompareMode() == CompareMode.NONE || param.getCompareMode() == null) {
            return current;
        }

        String prevStart = shiftMonths(startMonth, param.getCompareMode() == CompareMode.YOY ? -12 : -1);
        String prevEnd   = shiftMonths(endMonth,   param.getCompareMode() == CompareMode.YOY ? -12 : -1);
        List<OprContractChangeVO> previous = oprMapper.selectContractChanges(
                param.getProjectId(), param.getFormatType(), prevStart, prevEnd, permIds);

        if (previous != null && !previous.isEmpty()) {
            Map<String, Integer> prevMap = previous.stream()
                    .filter(v -> v.getTimeDim() != null)
                    .collect(Collectors.toMap(
                            v -> v.getTimeDim() + "_" + v.getProjectId(),
                            OprContractChangeVO::getChangeCount,
                            (a, b) -> a));
            current.forEach(v -> {
                Integer prev = prevMap.get(v.getTimeDim() + "_" + v.getProjectId());
                if (prev != null) {
                    v.setPrevChangeCount(prev);
                    v.setChangeCountGrowthRate(PeriodCompareUtil.calcGrowthRate(
                            BigDecimal.valueOf(v.getChangeCount() == null ? 0 : v.getChangeCount()),
                            BigDecimal.valueOf(prev)));
                }
            });
        }
        return current;
    }

    // ==================== 租金变更分析（P0）====================

    @Override
    public List<OprRentChangeVO> rentChanges(ReportQueryParam param) {
        List<Long> permIds = ReportPermissionContext.get();
        if (ReportPermissionContext.hasNoPermission()) return Collections.emptyList();

        String endMonth = resolveEndMonth(param, permIds);
        if (endMonth == null) return Collections.emptyList();
        String startMonth = param.getStartMonth() != null ? param.getStartMonth() : prevMonths(endMonth, 11);

        List<OprRentChangeVO> current = oprMapper.selectRentChanges(
                param.getProjectId(), param.getFormatType(), startMonth, endMonth, permIds);
        if (current == null) current = Collections.emptyList();

        if (param.getCompareMode() == CompareMode.NONE || param.getCompareMode() == null) {
            return current;
        }

        String prevStart = shiftMonths(startMonth, param.getCompareMode() == CompareMode.YOY ? -12 : -1);
        String prevEnd   = shiftMonths(endMonth,   param.getCompareMode() == CompareMode.YOY ? -12 : -1);
        List<OprRentChangeVO> previous = oprMapper.selectRentChanges(
                param.getProjectId(), param.getFormatType(), prevStart, prevEnd, permIds);

        if (previous != null && !previous.isEmpty()) {
            Map<String, BigDecimal> prevMap = previous.stream()
                    .filter(v -> v.getTimeDim() != null)
                    .collect(Collectors.toMap(
                            v -> v.getTimeDim() + "_" + v.getProjectId(),
                            OprRentChangeVO::getChangeRentImpact,
                            (a, b) -> a));
            current.forEach(v -> {
                BigDecimal prev = prevMap.get(v.getTimeDim() + "_" + v.getProjectId());
                if (prev != null) {
                    v.setPrevChangeRentImpact(prev);
                    v.setChangeRentGrowthRate(PeriodCompareUtil.calcGrowthRate(v.getChangeRentImpact(), prev));
                }
            });
        }
        return current;
    }

    // ==================== 合同到期预警（P0）====================

    @Override
    public List<OprExpiringContractVO> expiringContracts(ReportQueryParam param) {
        List<Long> permIds = ReportPermissionContext.get();
        if (ReportPermissionContext.hasNoPermission()) return Collections.emptyList();

        String statMonth = param.getStatMonth() != null
                ? param.getStatMonth()
                : oprMapper.selectMaxStatMonth(param.getProjectId(), permIds);
        if (statMonth == null) return Collections.emptyList();

        List<OprExpiringContractVO> list = oprMapper.selectExpiringContracts(
                statMonth, param.getProjectId(), permIds);
        return list != null ? list : Collections.emptyList();
    }

    // ==================== 地区业务对比（P0）====================

    @Override
    public List<OprRegionCompareVO> regionCompare(ReportQueryParam param) {
        List<Long> permIds = ReportPermissionContext.get();
        if (ReportPermissionContext.hasNoPermission()) return Collections.emptyList();

        String statMonth = param.getStatMonth() != null
                ? param.getStatMonth()
                : oprMapper.selectMaxStatMonth(param.getProjectId(), permIds);
        if (statMonth == null) return Collections.emptyList();

        List<OprRegionCompareVO> list = oprMapper.selectRegionCompare(
                statMonth, param.getProjectId(), permIds);
        if (list == null || list.isEmpty()) return Collections.emptyList();

        calcRegionScores(list);
        return list;
    }

    // ==================== P1 接口 ====================

    @Override
    public List<OprPassengerFlowVO> passengerFlow(ReportQueryParam param) {
        List<Long> permIds = ReportPermissionContext.get();
        if (ReportPermissionContext.hasNoPermission()) return Collections.emptyList();

        String endMonth = resolveEndMonth(param, permIds);
        if (endMonth == null) return Collections.emptyList();
        String startMonth = param.getStartMonth() != null ? param.getStartMonth() : prevMonths(endMonth, 11);

        List<OprPassengerFlowVO> current = oprMapper.selectPassengerFlow(
                param.getProjectId(), startMonth, endMonth, permIds);
        if (current == null) current = Collections.emptyList();

        if (param.getCompareMode() == CompareMode.NONE || param.getCompareMode() == null) {
            return current;
        }

        String prevStart = shiftMonths(startMonth, param.getCompareMode() == CompareMode.YOY ? -12 : -1);
        String prevEnd   = shiftMonths(endMonth,   param.getCompareMode() == CompareMode.YOY ? -12 : -1);
        List<OprPassengerFlowVO> previous = oprMapper.selectPassengerFlow(
                param.getProjectId(), prevStart, prevEnd, permIds);

        if (previous != null && !previous.isEmpty()) {
            Map<String, Long> prevMap = previous.stream()
                    .filter(v -> v.getTimeDim() != null)
                    .collect(Collectors.toMap(
                            v -> v.getTimeDim() + "_" + v.getProjectId(),
                            OprPassengerFlowVO::getPassengerFlow,
                            (a, b) -> a));
            current.forEach(v -> {
                Long prev = prevMap.get(v.getTimeDim() + "_" + v.getProjectId());
                if (prev != null) {
                    v.setPrevPassengerFlow(prev);
                    v.setGrowthRate(PeriodCompareUtil.calcGrowthRate(
                            BigDecimal.valueOf(v.getPassengerFlow() == null ? 0 : v.getPassengerFlow()),
                            BigDecimal.valueOf(prev)));
                }
            });
        }
        return current;
    }

    @Override
    public List<OprTerminationStatsVO> terminationStats(ReportQueryParam param) {
        List<Long> permIds = ReportPermissionContext.get();
        if (ReportPermissionContext.hasNoPermission()) return Collections.emptyList();

        String endMonth = resolveEndMonth(param, permIds);
        if (endMonth == null) return Collections.emptyList();
        String startMonth = param.getStartMonth() != null ? param.getStartMonth() : prevMonths(endMonth, 11);

        List<OprTerminationStatsVO> current = oprMapper.selectTerminationStats(
                param.getProjectId(), param.getFormatType(), startMonth, endMonth, permIds);
        if (current == null) current = Collections.emptyList();

        if (param.getCompareMode() == CompareMode.NONE || param.getCompareMode() == null) {
            return current;
        }

        String prevStart = shiftMonths(startMonth, param.getCompareMode() == CompareMode.YOY ? -12 : -1);
        String prevEnd   = shiftMonths(endMonth,   param.getCompareMode() == CompareMode.YOY ? -12 : -1);
        List<OprTerminationStatsVO> previous = oprMapper.selectTerminationStats(
                param.getProjectId(), param.getFormatType(), prevStart, prevEnd, permIds);

        if (previous != null && !previous.isEmpty()) {
            Map<String, Integer> prevMap = previous.stream()
                    .filter(v -> v.getTimeDim() != null)
                    .collect(Collectors.toMap(
                            v -> v.getTimeDim() + "_" + v.getProjectId(),
                            OprTerminationStatsVO::getTerminatedContracts,
                            (a, b) -> a));
            current.forEach(v -> {
                Integer prev = prevMap.get(v.getTimeDim() + "_" + v.getProjectId());
                if (prev != null) {
                    v.setPrevTerminatedContracts(prev);
                    v.setGrowthRate(PeriodCompareUtil.calcGrowthRate(
                            BigDecimal.valueOf(v.getTerminatedContracts() == null ? 0 : v.getTerminatedContracts()),
                            BigDecimal.valueOf(prev)));
                }
            });
        }
        return current;
    }

    @Override
    public List<OprFloatingRentVO> floatingRent(ReportQueryParam param) {
        List<Long> permIds = ReportPermissionContext.get();
        if (ReportPermissionContext.hasNoPermission()) return Collections.emptyList();

        String endMonth = resolveEndMonth(param, permIds);
        if (endMonth == null) return Collections.emptyList();
        String startMonth = param.getStartMonth() != null ? param.getStartMonth() : prevMonths(endMonth, 11);

        List<OprFloatingRentVO> current = oprMapper.selectFloatingRent(
                param.getProjectId(), param.getFormatType(), startMonth, endMonth, permIds);
        if (current == null) current = Collections.emptyList();

        if (param.getCompareMode() == CompareMode.NONE || param.getCompareMode() == null) {
            return current;
        }

        String prevStart = shiftMonths(startMonth, param.getCompareMode() == CompareMode.YOY ? -12 : -1);
        String prevEnd   = shiftMonths(endMonth,   param.getCompareMode() == CompareMode.YOY ? -12 : -1);
        List<OprFloatingRentVO> previous = oprMapper.selectFloatingRent(
                param.getProjectId(), param.getFormatType(), prevStart, prevEnd, permIds);

        if (previous != null && !previous.isEmpty()) {
            Map<String, BigDecimal> prevMap = previous.stream()
                    .filter(v -> v.getTimeDim() != null)
                    .collect(Collectors.toMap(
                            v -> v.getTimeDim() + "_" + v.getProjectId() + "_" + v.getFormatType(),
                            OprFloatingRentVO::getFloatingRentAmount,
                            (a, b) -> a));
            current.forEach(v -> {
                BigDecimal prev = prevMap.get(v.getTimeDim() + "_" + v.getProjectId() + "_" + v.getFormatType());
                if (prev != null) {
                    v.setPrevFloatingRentAmount(prev);
                    v.setGrowthRate(PeriodCompareUtil.calcGrowthRate(v.getFloatingRentAmount(), prev));
                }
            });
        }
        return current;
    }

    // ==================== 私有辅助方法 ====================

    /** 聚合多项目汇总数据到看板 VO */
    private void aggregateSummaryToVO(List<RptOperationMonthly> summaries, OprDashboardVO vo) {
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal floating = BigDecimal.ZERO;
        long passenger = 0L;
        int changes = 0;
        int terminated = 0;
        for (RptOperationMonthly r : summaries) {
            revenue   = revenue.add(r.getRevenueAmount() != null ? r.getRevenueAmount() : BigDecimal.ZERO);
            floating  = floating.add(r.getFloatingRentAmount() != null ? r.getFloatingRentAmount() : BigDecimal.ZERO);
            passenger += r.getPassengerFlow() != null ? r.getPassengerFlow() : 0L;
            changes   += r.getChangeCount() != null ? r.getChangeCount() : 0;
            terminated += r.getTerminatedContracts() != null ? r.getTerminatedContracts() : 0;
        }
        // 坪效取各项目均值
        BigDecimal sqmSum = summaries.stream()
                .map(r -> r.getAvgRevenuePerSqm() != null ? r.getAvgRevenuePerSqm() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgSqm = summaries.isEmpty() ? BigDecimal.ZERO
                : sqmSum.divide(BigDecimal.valueOf(summaries.size()), 2, java.math.RoundingMode.HALF_UP);

        vo.setTotalRevenue(revenue)
                .setFloatingRentAmount(floating)
                .setPassengerFlow(passenger)
                .setChangeCount(changes)
                .setTerminatedContracts(terminated)
                .setAvgRevenuePerSqm(avgSqm);
    }

    /** 为雷达图计算各项目评分（百分位归一化） */
    private void calcRegionScores(List<OprRegionCompareVO> list) {
        if (list.size() <= 1) {
            list.forEach(v -> {
                v.setRevenueScore(BigDecimal.valueOf(100));
                v.setPassengerScore(BigDecimal.valueOf(100));
                v.setAvgRevenueScore(BigDecimal.valueOf(100));
            });
            return;
        }
        BigDecimal maxRevenue  = list.stream().map(v -> v.getRevenueAmount()  != null ? v.getRevenueAmount() : BigDecimal.ZERO).max(BigDecimal::compareTo).orElse(BigDecimal.ONE);
        BigDecimal maxPassenger = BigDecimal.valueOf(list.stream().mapToLong(v -> v.getPassengerFlow() != null ? v.getPassengerFlow() : 0).max().orElse(1));
        BigDecimal maxSqm = list.stream().map(v -> v.getAvgRevenuePerSqm() != null ? v.getAvgRevenuePerSqm() : BigDecimal.ZERO).max(BigDecimal::compareTo).orElse(BigDecimal.ONE);

        list.forEach(v -> {
            v.setRevenueScore(PeriodCompareUtil.calcPercentage(
                    v.getRevenueAmount() != null ? v.getRevenueAmount() : BigDecimal.ZERO, maxRevenue));
            v.setPassengerScore(PeriodCompareUtil.calcPercentage(
                    BigDecimal.valueOf(v.getPassengerFlow() != null ? v.getPassengerFlow() : 0L), maxPassenger));
            v.setAvgRevenueScore(PeriodCompareUtil.calcPercentage(
                    v.getAvgRevenuePerSqm() != null ? v.getAvgRevenuePerSqm() : BigDecimal.ZERO, maxSqm));
        });
    }

    private BigDecimal sumRevenue(List<RptOperationMonthly> list) {
        return list.stream().map(r -> r.getRevenueAmount() != null ? r.getRevenueAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long sumPassenger(List<RptOperationMonthly> list) {
        return list.stream().mapToLong(r -> r.getPassengerFlow() != null ? r.getPassengerFlow() : 0L).sum();
    }

    private BigDecimal avgSqm(List<RptOperationMonthly> list) {
        if (list.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = list.stream()
                .map(r -> r.getAvgRevenuePerSqm() != null ? r.getAvgRevenuePerSqm() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(list.size()), 2, java.math.RoundingMode.HALF_UP);
    }

    /** 解析结束月份：优先 param.endMonth，否则查最新月份 */
    private String resolveEndMonth(ReportQueryParam param, List<Long> permIds) {
        if (param.getEndMonth() != null) return param.getEndMonth();
        if (param.getStatMonth() != null) return param.getStatMonth();
        return oprMapper.selectMaxStatMonth(param.getProjectId(), permIds);
    }

    /**
     * 按月数位移月份字符串（yyyy-MM）
     * @param month  基础月份
     * @param offset 偏移月数，负数=向前，正数=向后
     */
    private String shiftMonths(String month, int offset) {
        if (month == null) return null;
        java.time.YearMonth ym = java.time.YearMonth.parse(month);
        return ym.plusMonths(offset).toString();
    }

    /** 向前推 n 个月 */
    private String prevMonths(String month, int n) {
        return shiftMonths(month, -n);
    }

    /** 向前推 12 个月（去年同月） */
    private String prevYearMonth(String month) {
        return shiftMonths(month, -12);
    }
}
