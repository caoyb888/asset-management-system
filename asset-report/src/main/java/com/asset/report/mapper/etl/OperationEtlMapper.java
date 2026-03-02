package com.asset.report.mapper.etl;

import com.asset.report.entity.RptOperationMonthly;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 营运ETL数据源 Mapper
 * 从 opr_revenue_report + opr_contract_change + opr_passenger_flow 聚合营运指标
 */
@Mapper
public interface OperationEtlMapper {

    /**
     * 按 项目/楼栋/业态 聚合营运月指标
     * @param statMonth 统计月份 YYYY-MM
     */
    List<RptOperationMonthly> aggregateOperationMonthly(@Param("statMonth") String statMonth);
}
