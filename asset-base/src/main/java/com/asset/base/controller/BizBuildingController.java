package com.asset.base.controller;

import com.asset.base.model.dto.BuildingQuery;
import com.asset.base.model.dto.BuildingSaveDTO;
import com.asset.base.model.vo.BuildingVO;
import com.asset.base.service.BizBuildingService;
import com.asset.common.log.annotation.OperLog;
import com.asset.common.model.R;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 楼栋管理 Controller
 *
 * <pre>
 * GET    /api/base/buildings         分页查询（?projectId=必填）
 * POST   /api/base/buildings         新增楼栋
 * GET    /api/base/buildings/{id}    楼栋详情
 * PUT    /api/base/buildings/{id}    编辑楼栋
 * DELETE /api/base/buildings/{id}    逻辑删除
 * </pre>
 */
@Tag(name = "楼栋管理", description = "基础数据-楼栋增删改查")
@RestController
@RequestMapping("/base/buildings")
@RequiredArgsConstructor
public class BizBuildingController {

    private final BizBuildingService buildingService;

    @Operation(summary = "分页查询楼栋列表")
    @GetMapping
    @OperLog(module = "楼栋管理", action = "分页查询", type = OperLog.OperType.QUERY)
    public R<IPage<BuildingVO>> page(BuildingQuery query) {
        return R.ok(buildingService.pageBuilding(query));
    }

    @Operation(summary = "查询楼栋详情")
    @GetMapping("/{id}")
    @OperLog(module = "楼栋管理", action = "查询详情", type = OperLog.OperType.QUERY)
    public R<BuildingVO> detail(
            @Parameter(description = "楼栋ID") @PathVariable Long id) {
        return R.ok(buildingService.getBuildingById(id));
    }

    @Operation(summary = "新增楼栋")
    @PostMapping
    @OperLog(module = "楼栋管理", action = "新增", type = OperLog.OperType.CREATE)
    public R<Long> create(@Valid @RequestBody BuildingSaveDTO dto) {
        return R.ok(buildingService.createBuilding(dto));
    }

    @Operation(summary = "编辑楼栋")
    @PutMapping("/{id}")
    @OperLog(module = "楼栋管理", action = "编辑", type = OperLog.OperType.UPDATE)
    public R<Void> update(
            @Parameter(description = "楼栋ID") @PathVariable Long id,
            @Valid @RequestBody BuildingSaveDTO dto) {
        buildingService.updateBuilding(id, dto);
        return R.ok(null);
    }

    @Operation(summary = "删除楼栋（逻辑删除）")
    @DeleteMapping("/{id}")
    @OperLog(module = "楼栋管理", action = "删除", type = OperLog.OperType.DELETE)
    public R<Void> delete(
            @Parameter(description = "楼栋ID") @PathVariable Long id) {
        buildingService.deleteBuilding(id);
        return R.ok(null);
    }
}
