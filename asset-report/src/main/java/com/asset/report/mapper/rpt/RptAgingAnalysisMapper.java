package com.asset.report.mapper.rpt;

import com.asset.report.entity.RptAgingAnalysis;
import com.asset.report.vo.fin.FinAgingAnalysisVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 账龄分析表 Mapper
 */
@Mapper
public interface RptAgingAnalysisMapper extends BaseMapper<RptAgingAnalysis> {

    // ==================== ETL 专用 ====================

    int upsertBatch(@Param("list") List<RptAgingAnalysis> list);

    int deleteByStatDate(@Param("statDate") LocalDate statDate);

    // ==================== 报表查询 ====================

    /** 查询最新统计日期（项目汇总行：merchant_id=0, contract_id=0, fee_item_id=0） */
    LocalDate selectMaxStatDate(@Param("projectId") Long projectId,
                                @Param("permIds") List<Long> permIds);

    /**
     * 查询指定日期的账龄分析（按商家维度）
     * <p>
     * statDate=null 时取最新日期；merchantId=null 时返回所有商家；
     * fee_item_id=0 对应该商家所有费项的汇总行。
     * </p>
     */
    List<FinAgingAnalysisVO> selectAgingByMerchant(@Param("statDate") LocalDate statDate,
                                                   @Param("projectId") Long projectId,
                                                   @Param("merchantId") Long merchantId,
                                                   @Param("permIds") List<Long> permIds);

    /**
     * 查询指定日期的账龄汇总（按项目维度，merchant_id=0 的汇总行）
     */
    List<FinAgingAnalysisVO> selectAgingProjectSummary(@Param("statDate") LocalDate statDate,
                                                       @Param("projectId") Long projectId,
                                                       @Param("permIds") List<Long> permIds);

    /**
     * 查询欠款 TOP N 商家
     * <p>
     * 按 total_outstanding 降序，merchant_id != 0（排除汇总行）。
     * </p>
     */
    List<FinAgingAnalysisVO> selectOverdueTopN(@Param("statDate") LocalDate statDate,
                                               @Param("projectId") Long projectId,
                                               @Param("topN") int topN,
                                               @Param("permIds") List<Long> permIds);
}
