package com.asset.report.drill;

import com.asset.report.drill.dto.DrillDownRequestDTO;
import com.asset.report.drill.vo.DrillColumnVO;
import com.asset.report.drill.vo.DrillDownResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 数据钻取 Service 实现
 * <p>
 * 支持两个钻取域：
 * <ul>
 *   <li>资产域（ASSET_* 报表）：项目(1) → 楼栋(2) → 楼层(3) → 商铺(4)</li>
 *   <li>财务域（FIN_* 报表）：项目(1) → 费项(2) → 应收明细(3)</li>
 * </ul>
 * 其余域(OPR/INV)暂时路由至资产域(项目/楼栋两层)。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DrillDownServiceImpl implements DrillDownService {

    private final DrillDownMapper drillMapper;

    @Override
    public DrillDownResultVO drillDown(DrillDownRequestDTO dto) {
        String code = dto.getReportCode();
        if (code == null) throw new IllegalArgumentException("reportCode 不能为空");

        if (code.startsWith("FIN_")) {
            return drillFinance(dto);
        } else {
            // ASSET_* / OPR_* / INV_* 均走资产域路径
            return drillAsset(dto);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 资产域：项目 → 楼栋 → 楼层 → 商铺
    // ══════════════════════════════════════════════════════════════════════════

    private DrillDownResultVO drillAsset(DrillDownRequestDTO dto) {
        int fromLevel = dto.getFromLevel() == null ? 0 : dto.getFromLevel();
        Long dimId = dto.getDimensionId();
        String statDate = dto.getStatDate();

        return switch (fromLevel) {
            case 0, 1 -> {
                // 项目 → 楼栋
                List<Map<String, Object>> rows = drillMapper.selectBuildingsByProject(dimId, statDate);
                yield DrillDownResultVO.builder()
                        .currentLevel(2)
                        .levelName("楼栋")
                        .nextLevelName("楼层")
                        .canDrillDown(true)
                        .parentId(dimId)
                        .parentName("项目 #" + dimId)
                        .columns(assetBuildingColumns())
                        .rows(rows)
                        .total(rows.size())
                        .build();
            }
            case 2 -> {
                // 楼栋 → 楼层
                List<Map<String, Object>> rows = drillMapper.selectFloorsByBuilding(dimId, statDate);
                yield DrillDownResultVO.builder()
                        .currentLevel(3)
                        .levelName("楼层")
                        .nextLevelName("商铺")
                        .canDrillDown(true)
                        .parentId(dimId)
                        .parentName("楼栋 #" + dimId)
                        .columns(assetFloorColumns())
                        .rows(rows)
                        .total(rows.size())
                        .build();
            }
            case 3 -> {
                // 楼层 → 商铺（实时 biz_shop）
                List<Map<String, Object>> rows = drillMapper.selectShopsByFloor(dimId);
                yield DrillDownResultVO.builder()
                        .currentLevel(4)
                        .levelName("商铺")
                        .nextLevelName(null)
                        .canDrillDown(false)
                        .parentId(dimId)
                        .parentName("楼层 #" + dimId)
                        .columns(assetShopColumns())
                        .rows(rows)
                        .total(rows.size())
                        .build();
            }
            default -> throw new IllegalArgumentException("资产域最多四层钻取，当前 fromLevel=" + fromLevel + " 已到叶子节点");
        };
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 财务域：项目 → 费项 → 应收明细
    // ══════════════════════════════════════════════════════════════════════════

    private DrillDownResultVO drillFinance(DrillDownRequestDTO dto) {
        int fromLevel = dto.getFromLevel() == null ? 0 : dto.getFromLevel();
        Long dimId = dto.getDimensionId();
        String startMonth = dto.getStartMonth();
        String endMonth = dto.getEndMonth();
        String feeItemType = dto.getFeeItemType();

        return switch (fromLevel) {
            case 0, 1 -> {
                // 项目 → 费项
                List<Map<String, Object>> rows = drillMapper.selectFeeItemsByProject(dimId, startMonth, endMonth);
                yield DrillDownResultVO.builder()
                        .currentLevel(2)
                        .levelName("费项")
                        .nextLevelName("应收明细")
                        .canDrillDown(true)
                        .parentId(dimId)
                        .parentName("项目 #" + dimId)
                        .columns(finFeeItemColumns())
                        .rows(rows)
                        .total(rows.size())
                        .build();
            }
            case 2 -> {
                // 费项 → 应收明细（实时 fin_receivable）
                List<Map<String, Object>> rows = drillMapper.selectReceivablesByFeeItem(
                        dimId, feeItemType, startMonth, endMonth);
                yield DrillDownResultVO.builder()
                        .currentLevel(3)
                        .levelName("应收明细")
                        .nextLevelName(null)
                        .canDrillDown(false)
                        .parentId(dimId)
                        .parentName("费项: " + (feeItemType != null ? feeItemType : "全部"))
                        .columns(finReceivableColumns())
                        .rows(rows)
                        .total(rows.size())
                        .build();
            }
            default -> throw new IllegalArgumentException("财务域最多三层钻取，当前 fromLevel=" + fromLevel + " 已到叶子节点");
        };
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 列定义（静态配置）
    // ══════════════════════════════════════════════════════════════════════════

    private static List<DrillColumnVO> assetBuildingColumns() {
        return List.of(
                DrillColumnVO.builder().prop("buildingId").label("楼栋ID").drillable(true).drillIdField("buildingId").width(90).build(),
                DrillColumnVO.builder().prop("statDate").label("统计日期").width(110).build(),
                DrillColumnVO.builder().prop("totalShops").label("商铺总数").align("right").width(90).build(),
                DrillColumnVO.builder().prop("rentedShops").label("已租").align("right").width(70).build(),
                DrillColumnVO.builder().prop("vacantShops").label("空置").align("right").width(70).build(),
                DrillColumnVO.builder().prop("openedShops").label("已开业").align("right").width(80).build(),
                DrillColumnVO.builder().prop("totalArea").label("总面积(㎡)").align("right").width(110).build(),
                DrillColumnVO.builder().prop("rentedArea").label("已租面积(㎡)").align("right").width(120).build(),
                DrillColumnVO.builder().prop("vacancyRate").label("空置率(%)").align("right").width(90).build(),
                DrillColumnVO.builder().prop("rentalRate").label("出租率(%)").align("right").width(90).build(),
                DrillColumnVO.builder().prop("openingRate").label("开业率(%)").align("right").width(90).build()
        );
    }

    private static List<DrillColumnVO> assetFloorColumns() {
        return List.of(
                DrillColumnVO.builder().prop("floorId").label("楼层ID").drillable(true).drillIdField("floorId").width(90).build(),
                DrillColumnVO.builder().prop("buildingId").label("楼栋ID").width(90).build(),
                DrillColumnVO.builder().prop("statDate").label("统计日期").width(110).build(),
                DrillColumnVO.builder().prop("totalShops").label("商铺总数").align("right").width(90).build(),
                DrillColumnVO.builder().prop("rentedShops").label("已租").align("right").width(70).build(),
                DrillColumnVO.builder().prop("vacantShops").label("空置").align("right").width(70).build(),
                DrillColumnVO.builder().prop("totalArea").label("总面积(㎡)").align("right").width(110).build(),
                DrillColumnVO.builder().prop("vacancyRate").label("空置率(%)").align("right").width(90).build(),
                DrillColumnVO.builder().prop("rentalRate").label("出租率(%)").align("right").width(90).build(),
                DrillColumnVO.builder().prop("openingRate").label("开业率(%)").align("right").width(90).build()
        );
    }

    private static List<DrillColumnVO> assetShopColumns() {
        return List.of(
                DrillColumnVO.builder().prop("shopId").label("商铺ID").width(90).build(),
                DrillColumnVO.builder().prop("shopCode").label("商铺编码").width(120).build(),
                DrillColumnVO.builder().prop("shopType").label("商铺类型").width(100).build(),
                DrillColumnVO.builder().prop("planFormat").label("规划业态").width(100).build(),
                DrillColumnVO.builder().prop("rentArea").label("租赁面积(㎡)").align("right").width(120).build(),
                DrillColumnVO.builder().prop("actualArea").label("实际面积(㎡)").align("right").width(120).build(),
                DrillColumnVO.builder().prop("shopStatus").label("状态").align("center").width(90).build()
        );
    }

    private static List<DrillColumnVO> finFeeItemColumns() {
        return List.of(
                DrillColumnVO.builder().prop("feeItemType").label("费项类型").drillable(true).drillIdField("projectId").width(120).build(),
                DrillColumnVO.builder().prop("accrualMonth").label("账期月份").width(100).build(),
                DrillColumnVO.builder().prop("receivableAmount").label("应收金额(元)").align("right").width(130).build(),
                DrillColumnVO.builder().prop("receivedAmount").label("已收金额(元)").align("right").width(130).build(),
                DrillColumnVO.builder().prop("outstandingAmount").label("欠款金额(元)").align("right").width(130).build(),
                DrillColumnVO.builder().prop("overdueAmount").label("逾期金额(元)").align("right").width(130).build(),
                DrillColumnVO.builder().prop("collectionRate").label("收缴率(%)").align("right").width(100).build()
        );
    }

    private static List<DrillColumnVO> finReceivableColumns() {
        return List.of(
                DrillColumnVO.builder().prop("receivableId").label("应收ID").width(90).build(),
                DrillColumnVO.builder().prop("contractId").label("合同ID").width(90).build(),
                DrillColumnVO.builder().prop("merchantId").label("商家ID").width(90).build(),
                DrillColumnVO.builder().prop("accrualMonth").label("账期月份").width(100).build(),
                DrillColumnVO.builder().prop("billingStart").label("计费开始").width(110).build(),
                DrillColumnVO.builder().prop("billingEnd").label("计费结束").width(110).build(),
                DrillColumnVO.builder().prop("originalAmount").label("原始金额(元)").align("right").width(130).build(),
                DrillColumnVO.builder().prop("actualAmount").label("实际金额(元)").align("right").width(130).build(),
                DrillColumnVO.builder().prop("receivedAmount").label("已收(元)").align("right").width(110).build(),
                DrillColumnVO.builder().prop("outstandingAmount").label("欠款(元)").align("right").width(110).build(),
                DrillColumnVO.builder().prop("status").label("状态").align("center").width(80).build()
        );
    }
}
