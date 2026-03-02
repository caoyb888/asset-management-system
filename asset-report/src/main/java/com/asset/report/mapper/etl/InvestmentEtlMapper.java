package com.asset.report.mapper.etl;

import com.asset.report.entity.RptInvestmentDaily;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 招商ETL数据源 Mapper
 * 从 inv_intention + inv_lease_contract 聚合招商指标
 */
@Mapper
public interface InvestmentEtlMapper {

    /**
     * 按 项目/业态 聚合招商日指标
     * 包含：意向数/合同数/转化率/平均租金
     */
    List<RptInvestmentDaily> aggregateInvestmentDaily(@Param("statDate") LocalDate statDate);
}
