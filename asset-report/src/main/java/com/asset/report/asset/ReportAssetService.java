package com.asset.report.asset;

import com.asset.report.common.param.ReportQueryParam;
import com.asset.report.vo.asset.*;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 资产类报表 Service 接口
 * <p>
 * 对应 10 个 API：
 * <ul>
 *   <li>P0: dashboard / vacancyRate / rentalRate / openingRate / shopRental / brandDistribution</li>
 *   <li>P1: shopSplitMerge / merchantDistribution / regionSummary / areaSummary</li>
 * </ul>
 * 所有方法通过 {@link com.asset.report.common.permission.ReportPermissionContext} 读取当前用户可见项目范围。
 * </p>
 */
public interface ReportAssetService {

    /**
     * 资产数据看板（聚合接口）
     * <p>
     * 一次返回核心指标摘要 + 30天趋势 + 同比/环比 + 项目对比，
     * 减少前端多次 HTTP 请求。响应时间目标 &lt; 3s（含 Redis 缓存命中路径 &lt; 500ms）。
     * </p>
     *
     * @param param 查询参数（projectId/startDate/endDate/compareMode）
     * @return 资产看板聚合数据
     */
    AssetDashboardVO dashboard(ReportQueryParam param);

    /**
     * 空置率统计（支持趋势、同比/环比）
     *
     * @param param 查询参数（projectId/buildingId/floorId/formatType/startDate/endDate/timeUnit/compareMode）
     * @return 时间序列数据点列表
     */
    List<RateTrendVO> vacancyRate(ReportQueryParam param);

    /**
     * 出租率统计（支持趋势、同比/环比）
     *
     * @param param 查询参数（同上）
     * @return 时间序列数据点列表
     */
    List<RateTrendVO> rentalRate(ReportQueryParam param);

    /**
     * 开业率统计（支持趋势、同比/环比）
     *
     * @param param 查询参数（同上）
     * @return 时间序列数据点列表
     */
    List<RateTrendVO> openingRate(ReportQueryParam param);

    /**
     * 商铺租赁信息报表（楼栋/楼层粒度，分页）
     *
     * @param param 查询参数（projectId/buildingId/floorId/formatType/statDate/pageNum/pageSize）
     * @return 分页结果
     */
    IPage<ShopRentalVO> shopRental(ReportQueryParam param);

    /**
     * 品牌/业态分布报表
     *
     * @param param 查询参数（projectId/statDate）
     * @return 各业态分布统计列表（含占比）
     */
    List<BrandDistributionVO> brandDistribution(ReportQueryParam param);

    /**
     * 商铺拆分合并报表（P1）
     *
     * @param param 查询参数（projectId/buildingId/pageNum/pageSize）
     * @return 分页结果
     */
    IPage<ShopSplitMergeVO> shopSplitMerge(ReportQueryParam param);

    /**
     * 商家分布报表（P1）
     *
     * @param param 查询参数（projectId/buildingId/statDate）
     * @return 楼层维度商家分布列表
     */
    List<MerchantDistributionVO> merchantDistribution(ReportQueryParam param);

    /**
     * 区域归属报表（P1）
     *
     * @param param 查询参数（statDate）
     * @return 按省/市汇总的资产统计列表
     */
    List<RegionSummaryVO> regionSummary(ReportQueryParam param);

    /**
     * 经营面积统计（P1）
     *
     * @param param 查询参数（statDate，支持同比对比）
     * @return 各项目面积汇总列表（含同比增长率）
     */
    List<AreaSummaryVO> areaSummary(ReportQueryParam param);
}
