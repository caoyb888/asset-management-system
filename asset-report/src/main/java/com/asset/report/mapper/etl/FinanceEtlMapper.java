package com.asset.report.mapper.etl;

import com.asset.report.entity.RptFinanceMonthly;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 财务ETL数据源 Mapper
 * 从 fin_receivable + fin_receipt + fin_deposit_account + fin_prepay_account 聚合财务指标
 */
@Mapper
public interface FinanceEtlMapper {

    /**
     * 按 项目/费项 聚合财务月指标（应收/已收/欠款/逾期）
     * @param statMonth 统计月份 YYYY-MM
     */
    List<RptFinanceMonthly> aggregateFinanceMonthly(@Param("statMonth") String statMonth);

    /**
     * 查询各项目保证金余额快照
     * @param statMonth 统计月份 YYYY-MM
     */
    List<RptFinanceMonthly> aggregateDepositBalance(@Param("statMonth") String statMonth);

    /**
     * 查询各项目预收款余额快照
     * @param statMonth 统计月份 YYYY-MM
     */
    List<RptFinanceMonthly> aggregatePrepayBalance(@Param("statMonth") String statMonth);
}
