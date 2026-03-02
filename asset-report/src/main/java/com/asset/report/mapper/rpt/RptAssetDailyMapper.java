package com.asset.report.mapper.rpt;

import com.asset.report.entity.RptAssetDaily;
import com.asset.report.vo.asset.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 资产日汇总表 Mapper
 */
@Mapper
public interface RptAssetDailyMapper extends BaseMapper<RptAssetDaily> {

    // ==================== ETL 专用 ====================

    /**
     * 批量幂等写入（ON DUPLICATE KEY UPDATE）
     */
    int upsertBatch(@Param("list") List<RptAssetDaily> list);

    /**
     * 删除指定日期的旧数据（重跑前清理）
     */
    int deleteByStatDate(@Param("statDate") LocalDate statDate);

    // ==================== 报表查询 ====================

    /**
     * 查询最新统计日期（受数据权限限制）
     *
     * @param projectId 指定项目（null=不限制）
     * @param permIds   可见项目ID列表（null=管理员无限制，空列表=无权限）
     * @return 最新 stat_date，无数据时返回 null
     */
    LocalDate selectMaxStatDate(@Param("projectId") Long projectId,
                                @Param("permIds") List<Long> permIds);

    /**
     * 查询指定日期、项目级汇总数据（看板核心数据）
     * 行条件：building_id=0, floor_id=0, format_type=''（ETL 写入的项目级聚合行）
     *
     * @param statDate  统计日期
     * @param projectId 指定单个项目（null=不限制）
     * @param permIds   数据权限
     */
    List<RptAssetDaily> selectProjectSummaryByDate(@Param("statDate") LocalDate statDate,
                                                   @Param("projectId") Long projectId,
                                                   @Param("permIds") List<Long> permIds);

    /**
     * 指标趋势查询（时间序列）
     * <p>
     * 按 timeUnit 聚合（DAY/WEEK/MONTH/YEAR），返回指定指标的时间序列均值，
     * 支持按项目/楼栋/楼层/业态过滤。
     * </p>
     *
     * @param projectId  项目ID（null=汇总所有可见项目）
     * @param buildingId 楼栋ID（0 或 null=不限制）
     * @param floorId    楼层ID（0 或 null=不限制）
     * @param formatType 业态（null 或 ""=不限制）
     * @param startDate  起始日期（含）
     * @param endDate    结束日期（含）
     * @param timeUnit   时间聚合维度（DAY/WEEK/MONTH/YEAR）
     * @param metric     指标字段名（vacancy_rate / rental_rate / opening_rate）
     * @param permIds    数据权限
     */
    List<RateTrendVO> selectRateTrend(@Param("projectId") Long projectId,
                                      @Param("buildingId") Long buildingId,
                                      @Param("floorId") Long floorId,
                                      @Param("formatType") String formatType,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate,
                                      @Param("timeUnit") String timeUnit,
                                      @Param("metric") String metric,
                                      @Param("permIds") List<Long> permIds);

    /**
     * 商铺租赁信息分页查询（楼栋/楼层粒度）
     * <p>
     * 返回楼栋级或楼层级数据，可按项目/楼栋/楼层/业态过滤。
     * </p>
     */
    IPage<ShopRentalVO> selectShopRentalPage(IPage<?> page,
                                             @Param("statDate") LocalDate statDate,
                                             @Param("projectId") Long projectId,
                                             @Param("buildingId") Long buildingId,
                                             @Param("floorId") Long floorId,
                                             @Param("formatType") String formatType,
                                             @Param("permIds") List<Long> permIds);

    /**
     * 品牌/业态分布统计（按 format_type 分组）
     *
     * @param statDate  统计日期
     * @param projectId 项目ID（null=汇总）
     * @param permIds   数据权限
     */
    List<BrandDistributionVO> selectBrandDistribution(@Param("statDate") LocalDate statDate,
                                                      @Param("projectId") Long projectId,
                                                      @Param("permIds") List<Long> permIds);

    /**
     * 项目对比数据（看板用，最新日期各项目指标）
     *
     * @param statDate 统计日期（通常为最新日期）
     * @param permIds  数据权限
     */
    List<ProjectCompareVO> selectProjectComparison(@Param("statDate") LocalDate statDate,
                                                   @Param("permIds") List<Long> permIds);

    /**
     * 经营面积统计（P1，按项目汇总面积指标）
     *
     * @param statDate  统计日期
     * @param permIds   数据权限
     */
    List<AreaSummaryVO> selectAreaSummary(@Param("statDate") LocalDate statDate,
                                         @Param("permIds") List<Long> permIds);

    /**
     * 区域归属汇总（P1，JOIN biz_project 获取省市信息）
     *
     * @param statDate 统计日期
     * @param permIds  数据权限
     */
    List<RegionSummaryVO> selectRegionSummary(@Param("statDate") LocalDate statDate,
                                              @Param("permIds") List<Long> permIds);

    /**
     * 商家分布报表（P1，楼层粒度，building_id/floor_id != 0）
     *
     * @param statDate  统计日期
     * @param projectId 项目ID（null=不限制）
     * @param buildingId 楼栋ID（null=不限制）
     * @param permIds   数据权限
     */
    List<MerchantDistributionVO> selectMerchantDistribution(@Param("statDate") LocalDate statDate,
                                                            @Param("projectId") Long projectId,
                                                            @Param("buildingId") Long buildingId,
                                                            @Param("permIds") List<Long> permIds);

    /**
     * 商铺拆合报表（P1，查询 biz_shop 中有父子关联的商铺记录）
     *
     * @param projectId  项目ID（null=不限制）
     * @param buildingId 楼栋ID（null=不限制）
     * @param permIds    数据权限
     * @param offset     分页偏移
     * @param limit      每页条数
     */
    List<ShopSplitMergeVO> selectShopSplitMerge(@Param("projectId") Long projectId,
                                                 @Param("buildingId") Long buildingId,
                                                 @Param("permIds") List<Long> permIds,
                                                 @Param("offset") int offset,
                                                 @Param("limit") int limit);

    /**
     * 商铺拆合报表总数（P1，用于分页）
     */
    long countShopSplitMerge(@Param("projectId") Long projectId,
                              @Param("buildingId") Long buildingId,
                              @Param("permIds") List<Long> permIds);
}
