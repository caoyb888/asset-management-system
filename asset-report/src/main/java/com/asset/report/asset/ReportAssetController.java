package com.asset.report.asset;

import com.asset.common.model.R;
import com.asset.report.common.param.ReportQueryParam;
import com.asset.report.common.permission.RptDataScope;
import com.asset.report.vo.asset.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 资产类报表接口
 * <p>
 * 路径前缀：/rpt/asset（不含 /api，由网关/代理统一加前缀）
 * Knife4j 分组：01-资产类报表
 * 所有接口均通过 {@link RptDataScope} 注解自动注入数据权限。
 * </p>
 */
@Tag(name = "资产类报表", description = "基于 rpt_asset_daily 的空置率/出租率/开业率等资产分析接口")
@RestController
@RequestMapping("/rpt/asset")
@RequiredArgsConstructor
public class ReportAssetController {

    private final ReportAssetService assetService;

    // ==================== P0 接口 ====================

    /**
     * 资产数据看板（聚合接口）
     * <p>
     * 一次返回：核心指标摘要 + 同比/环比 + 30天趋势折线图数据 + 项目对比柱状图数据。
     * 响应时间目标 &lt; 3s。
     * </p>
     */
    @Operation(summary = "资产数据看板", description = "聚合接口，一次返回所有看板图表数据，减少 HTTP 请求数")
    @GetMapping("/dashboard")
    @RptDataScope
    public R<AssetDashboardVO> dashboard(
            @Parameter(description = "查询参数（projectId/compareMode/startDate/endDate）")
            ReportQueryParam param) {
        return R.ok(assetService.dashboard(param));
    }

    /**
     * 空置率统计（支持时间趋势、同比/环比）
     */
    @Operation(summary = "空置率统计",
            description = "按时间维度（DAY/WEEK/MONTH/YEAR）返回空置率趋势，compareMode=YOY/MOM 时附带增长率")
    @GetMapping("/vacancy-rate")
    @RptDataScope
    public R<List<RateTrendVO>> vacancyRate(
            @Parameter(description = "查询参数（projectId/startDate/endDate/timeUnit/compareMode/buildingId/floorId/formatType）")
            ReportQueryParam param) {
        return R.ok(assetService.vacancyRate(param));
    }

    /**
     * 出租率统计（支持时间趋势、同比/环比）
     */
    @Operation(summary = "出租率统计",
            description = "按时间维度返回出租率趋势，compareMode=YOY/MOM 时附带增长率")
    @GetMapping("/rental-rate")
    @RptDataScope
    public R<List<RateTrendVO>> rentalRate(
            @Parameter(description = "查询参数（同空置率）")
            ReportQueryParam param) {
        return R.ok(assetService.rentalRate(param));
    }

    /**
     * 开业率统计（支持时间趋势、同比/环比）
     */
    @Operation(summary = "开业率统计",
            description = "按时间维度返回开业率趋势，compareMode=YOY/MOM 时附带增长率")
    @GetMapping("/opening-rate")
    @RptDataScope
    public R<List<RateTrendVO>> openingRate(
            @Parameter(description = "查询参数（同空置率）")
            ReportQueryParam param) {
        return R.ok(assetService.openingRate(param));
    }

    /**
     * 商铺租赁信息报表（楼栋/楼层粒度，分页）
     * <p>
     * 支持按项目/楼栋/楼层/业态筛选，展示商铺总数/已租/空置/装修/开业及三率。
     * </p>
     */
    @Operation(summary = "商铺租赁信息报表",
            description = "返回楼栋/楼层粒度的租赁状态统计，支持多维筛选和年/月/周切换（statDate 不传时取最新日期）")
    @GetMapping("/shop-rental")
    @RptDataScope
    public R<IPage<ShopRentalVO>> shopRental(
            @Parameter(description = "查询参数（projectId/buildingId/floorId/formatType/statDate/pageNum/pageSize）")
            ReportQueryParam param) {
        return R.ok(assetService.shopRental(param));
    }

    /**
     * 品牌/业态分布报表
     * <p>
     * 按业态（format_type）分组统计商铺数量、面积及占比，用于渲染热力图/树状图。
     * </p>
     */
    @Operation(summary = "品牌/业态分布报表",
            description = "按业态分组，返回各业态商铺数量、面积、出租率及占比，statDate 不传时取最新日期")
    @GetMapping("/brand-distribution")
    @RptDataScope
    public R<List<BrandDistributionVO>> brandDistribution(
            @Parameter(description = "查询参数（projectId/statDate）")
            ReportQueryParam param) {
        return R.ok(assetService.brandDistribution(param));
    }

    // ==================== P1 接口 ====================

    /**
     * 商铺拆分合并报表（P1）
     * <p>
     * 展示有拆分/合并关系的商铺记录（通过 parent_shop_id 关联），含面积变动信息。
     * </p>
     */
    @Operation(summary = "商铺拆分合并报表（P1）",
            description = "展示有拆合历史的商铺，SPLIT=由父商铺拆分，MERGE=合并商铺，ORIGINAL=无拆合历史")
    @GetMapping("/shop-split-merge")
    @RptDataScope
    public R<IPage<ShopSplitMergeVO>> shopSplitMerge(
            @Parameter(description = "查询参数（projectId/buildingId/pageNum/pageSize）")
            ReportQueryParam param) {
        return R.ok(assetService.shopSplitMerge(param));
    }

    /**
     * 商家分布报表（P1）
     * <p>
     * 基于 rpt_asset_daily 楼层粒度，展示各楼层/业态的商家入驻分布情况。
     * </p>
     */
    @Operation(summary = "商家分布报表（P1）",
            description = "按楼栋/楼层/业态展示已签约商家分布及出租率，statDate 不传时取最新日期")
    @GetMapping("/merchant-distribution")
    @RptDataScope
    public R<List<MerchantDistributionVO>> merchantDistribution(
            @Parameter(description = "查询参数（projectId/buildingId/statDate）")
            ReportQueryParam param) {
        return R.ok(assetService.merchantDistribution(param));
    }

    /**
     * 区域归属报表（P1）
     * <p>
     * 按省/市维度汇总资产指标，JOIN biz_project 获取地理信息。
     * </p>
     */
    @Operation(summary = "区域归属报表（P1）",
            description = "按省/市归纳项目数量、商铺数量、面积及三率，statDate 不传时取最新日期")
    @GetMapping("/region-summary")
    @RptDataScope
    public R<List<RegionSummaryVO>> regionSummary(
            @Parameter(description = "查询参数（statDate）")
            ReportQueryParam param) {
        return R.ok(assetService.regionSummary(param));
    }

    /**
     * 经营面积统计（P1）
     * <p>
     * 按项目汇总各维度面积数据，附带已租面积同比增长率。
     * </p>
     */
    @Operation(summary = "经营面积统计（P1）",
            description = "按项目展示总面积/已租面积/空置面积/装修面积及同比增长，statDate 不传时取最新日期")
    @GetMapping("/area-summary")
    @RptDataScope
    public R<List<AreaSummaryVO>> areaSummary(
            @Parameter(description = "查询参数（statDate）")
            ReportQueryParam param) {
        return R.ok(assetService.areaSummary(param));
    }
}
