package com.asset.report.drill;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 数据钻取专用 Mapper
 * <p>
 * 所有 SQL 定义在 mapper/rpt/DrillDownMapper.xml 中。
 * 返回 {@code List<Map>} 以适配多层级的动态列结构。
 * </p>
 */
@Mapper
public interface DrillDownMapper {

    // ─── 资产域 ──────────────────────────────────────────────────────────────

    /** 查询项目下所有楼栋（L1 → L2） */
    List<Map<String, Object>> selectBuildingsByProject(
            @Param("projectId") Long projectId,
            @Param("statDate") String statDate);

    /** 查询楼栋下所有楼层（L2 → L3） */
    List<Map<String, Object>> selectFloorsByBuilding(
            @Param("buildingId") Long buildingId,
            @Param("statDate") String statDate);

    /** 查询楼层内所有商铺（L3 → L4，实时 biz_shop） */
    List<Map<String, Object>> selectShopsByFloor(
            @Param("floorId") Long floorId);

    // ─── 财务域 ──────────────────────────────────────────────────────────────

    /** 查询项目下各费项汇总（L1 → L2） */
    List<Map<String, Object>> selectFeeItemsByProject(
            @Param("projectId") Long projectId,
            @Param("startMonth") String startMonth,
            @Param("endMonth") String endMonth);

    /** 查询费项下应收明细（L2 → L3，实时 fin_receivable） */
    List<Map<String, Object>> selectReceivablesByFeeItem(
            @Param("projectId") Long projectId,
            @Param("feeItemType") String feeItemType,
            @Param("startMonth") String startMonth,
            @Param("endMonth") String endMonth);
}
