package com.asset.base.excel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.asset.base.entity.BizBuilding;
import com.asset.base.entity.BizFloor;
import com.asset.base.entity.BizProject;
import com.asset.base.entity.BizShop;
import com.asset.base.mapper.BizBuildingMapper;
import com.asset.base.mapper.BizFloorMapper;
import com.asset.base.mapper.BizProjectMapper;
import com.asset.base.model.dto.ShopImportRow;
import com.asset.base.service.BizShopService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商铺 Excel 导入监听器
 */
@Slf4j
public class ShopImportListener extends AnalysisEventListener<ShopImportRow> {

    private final BizShopService shopService;
    private final BizProjectMapper projectMapper;
    private final BizBuildingMapper buildingMapper;
    private final BizFloorMapper floorMapper;

    private final List<String> errors = new ArrayList<>();
    private int successCount = 0;
    /** 数据行索引，从第2行开始（第1行是表头） */
    private int rowIndex = 1;

    public ShopImportListener(BizShopService shopService,
                              BizProjectMapper projectMapper,
                              BizBuildingMapper buildingMapper,
                              BizFloorMapper floorMapper) {
        this.shopService = shopService;
        this.projectMapper = projectMapper;
        this.buildingMapper = buildingMapper;
        this.floorMapper = floorMapper;
    }

    @Override
    public void invoke(ShopImportRow row, AnalysisContext context) {
        rowIndex++;
        try {
            // 必填校验
            if (!StringUtils.hasText(row.getProjectCode())) {
                errors.add("第" + rowIndex + "行：项目编码不能为空");
                return;
            }
            if (!StringUtils.hasText(row.getBuildingCode())) {
                errors.add("第" + rowIndex + "行：楼栋编码不能为空");
                return;
            }
            if (!StringUtils.hasText(row.getFloorCode())) {
                errors.add("第" + rowIndex + "行：楼层编码不能为空");
                return;
            }
            if (!StringUtils.hasText(row.getShopCode())) {
                errors.add("第" + rowIndex + "行：铺位号不能为空");
                return;
            }

            // 查找项目
            BizProject project = projectMapper.selectOne(
                    new LambdaQueryWrapper<BizProject>()
                            .eq(BizProject::getProjectCode, row.getProjectCode().trim())
                            .eq(BizProject::getIsDeleted, 0)
                            .last("LIMIT 1")
            );
            if (project == null) {
                errors.add("第" + rowIndex + "行：项目编码[" + row.getProjectCode() + "]不存在");
                return;
            }

            // 查找楼栋
            BizBuilding building = buildingMapper.selectOne(
                    new LambdaQueryWrapper<BizBuilding>()
                            .eq(BizBuilding::getBuildingCode, row.getBuildingCode().trim())
                            .eq(BizBuilding::getProjectId, project.getId())
                            .eq(BizBuilding::getIsDeleted, 0)
                            .last("LIMIT 1")
            );
            if (building == null) {
                errors.add("第" + rowIndex + "行：楼栋编码[" + row.getBuildingCode() + "]在该项目下不存在");
                return;
            }

            // 查找楼层
            BizFloor floor = floorMapper.selectOne(
                    new LambdaQueryWrapper<BizFloor>()
                            .eq(BizFloor::getFloorCode, row.getFloorCode().trim())
                            .eq(BizFloor::getBuildingId, building.getId())
                            .eq(BizFloor::getIsDeleted, 0)
                            .last("LIMIT 1")
            );
            if (floor == null) {
                errors.add("第" + rowIndex + "行：楼层编码[" + row.getFloorCode() + "]在该楼栋下不存在");
                return;
            }

            BizShop shop = new BizShop();
            shop.setProjectId(project.getId());
            shop.setBuildingId(building.getId());
            shop.setFloorId(floor.getId());
            shop.setShopCode(row.getShopCode().trim());
            shop.setShopType(row.getShopType());
            shop.setRentArea(row.getRentArea());
            shop.setMeasuredArea(row.getMeasuredArea());
            shop.setBuildingArea(row.getBuildingArea());
            shop.setOperatingArea(row.getOperatingArea());
            shop.setShopStatus(row.getShopStatus() != null ? row.getShopStatus() : 0);
            shop.setPlannedFormat(row.getPlannedFormat());
            shop.setSignedFormat(row.getSignedFormat());
            shop.setOwnerName(row.getOwnerName());
            shop.setOwnerContact(row.getOwnerContact());
            shop.setOwnerPhone(row.getOwnerPhone());
            // 默认计入统计率
            shop.setCountLeasingRate(1);
            shop.setCountRentalRate(1);
            shop.setCountOpeningRate(1);

            shopService.save(shop);
            successCount++;
        } catch (Exception e) {
            log.warn("商铺导入第{}行失败: {}", rowIndex, e.getMessage());
            errors.add("第" + rowIndex + "行：" + e.getMessage());
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        log.info("商铺导入完成，成功{}条，失败{}条", successCount, errors.size());
    }

    /**
     * 获取导入结果统计
     */
    public Map<String, Object> getResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", errors.size());
        result.put("errors", errors);
        return result;
    }
}
