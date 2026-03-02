package com.asset.report.mapper.rpt;

import com.asset.report.entity.RptInvestmentDaily;
import com.asset.report.vo.inv.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 招商日汇总表 Mapper
 */
@Mapper
public interface RptInvestmentDailyMapper extends BaseMapper<RptInvestmentDaily> {

    // ==================== ETL 专用 ====================

    int upsertBatch(@Param("list") List<RptInvestmentDaily> list);

    int deleteByStatDate(@Param("statDate") LocalDate statDate);

    // ==================== 报表查询 ====================

    /**
     * 查询最新统计日期
     */
    LocalDate selectMaxStatDate(
            @Param("projectId") Long projectId,
            @Param("permIds") List<Long> permIds);

    /**
     * 查询指定日期的项目级汇总行（format_type='', investment_manager_id=0）
     */
    List<RptInvestmentDaily> selectProjectSummaryByDate(
            @Param("statDate") LocalDate statDate,
            @Param("projectId") Long projectId,
            @Param("permIds") List<Long> permIds);

    /**
     * 按时间维度聚合意向/合同趋势数据
     *
     * @param projectId       项目ID（null=全部可见项目）
     * @param formatType      业态（null=不过滤）
     * @param managerId       招商负责人ID（null=全员）
     * @param startDate       开始日期
     * @param endDate         结束日期
     * @param timeUnit        时间维度：DAY/WEEK/MONTH/YEAR
     * @param permIds         数据权限项目ID列表
     */
    List<InvTrendVO> selectInvTrend(
            @Param("projectId") Long projectId,
            @Param("formatType") String formatType,
            @Param("managerId") Long managerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("timeUnit") String timeUnit,
            @Param("permIds") List<Long> permIds);

    /**
     * 按时间维度聚合意向统计
     */
    List<IntentionStatsVO> selectIntentionStats(
            @Param("projectId") Long projectId,
            @Param("formatType") String formatType,
            @Param("managerId") Long managerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("timeUnit") String timeUnit,
            @Param("permIds") List<Long> permIds);

    /**
     * 按时间维度聚合合同统计
     */
    List<ContractStatsVO> selectContractStats(
            @Param("projectId") Long projectId,
            @Param("formatType") String formatType,
            @Param("managerId") Long managerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("timeUnit") String timeUnit,
            @Param("permIds") List<Long> permIds);

    /**
     * 漏斗数据：查询指定日期所有可见项目的漏斗阶段聚合值
     * 返回 intention_count / intention_signed / contract_count 的汇总
     */
    RptInvestmentDaily selectFunnelSummary(
            @Param("statDate") LocalDate statDate,
            @Param("projectId") Long projectId,
            @Param("permIds") List<Long> permIds);

    /**
     * 招商业绩：按项目/招商负责人查询业绩数据
     *
     * @param statDate   统计日期（取最新日期单点数据）
     * @param projectId  项目ID（null=全部）
     * @param managerId  招商负责人ID（null=全员），0=项目级汇总
     * @param permIds    数据权限
     */
    List<PerformanceVO> selectPerformance(
            @Param("statDate") LocalDate statDate,
            @Param("projectId") Long projectId,
            @Param("managerId") Long managerId,
            @Param("permIds") List<Long> permIds);

    /**
     * 租金水平分析：按项目+业态维度聚合平均租金
     */
    List<RentLevelVO> selectRentLevel(
            @Param("statDate") LocalDate statDate,
            @Param("projectId") Long projectId,
            @Param("managerId") Long managerId,
            @Param("permIds") List<Long> permIds);

    /**
     * 租决政策执行：按项目+业态维度聚合
     */
    List<PolicyExecutionVO> selectPolicyExecution(
            @Param("statDate") LocalDate statDate,
            @Param("projectId") Long projectId,
            @Param("permIds") List<Long> permIds);

    /**
     * 品牌签约排行：按业态或项目分组排行
     *
     * @param statDate  统计日期
     * @param projectId 项目ID（null=全部可见项目）
     * @param permIds   数据权限
     */
    List<BrandRankingVO> selectBrandRanking(
            @Param("statDate") LocalDate statDate,
            @Param("projectId") Long projectId,
            @Param("permIds") List<Long> permIds);
}
