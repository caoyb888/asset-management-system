package com.asset.report.mapper.rpt;

import com.asset.report.entity.RptInvestmentDaily;
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

    int upsertBatch(@Param("list") List<RptInvestmentDaily> list);

    int deleteByStatDate(@Param("statDate") LocalDate statDate);
}
