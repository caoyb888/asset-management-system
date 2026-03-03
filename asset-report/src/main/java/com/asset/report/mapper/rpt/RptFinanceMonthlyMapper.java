package com.asset.report.mapper.rpt;

import com.asset.report.entity.RptFinanceMonthly;
import com.asset.report.vo.fin.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 财务月汇总表 Mapper
 */
@Mapper
public interface RptFinanceMonthlyMapper extends BaseMapper<RptFinanceMonthly> {

    // ==================== ETL 专用 ====================

    int upsertBatch(@Param("list") List<RptFinanceMonthly> list);

    int deleteByStatMonth(@Param("statMonth") String statMonth);

    // ==================== 报表查询 ====================

    /** 查询最新统计月份（项目汇总行：fee_item_id=0） */
    String selectMaxStatMonth(@Param("projectId") Long projectId,
                              @Param("permIds") List<Long> permIds);

    /** 查询指定月份的项目级汇总行 */
    List<RptFinanceMonthly> selectProjectSummaryByMonth(@Param("statMonth") String statMonth,
                                                        @Param("projectId") Long projectId,
                                                        @Param("permIds") List<Long> permIds);

    /** 财务趋势（近N月应收/已收/欠款/收缴率，项目级汇总行） */
    List<FinTrendVO> selectFinTrend(@Param("projectId") Long projectId,
                                   @Param("startMonth") String startMonth,
                                   @Param("endMonth") String endMonth,
                                   @Param("permIds") List<Long> permIds);

    /** 应收汇总报表（支持按费项类型细分） */
    List<FinReceivableSummaryVO> selectReceivableSummary(@Param("projectId") Long projectId,
                                                        @Param("feeItemType") String feeItemType,
                                                        @Param("startMonth") String startMonth,
                                                        @Param("endMonth") String endMonth,
                                                        @Param("permIds") List<Long> permIds);

    /** 收款汇总报表（支持按费项类型细分） */
    List<FinReceiptSummaryVO> selectReceiptSummary(@Param("projectId") Long projectId,
                                                  @Param("feeItemType") String feeItemType,
                                                  @Param("startMonth") String startMonth,
                                                  @Param("endMonth") String endMonth,
                                                  @Param("permIds") List<Long> permIds);

    /** 欠款统计报表 */
    List<FinOutstandingSummaryVO> selectOutstandingSummary(@Param("projectId") Long projectId,
                                                          @Param("feeItemType") String feeItemType,
                                                          @Param("startMonth") String startMonth,
                                                          @Param("endMonth") String endMonth,
                                                          @Param("permIds") List<Long> permIds);

    /** 逾期率统计（项目级汇总行） */
    List<FinOverdueRateVO> selectOverdueRate(@Param("projectId") Long projectId,
                                            @Param("startMonth") String startMonth,
                                            @Param("endMonth") String endMonth,
                                            @Param("permIds") List<Long> permIds);

    /** 收缴率统计（支持按费项类型细分） */
    List<FinCollectionRateVO> selectCollectionRate(@Param("projectId") Long projectId,
                                                  @Param("feeItemType") String feeItemType,
                                                  @Param("startMonth") String startMonth,
                                                  @Param("endMonth") String endMonth,
                                                  @Param("permIds") List<Long> permIds);

    /** 保证金余额趋势（P1） */
    List<FinDepositSummaryVO> selectDepositSummary(@Param("projectId") Long projectId,
                                                  @Param("startMonth") String startMonth,
                                                  @Param("endMonth") String endMonth,
                                                  @Param("permIds") List<Long> permIds);

    /** 预收款余额趋势（P1） */
    List<FinPrepaySummaryVO> selectPrepaySummary(@Param("projectId") Long projectId,
                                                @Param("startMonth") String startMonth,
                                                @Param("endMonth") String endMonth,
                                                @Param("permIds") List<Long> permIds);

    /** 减免/调整统计（P1，支持按费项类型细分） */
    List<FinDeductionAdjustmentVO> selectDeductionAdjustment(@Param("projectId") Long projectId,
                                                             @Param("feeItemType") String feeItemType,
                                                             @Param("startMonth") String startMonth,
                                                             @Param("endMonth") String endMonth,
                                                             @Param("permIds") List<Long> permIds);

    /** 凭证统计（P1，直接查询 fin_voucher 业务表） */
    List<FinVoucherStatsVO> selectVoucherStats(@Param("projectId") Long projectId,
                                              @Param("startMonth") String startMonth,
                                              @Param("endMonth") String endMonth,
                                              @Param("permIds") List<Long> permIds);
}
