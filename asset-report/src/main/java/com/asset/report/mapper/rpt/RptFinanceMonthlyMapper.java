package com.asset.report.mapper.rpt;

import com.asset.report.entity.RptFinanceMonthly;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 财务月汇总表 Mapper
 */
@Mapper
public interface RptFinanceMonthlyMapper extends BaseMapper<RptFinanceMonthly> {

    int upsertBatch(@Param("list") List<RptFinanceMonthly> list);

    int deleteByStatMonth(@Param("statMonth") String statMonth);
}
