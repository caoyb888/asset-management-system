package com.asset.report.mapper.rpt;

import com.asset.report.entity.RptOperationMonthly;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 营运月汇总表 Mapper
 */
@Mapper
public interface RptOperationMonthlyMapper extends BaseMapper<RptOperationMonthly> {

    int upsertBatch(@Param("list") List<RptOperationMonthly> list);

    int deleteByStatMonth(@Param("statMonth") String statMonth);
}
