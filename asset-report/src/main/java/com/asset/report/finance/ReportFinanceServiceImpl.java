package com.asset.report.finance;

import com.asset.report.common.param.ReportQueryParam;
import com.asset.report.common.param.ReportQueryParam.CompareMode;
import com.asset.report.common.permission.ReportPermissionContext;
import com.asset.report.common.util.PeriodCompareUtil;
import com.asset.report.entity.RptFinanceMonthly;
import com.asset.report.mapper.rpt.RptAgingAnalysisMapper;
import com.asset.report.mapper.rpt.RptFinanceMonthlyMapper;
import com.asset.report.vo.fin.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 财务类报表 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportFinanceServiceImpl implements ReportFinanceService {

    private final RptFinanceMonthlyMapper finMapper;
    private final RptAgingAnalysisMapper agingMapper;

    // ==================== 看板（P0）====================

    @Override
    public FinDashboardVO dashboard(ReportQueryParam param) {
        List<Long> permIds = ReportPermissionContext.get();
        if (ReportPermissionContext.hasNoPermission()) {
            return new FinDashboardVO();
        }

        // 1. 确定最新统计月份
        String latestMonth = finMapper.selectMaxStatMonth(param.getProjectId(), permIds);
        if (latestMonth == null) {
            log.warn("[FinDashboard] rpt_finance_monthly 无数据，返回空看板");
            return new FinDashboardVO();
        }

        // 2. 查询最新月份的项目级汇总
        List<RptFinanceMonthly> summaries = finMapper.selectProjectSummaryByMonth(
                latestMonth, param.getProjectId(), permIds);

        FinDashboardVO vo = new FinDashboardVO().setLatestMonth(latestMonth);

        if (!summaries.isEmpty()) {
            aggregateSummaryToVO(summaries, vo);
        }

        // 3. 同比（YoY）：去年同月
        String yoyMonth = prevYearMonth(latestMonth);
        List<RptFinanceMonthly> yoySummaries = finMapper.selectProjectSummaryByMonth(
                yoyMonth, param.getProjectId(), permIds);
        if (!yoySummaries.isEmpty()) {
            BigDecimal prevReceivable = sumReceivable(yoySummaries);
            BigDecimal prevReceived = sumReceived(yoySummaries);
            BigDecimal prevOverdue = sumOverdue(yoySummaries);
            BigDecimal prevCollectionRate = calcWeightedCollectionRate(yoySummaries);
            BigDecimal prevOverdueRate = calcWeightedOverdueRate(yoySummaries);

            vo.setReceivableYoY(PeriodCompareUtil.calcYoY(vo.getTotalReceivable(), prevReceivable));
            vo.setReceivedYoY(PeriodCompareUtil.calcYoY(vo.getTotalReceived(), prevReceived));
            vo.setCollectionRateYoY(PeriodCompareUtil.calcYoY(vo.getAvgCollectionRate(), prevCollectionRate));
            vo.setOverdueRateYoY(PeriodCompareUtil.calcYoY(vo.getAvgOverdueRate(), prevOverdueRate));
        }

        // 4. 近 12 个月趋势
        String trendStart = prevMonths(latestMonth, 11);
        List<FinTrendVO> trend = finMapper.selectFinTrend(
                param.getProjectId(), trendStart, latestMonth, permIds);
        vo.setFinanceTrend(trend != null ? trend : Collections.emptyList());

        // 5. 账龄分布（最新统计日期的项目汇总）
        LocalDate latestAgingDate = agingMapper.selectMaxStatDate(param.getProjectId(), permIds);
        if (latestAgingDate != null) {
            List<FinAgingAnalysisVO> agingList = agingMapper.selectAgingProjectSummary(
                    latestAgingDate, param.getProjectId(), permIds);
            if (!agingList.isEmpty()) {
                FinAgingAnalysisVO agingSummary = mergeAgingList(agingList);
                calcAgingRates(agingSummary);
                vo.setAgingSummary(agingSummary);
            }

            // 6. 欠款 TOP10 商家
            List<FinAgingAnalysisVO> top10 = agingMapper.selectOverdueTopN(
                    latestAgingDate, param.getProjectId(), 10, permIds);
            if (top10 != null) {
                top10.forEach(this::calcAgingRates);
                vo.setOverdueTop10(top10);
            }
        }

        return vo;
    }

    // ==================== 应收汇总（P0）====================

    @Override
    public List<FinReceivableSummaryVO> receivableSummary(ReportQueryParam param) {
        List<Long> permIds = ReportPermissionContext.get();
        if (ReportPermissionContext.hasNoPermission()) {
            return Collections.emptyList();
        }

        String endMonth = resolveEndMonth(param, permIds);
        if (endMonth == null) return Collections.emptyList();
        String startMonth = param.getStartMonth() != null ? param.getStartMonth() : prevMonths(endMonth, 11);

        List<FinReceivableSummaryVO> current = finMapper.selectReceivableSummary(
                param.getProjectId(), param.getFeeItemType(), startMonth, endMonth, permIds);
        if (current == null) current = Collections.emptyList();

        if (param.getCompareMode() == null || param.getCompareMode() == CompareMode.NONE) {
            return current;
        }

        // 同比/环比对比期
        String prevStart;
        String prevEnd;
        if (param.getCompareMode() == CompareMode.YOY) {
            prevStart = prevYearMonth(startMonth);
            prevEnd = prevYearMonth(endMonth);
        } else {
            int monthSpan = monthsBetween(startMonth, endMonth);
            prevEnd = prevMonths(startMonth, 1);
            prevStart = prevMonths(prevEnd, monthSpan - 1);
        }

        List<FinReceivableSummaryVO> prev = finMapper.selectReceivableSummary(
                param.getProjectId(), param.getFeeItemType(), prevStart, prevEnd, permIds);
        if (prev == null) prev = Collections.emptyList();

        // 构建上期索引：key = timeDim + "_" + projectId + "_" + feeItemType
        Map<String, FinReceivableSummaryVO> prevMap = prev.stream().collect(
                Collectors.toMap(v -> buildKey(v.getTimeDim(), v.getProjectId(), v.getFeeItemType()),
                        v -> v, (a, b) -> a));

        // 填充增长率
        boolean isYoy = param.getCompareMode() == CompareMode.YOY;
        for (FinReceivableSummaryVO cur : current) {
            String prevTimeDim = isYoy
                    ? prevYearMonth(cur.getTimeDim())
                    : prevMonths(cur.getTimeDim(), 1);
            String key = buildKey(prevTimeDim, cur.getProjectId(), cur.getFeeItemType());
            FinReceivableSummaryVO p = prevMap.get(key);
            if (p != null) {
                BigDecimal growthReceivable = PeriodCompareUtil.calcYoY(cur.getReceivableAmount(), p.getReceivableAmount());
                BigDecimal growthReceived = PeriodCompareUtil.calcYoY(cur.getReceivedAmount(), p.getReceivedAmount());
                BigDecimal growthCollection = PeriodCompareUtil.calcYoY(cur.getCollectionRate(), p.getCollectionRate());
                if (isYoy) {
                    cur.setReceivableYoY(growthReceivable)
                       .setReceivedYoY(growthReceived)
                       .setCollectionRateYoY(growthCollection);
                } else {
                    cur.setReceivableMoM(growthReceivable)
                       .setCollectionRateMoM(growthCollection);
                }
            }
        }
        return current;
    }

    // ==================== 收款汇总（P0）====================

    @Override
    public List<FinReceiptSummaryVO> receiptSummary(ReportQueryParam param) {
        List<Long> permIds = ReportPermissionContext.get();
        if (ReportPermissionContext.hasNoPermission()) {
            return Collections.emptyList();
        }

        String endMonth = resolveEndMonth(param, permIds);
        if (endMonth == null) return Collections.emptyList();
        String startMonth = param.getStartMonth() != null ? param.getStartMonth() : prevMonths(endMonth, 11);

        List<FinReceiptSummaryVO> current = finMapper.selectReceiptSummary(
                param.getProjectId(), param.getFeeItemType(), startMonth, endMonth, permIds);
        if (current == null) current = Collections.emptyList();

        if (param.getCompareMode() == null || param.getCompareMode() == CompareMode.NONE) {
            return current;
        }

        String prevStart;
        String prevEnd;
        if (param.getCompareMode() == CompareMode.YOY) {
            prevStart = prevYearMonth(startMonth);
            prevEnd = prevYearMonth(endMonth);
        } else {
            int monthSpan = monthsBetween(startMonth, endMonth);
            prevEnd = prevMonths(startMonth, 1);
            prevStart = prevMonths(prevEnd, monthSpan - 1);
        }

        List<FinReceiptSummaryVO> prev = finMapper.selectReceiptSummary(
                param.getProjectId(), param.getFeeItemType(), prevStart, prevEnd, permIds);
        if (prev == null) prev = Collections.emptyList();

        Map<String, FinReceiptSummaryVO> prevMap = prev.stream().collect(
                Collectors.toMap(v -> buildKey(v.getTimeDim(), v.getProjectId(), v.getFeeItemType()),
                        v -> v, (a, b) -> a));

        boolean isYoy = param.getCompareMode() == CompareMode.YOY;
        for (FinReceiptSummaryVO cur : current) {
            String prevTimeDim = isYoy ? prevYearMonth(cur.getTimeDim()) : prevMonths(cur.getTimeDim(), 1);
            FinReceiptSummaryVO p = prevMap.get(buildKey(prevTimeDim, cur.getProjectId(), cur.getFeeItemType()));
            if (p != null) {
                BigDecimal growthReceived = PeriodCompareUtil.calcYoY(cur.getReceivedAmount(), p.getReceivedAmount());
                BigDecimal growthCollection = PeriodCompareUtil.calcYoY(cur.getCollectionRate(), p.getCollectionRate());
                if (isYoy) {
                    cur.setReceivedYoY(growthReceived).setCollectionRateYoY(growthCollection);
                } else {
                    cur.setReceivedMoM(growthReceived).setCollectionRateMoM(growthCollection);
                }
            }
        }
        return current;
    }

    // ==================== 欠款统计（P0）====================

    @Override
    public List<FinOutstandingSummaryVO> outstandingSummary(ReportQueryParam param) {
        List<Long> permIds = ReportPermissionContext.get();
        if (ReportPermissionContext.hasNoPermission()) {
            return Collections.emptyList();
        }

        String endMonth = resolveEndMonth(param, permIds);
        if (endMonth == null) return Collections.emptyList();
        String startMonth = param.getStartMonth() != null ? param.getStartMonth() : prevMonths(endMonth, 11);

        List<FinOutstandingSummaryVO> current = finMapper.selectOutstandingSummary(
                param.getProjectId(), param.getFeeItemType(), startMonth, endMonth, permIds);
        if (current == null) current = Collections.emptyList();

        if (param.getCompareMode() == null || param.getCompareMode() == CompareMode.NONE) {
            return current;
        }

        String prevStart;
        String prevEnd;
        if (param.getCompareMode() == CompareMode.YOY) {
            prevStart = prevYearMonth(startMonth);
            prevEnd = prevYearMonth(endMonth);
        } else {
            int monthSpan = monthsBetween(startMonth, endMonth);
            prevEnd = prevMonths(startMonth, 1);
            prevStart = prevMonths(prevEnd, monthSpan - 1);
        }

        List<FinOutstandingSummaryVO> prev = finMapper.selectOutstandingSummary(
                param.getProjectId(), param.getFeeItemType(), prevStart, prevEnd, permIds);
        if (prev == null) prev = Collections.emptyList();

        Map<String, FinOutstandingSummaryVO> prevMap = prev.stream().collect(
                Collectors.toMap(v -> buildKey(v.getTimeDim(), v.getProjectId(), v.getFeeItemType()),
                        v -> v, (a, b) -> a));

        boolean isYoy = param.getCompareMode() == CompareMode.YOY;
        for (FinOutstandingSummaryVO cur : current) {
            String prevTimeDim = isYoy ? prevYearMonth(cur.getTimeDim()) : prevMonths(cur.getTimeDim(), 1);
            FinOutstandingSummaryVO p = prevMap.get(buildKey(prevTimeDim, cur.getProjectId(), cur.getFeeItemType()));
            if (p != null) {
                cur.setOutstandingYoY(PeriodCompareUtil.calcYoY(cur.getOutstandingAmount(), p.getOutstandingAmount()));
                cur.setOverdueRateYoY(PeriodCompareUtil.calcYoY(cur.getOverdueRate(), p.getOverdueRate()));
            }
        }
        return current;
    }

    // ==================== 账龄分析（P0）====================

    @Override
    public List<FinAgingAnalysisVO> agingAnalysis(ReportQueryParam param) {
        List<Long> permIds = ReportPermissionContext.get();
        if (ReportPermissionContext.hasNoPermission()) {
            return Collections.emptyList();
        }

        // 确定统计日期：statDate 不传时取最新日期
        LocalDate statDate = param.getStatDate();
        if (statDate == null) {
            statDate = agingMapper.selectMaxStatDate(param.getProjectId(), permIds);
        }
        if (statDate == null) {
            log.warn("[AgingAnalysis] rpt_aging_analysis 无数据，返回空列表");
            return Collections.emptyList();
        }

        List<FinAgingAnalysisVO> result;
        if (param.getMerchantId() != null) {
            // 指定商家时查商家+合同粒度
            result = agingMapper.selectAgingByMerchant(statDate, param.getProjectId(),
                    param.getMerchantId(), permIds);
        } else {
            // 未指定商家时返回所有商家的汇总行
            result = agingMapper.selectAgingByMerchant(statDate, param.getProjectId(), null, permIds);
        }

        if (result == null) result = Collections.emptyList();
        result.forEach(this::calcAgingRates);
        return result;
    }

    // ==================== 逾期率统计（P0）====================

    @Override
    public List<FinOverdueRateVO> overdueRate(ReportQueryParam param) {
        List<Long> permIds = ReportPermissionContext.get();
        if (ReportPermissionContext.hasNoPermission()) {
            return Collections.emptyList();
        }

        String endMonth = resolveEndMonth(param, permIds);
        if (endMonth == null) return Collections.emptyList();
        String startMonth = param.getStartMonth() != null ? param.getStartMonth() : prevMonths(endMonth, 11);

        List<FinOverdueRateVO> current = finMapper.selectOverdueRate(
                param.getProjectId(), startMonth, endMonth, permIds);
        if (current == null) current = Collections.emptyList();

        if (param.getCompareMode() == null || param.getCompareMode() == CompareMode.NONE) {
            return current;
        }

        String prevStart;
        String prevEnd;
        if (param.getCompareMode() == CompareMode.YOY) {
            prevStart = prevYearMonth(startMonth);
            prevEnd = prevYearMonth(endMonth);
        } else {
            int monthSpan = monthsBetween(startMonth, endMonth);
            prevEnd = prevMonths(startMonth, 1);
            prevStart = prevMonths(prevEnd, monthSpan - 1);
        }

        List<FinOverdueRateVO> prev = finMapper.selectOverdueRate(
                param.getProjectId(), prevStart, prevEnd, permIds);
        if (prev == null) prev = Collections.emptyList();

        Map<String, FinOverdueRateVO> prevMap = prev.stream().collect(
                Collectors.toMap(v -> buildKey(v.getTimeDim(), v.getProjectId(), null),
                        v -> v, (a, b) -> a));

        boolean isYoy = param.getCompareMode() == CompareMode.YOY;
        for (FinOverdueRateVO cur : current) {
            String prevTimeDim = isYoy ? prevYearMonth(cur.getTimeDim()) : prevMonths(cur.getTimeDim(), 1);
            FinOverdueRateVO p = prevMap.get(buildKey(prevTimeDim, cur.getProjectId(), null));
            if (p != null) {
                BigDecimal rateGrowth = PeriodCompareUtil.calcYoY(cur.getOverdueRate(), p.getOverdueRate());
                BigDecimal amountGrowth = PeriodCompareUtil.calcYoY(cur.getOverdueAmount(), p.getOverdueAmount());
                if (isYoy) {
                    cur.setOverdueRateYoY(rateGrowth).setOverdueAmountYoY(amountGrowth);
                } else {
                    cur.setOverdueRateMoM(rateGrowth);
                }
            }
        }
        return current;
    }

    // ==================== 收缴率统计（P0）====================

    @Override
    public List<FinCollectionRateVO> collectionRate(ReportQueryParam param) {
        List<Long> permIds = ReportPermissionContext.get();
        if (ReportPermissionContext.hasNoPermission()) {
            return Collections.emptyList();
        }

        String endMonth = resolveEndMonth(param, permIds);
        if (endMonth == null) return Collections.emptyList();
        String startMonth = param.getStartMonth() != null ? param.getStartMonth() : prevMonths(endMonth, 11);

        List<FinCollectionRateVO> current = finMapper.selectCollectionRate(
                param.getProjectId(), param.getFeeItemType(), startMonth, endMonth, permIds);
        if (current == null) current = Collections.emptyList();

        if (param.getCompareMode() == null || param.getCompareMode() == CompareMode.NONE) {
            return current;
        }

        String prevStart;
        String prevEnd;
        if (param.getCompareMode() == CompareMode.YOY) {
            prevStart = prevYearMonth(startMonth);
            prevEnd = prevYearMonth(endMonth);
        } else {
            int monthSpan = monthsBetween(startMonth, endMonth);
            prevEnd = prevMonths(startMonth, 1);
            prevStart = prevMonths(prevEnd, monthSpan - 1);
        }

        List<FinCollectionRateVO> prev = finMapper.selectCollectionRate(
                param.getProjectId(), param.getFeeItemType(), prevStart, prevEnd, permIds);
        if (prev == null) prev = Collections.emptyList();

        Map<String, FinCollectionRateVO> prevMap = prev.stream().collect(
                Collectors.toMap(v -> buildKey(v.getTimeDim(), v.getProjectId(), v.getFeeItemType()),
                        v -> v, (a, b) -> a));

        boolean isYoy = param.getCompareMode() == CompareMode.YOY;
        for (FinCollectionRateVO cur : current) {
            String prevTimeDim = isYoy ? prevYearMonth(cur.getTimeDim()) : prevMonths(cur.getTimeDim(), 1);
            FinCollectionRateVO p = prevMap.get(buildKey(prevTimeDim, cur.getProjectId(), cur.getFeeItemType()));
            if (p != null) {
                BigDecimal growth = PeriodCompareUtil.calcYoY(cur.getCollectionRate(), p.getCollectionRate());
                if (isYoy) {
                    cur.setCollectionRateYoY(growth);
                } else {
                    cur.setCollectionRateMoM(growth);
                }
            }
        }
        return current;
    }

    // ==================== P1 接口 ====================

    @Override
    public List<FinVoucherStatsVO> voucherStats(ReportQueryParam param) {
        List<Long> permIds = ReportPermissionContext.get();
        if (ReportPermissionContext.hasNoPermission()) {
            return Collections.emptyList();
        }

        String endMonth = param.getEndMonth();
        String startMonth = param.getStartMonth();
        // 默认近12个月
        if (endMonth == null) {
            endMonth = YearMonth.now().toString();
        }
        if (startMonth == null) {
            startMonth = prevMonths(endMonth, 11);
        }

        List<FinVoucherStatsVO> result = finMapper.selectVoucherStats(
                param.getProjectId(), startMonth, endMonth, permIds);
        return result != null ? result : Collections.emptyList();
    }

    @Override
    public List<FinDepositSummaryVO> depositSummary(ReportQueryParam param) {
        List<Long> permIds = ReportPermissionContext.get();
        if (ReportPermissionContext.hasNoPermission()) {
            return Collections.emptyList();
        }

        String endMonth = resolveEndMonth(param, permIds);
        if (endMonth == null) return Collections.emptyList();
        String startMonth = param.getStartMonth() != null ? param.getStartMonth() : prevMonths(endMonth, 11);

        List<FinDepositSummaryVO> current = finMapper.selectDepositSummary(
                param.getProjectId(), startMonth, endMonth, permIds);
        if (current == null) current = Collections.emptyList();

        if (param.getCompareMode() == null || param.getCompareMode() == CompareMode.NONE) {
            return current;
        }

        boolean isYoy = param.getCompareMode() == CompareMode.YOY;
        String prevStart = isYoy ? prevYearMonth(startMonth) : prevMonths(startMonth, 1);
        String prevEnd = isYoy ? prevYearMonth(endMonth) : prevMonths(endMonth, 1);

        List<FinDepositSummaryVO> prev = finMapper.selectDepositSummary(
                param.getProjectId(), prevStart, prevEnd, permIds);
        if (prev == null) prev = Collections.emptyList();

        Map<String, FinDepositSummaryVO> prevMap = prev.stream().collect(
                Collectors.toMap(v -> buildKey(v.getTimeDim(), v.getProjectId(), null),
                        v -> v, (a, b) -> a));

        for (FinDepositSummaryVO cur : current) {
            String prevTimeDim = isYoy ? prevYearMonth(cur.getTimeDim()) : prevMonths(cur.getTimeDim(), 1);
            FinDepositSummaryVO p = prevMap.get(buildKey(prevTimeDim, cur.getProjectId(), null));
            if (p != null) {
                BigDecimal growth = PeriodCompareUtil.calcYoY(cur.getDepositBalance(), p.getDepositBalance());
                if (isYoy) cur.setDepositBalanceYoY(growth);
                else cur.setDepositBalanceMoM(growth);
            }
        }
        return current;
    }

    @Override
    public List<FinPrepaySummaryVO> prepaySummary(ReportQueryParam param) {
        List<Long> permIds = ReportPermissionContext.get();
        if (ReportPermissionContext.hasNoPermission()) {
            return Collections.emptyList();
        }

        String endMonth = resolveEndMonth(param, permIds);
        if (endMonth == null) return Collections.emptyList();
        String startMonth = param.getStartMonth() != null ? param.getStartMonth() : prevMonths(endMonth, 11);

        List<FinPrepaySummaryVO> current = finMapper.selectPrepaySummary(
                param.getProjectId(), startMonth, endMonth, permIds);
        if (current == null) current = Collections.emptyList();

        if (param.getCompareMode() == null || param.getCompareMode() == CompareMode.NONE) {
            return current;
        }

        boolean isYoy = param.getCompareMode() == CompareMode.YOY;
        String prevStart = isYoy ? prevYearMonth(startMonth) : prevMonths(startMonth, 1);
        String prevEnd = isYoy ? prevYearMonth(endMonth) : prevMonths(endMonth, 1);

        List<FinPrepaySummaryVO> prev = finMapper.selectPrepaySummary(
                param.getProjectId(), prevStart, prevEnd, permIds);
        if (prev == null) prev = Collections.emptyList();

        Map<String, FinPrepaySummaryVO> prevMap = prev.stream().collect(
                Collectors.toMap(v -> buildKey(v.getTimeDim(), v.getProjectId(), null),
                        v -> v, (a, b) -> a));

        for (FinPrepaySummaryVO cur : current) {
            String prevTimeDim = isYoy ? prevYearMonth(cur.getTimeDim()) : prevMonths(cur.getTimeDim(), 1);
            FinPrepaySummaryVO p = prevMap.get(buildKey(prevTimeDim, cur.getProjectId(), null));
            if (p != null) {
                BigDecimal growth = PeriodCompareUtil.calcYoY(cur.getPrepayBalance(), p.getPrepayBalance());
                if (isYoy) cur.setPrepayBalanceYoY(growth);
                else cur.setPrepayBalanceMoM(growth);
            }
        }
        return current;
    }

    @Override
    public List<FinDeductionAdjustmentVO> deductionAdjustment(ReportQueryParam param) {
        List<Long> permIds = ReportPermissionContext.get();
        if (ReportPermissionContext.hasNoPermission()) {
            return Collections.emptyList();
        }

        String endMonth = resolveEndMonth(param, permIds);
        if (endMonth == null) return Collections.emptyList();
        String startMonth = param.getStartMonth() != null ? param.getStartMonth() : prevMonths(endMonth, 11);

        List<FinDeductionAdjustmentVO> current = finMapper.selectDeductionAdjustment(
                param.getProjectId(), param.getFeeItemType(), startMonth, endMonth, permIds);
        if (current == null) current = Collections.emptyList();

        if (param.getCompareMode() == null || param.getCompareMode() == CompareMode.NONE) {
            return current;
        }

        boolean isYoy = param.getCompareMode() == CompareMode.YOY;
        String prevStart;
        String prevEnd;
        if (isYoy) {
            prevStart = prevYearMonth(startMonth);
            prevEnd = prevYearMonth(endMonth);
        } else {
            int monthSpan = monthsBetween(startMonth, endMonth);
            prevEnd = prevMonths(startMonth, 1);
            prevStart = prevMonths(prevEnd, monthSpan - 1);
        }

        List<FinDeductionAdjustmentVO> prev = finMapper.selectDeductionAdjustment(
                param.getProjectId(), param.getFeeItemType(), prevStart, prevEnd, permIds);
        if (prev == null) prev = Collections.emptyList();

        Map<String, FinDeductionAdjustmentVO> prevMap = prev.stream().collect(
                Collectors.toMap(v -> buildKey(v.getTimeDim(), v.getProjectId(), v.getFeeItemType()),
                        v -> v, (a, b) -> a));

        for (FinDeductionAdjustmentVO cur : current) {
            String prevTimeDim = isYoy ? prevYearMonth(cur.getTimeDim()) : prevMonths(cur.getTimeDim(), 1);
            FinDeductionAdjustmentVO p = prevMap.get(buildKey(prevTimeDim, cur.getProjectId(), cur.getFeeItemType()));
            if (p != null) {
                cur.setDeductionYoY(PeriodCompareUtil.calcYoY(cur.getDeductionAmount(), p.getDeductionAmount()));
                cur.setAdjustmentYoY(PeriodCompareUtil.calcYoY(cur.getAdjustmentAmount(), p.getAdjustmentAmount()));
            }
        }
        return current;
    }

    // ==================== 私有辅助方法 ====================

    /** 将项目级汇总行聚合写入看板 VO */
    private void aggregateSummaryToVO(List<RptFinanceMonthly> summaries, FinDashboardVO vo) {
        BigDecimal totalReceivable = sumReceivable(summaries);
        BigDecimal totalReceived = sumReceived(summaries);
        BigDecimal totalOutstanding = summaries.stream()
                .map(r -> r.getOutstandingAmount() != null ? r.getOutstandingAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOverdue = sumOverdue(summaries);
        BigDecimal totalDeposit = summaries.stream()
                .map(r -> r.getDepositBalance() != null ? r.getDepositBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPrepay = summaries.stream()
                .map(r -> r.getPrepayBalance() != null ? r.getPrepayBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal collectionRate = totalReceivable.compareTo(BigDecimal.ZERO) > 0
                ? PeriodCompareUtil.calcPercentage(totalReceived, totalReceivable)
                : BigDecimal.ZERO;
        BigDecimal overdueRate = totalReceivable.compareTo(BigDecimal.ZERO) > 0
                ? PeriodCompareUtil.calcPercentage(totalOverdue, totalReceivable)
                : BigDecimal.ZERO;

        vo.setTotalReceivable(totalReceivable)
          .setTotalReceived(totalReceived)
          .setTotalOutstanding(totalOutstanding)
          .setTotalOverdue(totalOverdue)
          .setAvgCollectionRate(collectionRate)
          .setAvgOverdueRate(overdueRate)
          .setTotalDepositBalance(totalDeposit)
          .setTotalPrepayBalance(totalPrepay);
    }

    /** 合并账龄列表为总体汇总行（项目维度） */
    private FinAgingAnalysisVO mergeAgingList(List<FinAgingAnalysisVO> list) {
        FinAgingAnalysisVO merged = new FinAgingAnalysisVO();
        for (FinAgingAnalysisVO item : list) {
            merged.setWithin30(safeAdd(merged.getWithin30(), item.getWithin30()));
            merged.setDays3160(safeAdd(merged.getDays3160(), item.getDays3160()));
            merged.setDays6190(safeAdd(merged.getDays6190(), item.getDays6190()));
            merged.setDays91180(safeAdd(merged.getDays91180(), item.getDays91180()));
            merged.setDays181365(safeAdd(merged.getDays181365(), item.getDays181365()));
            merged.setOver365(safeAdd(merged.getOver365(), item.getOver365()));
            merged.setTotalOutstanding(safeAdd(merged.getTotalOutstanding(), item.getTotalOutstanding()));
        }
        return merged;
    }

    /** 计算账龄各分档占比（回填到 VO） */
    private void calcAgingRates(FinAgingAnalysisVO vo) {
        BigDecimal total = vo.getTotalOutstanding();
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) return;
        vo.setWithin30Rate(PeriodCompareUtil.calcPercentage(vo.getWithin30(), total));
        vo.setDays3160Rate(PeriodCompareUtil.calcPercentage(vo.getDays3160(), total));
        vo.setDays6190Rate(PeriodCompareUtil.calcPercentage(vo.getDays6190(), total));
        vo.setDays91180Rate(PeriodCompareUtil.calcPercentage(vo.getDays91180(), total));
        vo.setDays181365Rate(PeriodCompareUtil.calcPercentage(vo.getDays181365(), total));
        vo.setOver365Rate(PeriodCompareUtil.calcPercentage(vo.getOver365(), total));
    }

    /** 解析结束月份：优先用 param.endMonth，否则取数据库最大月份 */
    private String resolveEndMonth(ReportQueryParam param, List<Long> permIds) {
        if (param.getEndMonth() != null) return param.getEndMonth();
        if (param.getStatMonth() != null) return param.getStatMonth();
        return finMapper.selectMaxStatMonth(param.getProjectId(), permIds);
    }

    /** 计算加权平均收缴率 */
    private BigDecimal calcWeightedCollectionRate(List<RptFinanceMonthly> list) {
        BigDecimal totalReceivable = sumReceivable(list);
        BigDecimal totalReceived = sumReceived(list);
        return totalReceivable.compareTo(BigDecimal.ZERO) > 0
                ? PeriodCompareUtil.calcPercentage(totalReceived, totalReceivable)
                : BigDecimal.ZERO;
    }

    /** 计算加权平均逾期率 */
    private BigDecimal calcWeightedOverdueRate(List<RptFinanceMonthly> list) {
        BigDecimal totalReceivable = sumReceivable(list);
        BigDecimal totalOverdue = sumOverdue(list);
        return totalReceivable.compareTo(BigDecimal.ZERO) > 0
                ? PeriodCompareUtil.calcPercentage(totalOverdue, totalReceivable)
                : BigDecimal.ZERO;
    }

    private BigDecimal sumReceivable(List<RptFinanceMonthly> list) {
        return list.stream().map(r -> r.getReceivableAmount() != null ? r.getReceivableAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumReceived(List<RptFinanceMonthly> list) {
        return list.stream().map(r -> r.getReceivedAmount() != null ? r.getReceivedAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumOverdue(List<RptFinanceMonthly> list) {
        return list.stream().map(r -> r.getOverdueAmount() != null ? r.getOverdueAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal safeAdd(BigDecimal a, BigDecimal b) {
        return (a != null ? a : BigDecimal.ZERO).add(b != null ? b : BigDecimal.ZERO);
    }

    /** 上一年同月：YYYY-MM → (YYYY-1)-MM */
    private String prevYearMonth(String statMonth) {
        return PeriodCompareUtil.previousYearMonth(statMonth);
    }

    /** 往前 N 个月：如 prevMonths("2026-03", 11) = "2025-04" */
    private String prevMonths(String statMonth, int months) {
        YearMonth ym = YearMonth.parse(statMonth);
        return ym.minusMonths(months).toString();
    }

    /** 两个月份之间的月数差 */
    private int monthsBetween(String startMonth, String endMonth) {
        YearMonth s = YearMonth.parse(startMonth);
        YearMonth e = YearMonth.parse(endMonth);
        long diff = s.until(e, java.time.temporal.ChronoUnit.MONTHS);
        return (int) Math.max(diff, 0) + 1;
    }

    /** 构建同比/环比索引键 */
    private String buildKey(String timeDim, Long projectId, String feeItemType) {
        return (timeDim != null ? timeDim : "") + "_"
                + (projectId != null ? projectId : "") + "_"
                + (feeItemType != null ? feeItemType : "");
    }
}
