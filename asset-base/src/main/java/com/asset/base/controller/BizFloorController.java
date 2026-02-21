package com.asset.base.controller;

import com.asset.base.model.dto.FloorQuery;
import com.asset.base.model.dto.FloorSaveDTO;
import com.asset.base.model.vo.FloorVO;
import com.asset.base.service.BizFloorService;
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
 * 楼层管理 Controller
 *
 * <pre>
 * GET    /api/base/floors         分页查询（?buildingId=必填）
 * POST   /api/base/floors         新增楼层
 * GET    /api/base/floors/{id}    楼层详情
 * PUT    /api/base/floors/{id}    编辑楼层
 * DELETE /api/base/floors/{id}    逻辑删除
 * </pre>
 */
@Tag(name = "楼层管理", description = "基础数据-楼层增删改查")
@RestController
@RequestMapping("/base/floors")
@RequiredArgsConstructor
public class BizFloorController {

    private final BizFloorService floorService;

    @Operation(summary = "分页查询楼层列表")
    @GetMapping
    @OperLog(module = "楼层管理", action = "分页查询", type = OperLog.OperType.QUERY)
    public R<IPage<FloorVO>> page(FloorQuery query) {
        return R.ok(floorService.pageFloor(query));
    }

    @Operation(summary = "查询楼层详情")
    @GetMapping("/{id}")
    @OperLog(module = "楼层管理", action = "查询详情", type = OperLog.OperType.QUERY)
    public R<FloorVO> detail(
            @Parameter(description = "楼层ID") @PathVariable Long id) {
        return R.ok(floorService.getFloorById(id));
    }

    @Operation(summary = "新增楼层")
    @PostMapping
    @OperLog(module = "楼层管理", action = "新增", type = OperLog.OperType.CREATE)
    public R<Long> create(@Valid @RequestBody FloorSaveDTO dto) {
        return R.ok(floorService.createFloor(dto));
    }

    @Operation(summary = "编辑楼层")
    @PutMapping("/{id}")
    @OperLog(module = "楼层管理", action = "编辑", type = OperLog.OperType.UPDATE)
    public R<Void> update(
            @Parameter(description = "楼层ID") @PathVariable Long id,
            @Valid @RequestBody FloorSaveDTO dto) {
        floorService.updateFloor(id, dto);
        return R.ok(null);
    }

    @Operation(summary = "删除楼层（逻辑删除）")
    @DeleteMapping("/{id}")
    @OperLog(module = "楼层管理", action = "删除", type = OperLog.OperType.DELETE)
    public R<Void> delete(
            @Parameter(description = "楼层ID") @PathVariable Long id) {
        floorService.deleteFloor(id);
        return R.ok(null);
    }
}
