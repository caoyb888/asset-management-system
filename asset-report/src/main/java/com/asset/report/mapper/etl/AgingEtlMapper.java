package com.asset.report.mapper.etl;

import com.asset.report.entity.RptAgingAnalysis;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 账龄ETL数据源 Mapper
 * 从 fin_receivable 按商家+合同维度预计算账龄分档
 */
@Mapper
public interface AgingEtlMapper {

    /**
     * 按 项目/商家/合同/费项 聚合账龄分析
     * 账龄 = statDate - due_date（超期天数）
     * 分档：≤30 / 31-60 / 61-90 / 91-180 / 181-365 / >365
     * @param statDate 统计日期
     */
    List<RptAgingAnalysis> aggregateAgingAnalysis(@Param("statDate") LocalDate statDate);
}
