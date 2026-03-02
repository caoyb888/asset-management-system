package com.asset.report.mapper.rpt;

import com.asset.report.entity.RptAssetDaily;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 资产日汇总表 Mapper
 */
@Mapper
public interface RptAssetDailyMapper extends BaseMapper<RptAssetDaily> {

    /**
     * 批量幂等写入（ON DUPLICATE KEY UPDATE）
     */
    int upsertBatch(@Param("list") List<RptAssetDaily> list);

    /**
     * 删除指定日期的旧数据（重跑前清理）
     */
    int deleteByStatDate(@Param("statDate") LocalDate statDate);
}
