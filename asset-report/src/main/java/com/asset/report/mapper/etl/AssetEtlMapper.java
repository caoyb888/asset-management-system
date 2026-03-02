package com.asset.report.mapper.etl;

import com.asset.report.entity.RptAssetDaily;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 资产ETL数据源 Mapper
 * 从 biz_shop + inv_lease_contract 聚合资产指标
 */
@Mapper
public interface AssetEtlMapper {

    /**
     * 按 项目/楼栋/楼层/业态 聚合资产日指标
     * 包含：商铺数量/面积/空置率/出租率/开业率
     */
    List<RptAssetDaily> aggregateAssetDaily(@Param("statDate") LocalDate statDate);
}
