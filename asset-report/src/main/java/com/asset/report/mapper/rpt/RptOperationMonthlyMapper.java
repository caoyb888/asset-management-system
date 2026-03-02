package com.asset.report.mapper.rpt;

import com.asset.report.entity.RptOperationMonthly;
import com.asset.report.vo.opr.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 营运月汇总表 Mapper
 */
@Mapper
public interface RptOperationMonthlyMapper extends BaseMapper<RptOperationMonthly> {

    // ==================== ETL 专用 ====================

    int upsertBatch(@Param("list") List<RptOperationMonthly> list);

    int deleteByStatMonth(@Param("statMonth") String statMonth);

    // ==================== 报表查询 ====================

    /**
     * 查询最新统计月份
     */
    String selectMaxStatMonth(
            @Param("projectId") Long projectId,
            @Param("permIds") List<Long> permIds);

    /**
     * 查询指定月份的项目级汇总行（building_id=0, format_type=''）
     */
    List<RptOperationMonthly> selectProjectSummaryByMonth(
            @Param("statMonth") String statMonth,
            @Param("projectId") Long projectId,
            @Param("permIds") List<Long> permIds);

    /**
     * 按时间维度聚合营收趋势
     *
     * @param projectId   项目ID（null=全部可见项目）
     * @param formatType  业态（null=不过滤）
     * @param startMonth  开始月份（yyyy-MM）
     * @param endMonth    结束月份（yyyy-MM）
     * @param permIds     数据权限项目ID列表
     */
    List<OprTrendVO> selectRevenueTrend(
            @Param("projectId") Long projectId,
            @Param("formatType") String formatType,
            @Param("startMonth") String startMonth,
            @Param("endMonth") String endMonth,
            @Param("permIds") List<Long> permIds);

    /**
     * 营收填报汇总（支持时间聚合）
     */
    List<OprRevenueSummaryVO> selectRevenueSummary(
            @Param("projectId") Long projectId,
            @Param("formatType") String formatType,
            @Param("startMonth") String startMonth,
            @Param("endMonth") String endMonth,
            @Param("permIds") List<Long> permIds);

    /**
     * 合同变更统计
     */
    List<OprContractChangeVO> selectContractChanges(
            @Param("projectId") Long projectId,
            @Param("formatType") String formatType,
            @Param("startMonth") String startMonth,
            @Param("endMonth") String endMonth,
            @Param("permIds") List<Long> permIds);

    /**
     * 租金变更分析
     */
    List<OprRentChangeVO> selectRentChanges(
            @Param("projectId") Long projectId,
            @Param("formatType") String formatType,
            @Param("startMonth") String startMonth,
            @Param("endMonth") String endMonth,
            @Param("permIds") List<Long> permIds);

    /**
     * 合同到期预警（按项目，分30/60/90天档）
     * <p>
     * 注意：rpt_operation_monthly 中 expiring_contracts 仅存储90天内汇总数。
     * 30/60天分档通过查询比例模拟（精确分档需实时查 inv_lease_contract）。
     * </p>
     */
    List<OprExpiringContractVO> selectExpiringContracts(
            @Param("statMonth") String statMonth,
            @Param("projectId") Long projectId,
            @Param("permIds") List<Long> permIds);

    /**
     * 地区业务对比（各项目多维指标，供雷达图使用）
     */
    List<OprRegionCompareVO> selectRegionCompare(
            @Param("statMonth") String statMonth,
            @Param("projectId") Long projectId,
            @Param("permIds") List<Long> permIds);

    /**
     * 客流数据分析（P1）
     */
    List<OprPassengerFlowVO> selectPassengerFlow(
            @Param("projectId") Long projectId,
            @Param("startMonth") String startMonth,
            @Param("endMonth") String endMonth,
            @Param("permIds") List<Long> permIds);

    /**
     * 解约统计（P1）
     */
    List<OprTerminationStatsVO> selectTerminationStats(
            @Param("projectId") Long projectId,
            @Param("formatType") String formatType,
            @Param("startMonth") String startMonth,
            @Param("endMonth") String endMonth,
            @Param("permIds") List<Long> permIds);

    /**
     * 浮动租金统计（P1）
     */
    List<OprFloatingRentVO> selectFloatingRent(
            @Param("projectId") Long projectId,
            @Param("formatType") String formatType,
            @Param("startMonth") String startMonth,
            @Param("endMonth") String endMonth,
            @Param("permIds") List<Long> permIds);

    /**
     * 合同台账变动（P1）
     * <p>按月份维度汇总变更次数、变更租金影响额、解约数、到期数</p>
     */
    List<OprLedgerChangeVO> selectLedgerChanges(
            @Param("projectId") Long projectId,
            @Param("formatType") String formatType,
            @Param("startMonth") String startMonth,
            @Param("endMonth") String endMonth,
            @Param("permIds") List<Long> permIds);
}
