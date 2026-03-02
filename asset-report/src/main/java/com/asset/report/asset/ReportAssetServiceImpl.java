package com.asset.report.asset;

import com.asset.report.common.param.ReportQueryParam;
import com.asset.report.common.param.ReportQueryParam.CompareMode;
import com.asset.report.common.permission.ReportPermissionContext;
import com.asset.report.common.util.PeriodCompareUtil;
import com.asset.report.entity.RptAssetDaily;
import com.asset.report.mapper.rpt.RptAssetDailyMapper;
import com.asset.report.vo.asset.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 资产类报表 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportAssetServiceImpl implements ReportAssetService {

    private final RptAssetDailyMapper assetDailyMapper;

    // ==================== 看板（P0） ====================

    @Override
    public AssetDashboardVO dashboard(ReportQueryParam param) {
        List<Long> permIds = ReportPermissionContext.get();
        if (ReportPermissionContext.hasNoPermission()) {
            return new AssetDashboardVO();
        }

        // 1. 确定最新统计日期
        LocalDate latestDate = assetDailyMapper.selectMaxStatDate(param.getProjectId(), permIds);
        if (latestDate == null) {
            log.warn("[AssetDashboard] rpt_asset_daily 无数据，返回空看板");
            return new AssetDashboardVO();
        }

        // 2. 查询最新日期的项目级汇总
        List<RptAssetDaily> summaries = assetDailyMapper.selectProjectSummaryByDate(
                latestDate, param.getProjectId(), permIds);

        AssetDashboardVO vo = new AssetDashboardVO().setLatestDate(latestDate);

        // 3. 聚合核心指标
        if (!summaries.isEmpty()) {
            aggregateSummaryToVO(summaries, vo);
        }

        // 4. 30天趋势数据
        LocalDate trendEnd = latestDate;
        LocalDate trendStart = latestDate.minusDays(29);
        vo.setVacancyTrend(queryTrend(param.getProjectId(), null, null, null,
                trendStart, trendEnd, "DAY", "vacancy_rate", permIds));
        vo.setRentalTrend(queryTrend(param.getProjectId(), null, null, null,
                trendStart, trendEnd, "DAY", "rental_rate", permIds));
        vo.setOpeningTrend(queryTrend(param.getProjectId(), null, null, null,
                trendStart, trendEnd, "DAY", "opening_rate", permIds));

        // 5. 同比（YoY）：去年同日
        LocalDate yoyDate = latestDate.minusYears(1);
        List<RptAssetDaily> yoySummaries = assetDailyMapper.selectProjectSummaryByDate(
                yoyDate, param.getProjectId(), permIds);
        if (!yoySummaries.isEmpty()) {
            RptAssetDaily yoy = aggregateSummary(yoySummaries);
            vo.setVacancyRateYoY(PeriodCompareUtil.calcYoY(vo.getVacancyRate(), yoy.getVacancyRate()));
            vo.setRentalRateYoY(PeriodCompareUtil.calcYoY(vo.getRentalRate(), yoy.getRentalRate()));
            vo.setOpeningRateYoY(PeriodCompareUtil.calcYoY(vo.getOpeningRate(), yoy.getOpeningRate()));
        }

        // 6. 环比（MoM）：上月同日
        LocalDate momDate = latestDate.minusMonths(1);
        List<RptAssetDaily> momSummaries = assetDailyMapper.selectProjectSummaryByDate(
                momDate, param.getProjectId(), permIds);
        if (!momSummaries.isEmpty()) {
            RptAssetDaily mom = aggregateSummary(momSummaries);
            vo.setVacancyRateMoM(PeriodCompareUtil.calcMoM(vo.getVacancyRate(), mom.getVacancyRate()));
            vo.setRentalRateMoM(PeriodCompareUtil.calcMoM(vo.getRentalRate(), mom.getRentalRate()));
            vo.setOpeningRateMoM(PeriodCompareUtil.calcMoM(vo.getOpeningRate(), mom.getOpeningRate()));
        }

        // 7. 项目对比数据
        List<ProjectCompareVO> comparison = assetDailyMapper.selectProjectComparison(latestDate, permIds);
        vo.setProjectComparison(comparison != null ? comparison : Collections.emptyList());

        return vo;
    }

    // ==================== 三率趋势（P0） ====================

    @Override
    public List<RateTrendVO> vacancyRate(ReportQueryParam param) {
        return queryRateTrendWithCompare(param, "vacancy_rate");
    }

    @Override
    public List<RateTrendVO> rentalRate(ReportQueryParam param) {
        return queryRateTrendWithCompare(param, "rental_rate");
    }

    @Override
    public List<RateTrendVO> openingRate(ReportQueryParam param) {
        return queryRateTrendWithCompare(param, "opening_rate");
    }

    // ==================== 商铺租赁信息（P0） ====================

    @Override
    public IPage<ShopRentalVO> shopRental(ReportQueryParam param) {
        if (ReportPermissionContext.hasNoPermission()) {
            return Page.of(param.getPageNum(), param.getPageSize());
        }
        List<Long> permIds = ReportPermissionContext.get();

        // 未指定日期时使用最新日期
        LocalDate statDate = param.getStatDate() != null
                ? param.getStatDate()
                : assetDailyMapper.selectMaxStatDate(param.getProjectId(), permIds);
        if (statDate == null) {
            return Page.of(param.getPageNum(), param.getPageSize());
        }

        IPage<ShopRentalVO> page = new Page<>(param.getPageNum(), param.getPageSize());
        return assetDailyMapper.selectShopRentalPage(page, statDate,
                param.getProjectId(), param.getBuildingId(), param.getFloorId(),
                param.getFormatType(), permIds);
    }

    // ==================== 品牌分布（P0） ====================

    @Override
    public List<BrandDistributionVO> brandDistribution(ReportQueryParam param) {
        if (ReportPermissionContext.hasNoPermission()) {
            return Collections.emptyList();
        }
        List<Long> permIds = ReportPermissionContext.get();

        LocalDate statDate = resolveStatDate(param, permIds);
        if (statDate == null) return Collections.emptyList();

        List<BrandDistributionVO> list = assetDailyMapper.selectBrandDistribution(
                statDate, param.getProjectId(), permIds);
        if (list == null || list.isEmpty()) return Collections.emptyList();

        // 计算占比（分子=本业态，分母=全量）
        int totalShopsAll = list.stream().mapToInt(v -> v.getTotalShops() == null ? 0 : v.getTotalShops()).sum();
        BigDecimal totalAreaAll = list.stream()
                .map(v -> v.getTotalArea() == null ? BigDecimal.ZERO : v.getTotalArea())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        list.forEach(v -> {
            v.setShopPercentage(PeriodCompareUtil.calcPercentage(
                    BigDecimal.valueOf(v.getTotalShops() == null ? 0 : v.getTotalShops()),
                    BigDecimal.valueOf(totalShopsAll)));
            v.setAreaPercentage(PeriodCompareUtil.calcPercentage(
                    v.getTotalArea(), totalAreaAll));
        });
        return list;
    }

    // ==================== P1 接口 ====================

    @Override
    public IPage<ShopSplitMergeVO> shopSplitMerge(ReportQueryParam param) {
        if (ReportPermissionContext.hasNoPermission()) {
            return Page.of(param.getPageNum(), param.getPageSize());
        }
        List<Long> permIds = ReportPermissionContext.get();
        int pageNum = param.getPageNum() != null ? param.getPageNum() : 1;
        int pageSize = param.getPageSize() != null ? param.getPageSize() : 20;
        int offset = (pageNum - 1) * pageSize;

        long total = assetDailyMapper.countShopSplitMerge(
                param.getProjectId(), param.getBuildingId(), permIds);
        List<ShopSplitMergeVO> records = assetDailyMapper.selectShopSplitMerge(
                param.getProjectId(), param.getBuildingId(), permIds, offset, pageSize);

        Page<ShopSplitMergeVO> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(records != null ? records : Collections.emptyList());
        return page;
    }

    @Override
    public List<MerchantDistributionVO> merchantDistribution(ReportQueryParam param) {
        if (ReportPermissionContext.hasNoPermission()) {
            return Collections.emptyList();
        }
        List<Long> permIds = ReportPermissionContext.get();
        LocalDate statDate = resolveStatDate(param, permIds);
        if (statDate == null) return Collections.emptyList();

        List<MerchantDistributionVO> list = assetDailyMapper.selectMerchantDistribution(
                statDate, param.getProjectId(), param.getBuildingId(), permIds);
        return list != null ? list : Collections.emptyList();
    }

    @Override
    public List<RegionSummaryVO> regionSummary(ReportQueryParam param) {
        if (ReportPermissionContext.hasNoPermission()) {
            return Collections.emptyList();
        }
        List<Long> permIds = ReportPermissionContext.get();
        LocalDate statDate = resolveStatDate(param, permIds);
        if (statDate == null) return Collections.emptyList();

        List<RegionSummaryVO> list = assetDailyMapper.selectRegionSummary(statDate, permIds);
        return list != null ? list : Collections.emptyList();
    }

    @Override
    public List<AreaSummaryVO> areaSummary(ReportQueryParam param) {
        if (ReportPermissionContext.hasNoPermission()) {
            return Collections.emptyList();
        }
        List<Long> permIds = ReportPermissionContext.get();
        LocalDate statDate = resolveStatDate(param, permIds);
        if (statDate == null) return Collections.emptyList();

        List<AreaSummaryVO> current = assetDailyMapper.selectAreaSummary(statDate, permIds);
        if (current == null || current.isEmpty()) return Collections.emptyList();

        // 计算同比（YoY）：去年同日数据
        LocalDate yoyDate = statDate.minusYears(1);
        List<AreaSummaryVO> yoyList = assetDailyMapper.selectAreaSummary(yoyDate, permIds);
        if (yoyList != null && !yoyList.isEmpty()) {
            Map<Long, AreaSummaryVO> yoyMap = yoyList.stream()
                    .collect(Collectors.toMap(AreaSummaryVO::getProjectId, v -> v, (a, b) -> a));
            current.forEach(v -> {
                AreaSummaryVO yoy = yoyMap.get(v.getProjectId());
                if (yoy != null) {
                    v.setRentedAreaYoY(PeriodCompareUtil.calcYoY(v.getRentedArea(), yoy.getRentedArea()));
                    v.setVacancyRateYoY(PeriodCompareUtil.calcYoY(v.getVacancyRate(), yoy.getVacancyRate()));
                }
            });
        }
        return current;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 通用趋势查询（带同比/环比叠加）
     */
    private List<RateTrendVO> queryRateTrendWithCompare(ReportQueryParam param, String metric) {
        if (ReportPermissionContext.hasNoPermission()) {
            return Collections.emptyList();
        }
        List<Long> permIds = ReportPermissionContext.get();

        LocalDate startDate = param.getStartDate();
        LocalDate endDate = param.getEndDate();
        // 默认近30天
        if (endDate == null) {
            endDate = assetDailyMapper.selectMaxStatDate(param.getProjectId(), permIds);
            if (endDate == null) return Collections.emptyList();
        }
        if (startDate == null) {
            startDate = endDate.minusDays(29);
        }

        String timeUnit = param.getTimeUnit() != null ? param.getTimeUnit().name() : "DAY";
        List<RateTrendVO> current = queryTrend(param.getProjectId(),
                param.getBuildingId(), param.getFloorId(), param.getFormatType(),
                startDate, endDate, timeUnit, metric, permIds);

        // 若无对比模式，直接返回
        if (param.getCompareMode() == CompareMode.NONE || param.getCompareMode() == null) {
            return current;
        }

        // 计算上期时间范围
        LocalDate prevStart;
        LocalDate prevEnd;
        if (param.getCompareMode() == CompareMode.YOY) {
            prevStart = PeriodCompareUtil.previousYearPeriod(startDate, param.getTimeUnit());
            prevEnd   = PeriodCompareUtil.previousYearPeriod(endDate, param.getTimeUnit());
        } else { // MOM
            prevStart = PeriodCompareUtil.previousPeriod(startDate, param.getTimeUnit());
            prevEnd   = PeriodCompareUtil.previousPeriod(endDate, param.getTimeUnit());
        }

        List<RateTrendVO> previous = queryTrend(param.getProjectId(),
                param.getBuildingId(), param.getFloorId(), param.getFormatType(),
                prevStart, prevEnd, timeUnit, metric, permIds);

        // 按 timeDim 合并
        Map<String, BigDecimal> prevMap = previous.stream()
                .filter(v -> v.getTimeDim() != null && v.getValue() != null)
                .collect(Collectors.toMap(RateTrendVO::getTimeDim, RateTrendVO::getValue, (a, b) -> a));

        current.forEach(v -> {
            BigDecimal prev = prevMap.get(v.getTimeDim());
            if (prev != null) {
                v.setPrevValue(prev);
                v.setGrowthRate(PeriodCompareUtil.calcGrowthRate(v.getValue(), prev));
            }
        });
        return current;
    }

    /**
     * 调用 Mapper 查趋势数据（不含对比）
     */
    private List<RateTrendVO> queryTrend(Long projectId, Long buildingId, Long floorId,
                                          String formatType, LocalDate startDate, LocalDate endDate,
                                          String timeUnit, String metric, List<Long> permIds) {
        List<RateTrendVO> result = assetDailyMapper.selectRateTrend(
                projectId, buildingId, floorId, formatType,
                startDate, endDate, timeUnit, metric, permIds);
        return result != null ? result : Collections.emptyList();
    }

    /**
     * 聚合多项目汇总数据到看板 VO（加权平均三率）
     */
    private void aggregateSummaryToVO(List<RptAssetDaily> summaries, AssetDashboardVO vo) {
        int totalShops = 0, rentedShops = 0, vacantShops = 0, decoratingShops = 0, openedShops = 0;
        BigDecimal totalArea = BigDecimal.ZERO, rentedArea = BigDecimal.ZERO, vacantArea = BigDecimal.ZERO;

        for (RptAssetDaily r : summaries) {
            totalShops      += r.getTotalShops() != null ? r.getTotalShops() : 0;
            rentedShops     += r.getRentedShops() != null ? r.getRentedShops() : 0;
            vacantShops     += r.getVacantShops() != null ? r.getVacantShops() : 0;
            decoratingShops += r.getDecoratingShops() != null ? r.getDecoratingShops() : 0;
            openedShops     += r.getOpenedShops() != null ? r.getOpenedShops() : 0;
            totalArea    = totalArea.add(r.getTotalArea() != null ? r.getTotalArea() : BigDecimal.ZERO);
            rentedArea   = rentedArea.add(r.getRentedArea() != null ? r.getRentedArea() : BigDecimal.ZERO);
            vacantArea   = vacantArea.add(r.getVacantArea() != null ? r.getVacantArea() : BigDecimal.ZERO);
        }

        vo.setTotalShops(totalShops).setRentedShops(rentedShops)
                .setVacantShops(vacantShops).setDecoratingShops(decoratingShops)
                .setOpenedShops(openedShops)
                .setTotalArea(totalArea).setRentedArea(rentedArea).setVacantArea(vacantArea);

        // 加权平均三率（面积加权）
        vo.setVacancyRate(PeriodCompareUtil.calcPercentage(vacantArea, totalArea));
        vo.setRentalRate(PeriodCompareUtil.calcPercentage(rentedArea, totalArea));
        vo.setOpeningRate(PeriodCompareUtil.calcPercentage(
                BigDecimal.valueOf(openedShops), BigDecimal.valueOf(totalShops)));
    }

    /**
     * 聚合多项目列表为单条汇总（供同比/环比用）
     */
    private RptAssetDaily aggregateSummary(List<RptAssetDaily> summaries) {
        RptAssetDaily agg = new RptAssetDaily();
        AssetDashboardVO tmpVo = new AssetDashboardVO();
        aggregateSummaryToVO(summaries, tmpVo);
        agg.setVacancyRate(tmpVo.getVacancyRate());
        agg.setRentalRate(tmpVo.getRentalRate());
        agg.setOpeningRate(tmpVo.getOpeningRate());
        return agg;
    }

    /**
     * 解析统计日期：优先用 param.statDate，否则查最新日期
     */
    private LocalDate resolveStatDate(ReportQueryParam param, List<Long> permIds) {
        if (param.getStatDate() != null) return param.getStatDate();
        return assetDailyMapper.selectMaxStatDate(param.getProjectId(), permIds);
    }
}
