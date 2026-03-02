package com.asset.report.mapper.rpt;

import com.asset.report.entity.RptAgingAnalysis;
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

    int upsertBatch(@Param("list") List<RptAgingAnalysis> list);

    int deleteByStatDate(@Param("statDate") LocalDate statDate);
}
