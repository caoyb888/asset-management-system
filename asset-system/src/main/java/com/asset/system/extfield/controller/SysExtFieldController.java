package com.asset.system.extfield.controller;

import com.asset.common.model.R;
import com.asset.system.extfield.dto.ExtFieldCreateDTO;
import com.asset.system.extfield.dto.ExtFieldSortItem;
import com.asset.system.extfield.service.SysExtFieldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 用户自定义扩展字段管理接口 */
@Tag(name = "12-扩展字段管理")
@RestController
@RequestMapping("/sys/ext-fields")
@RequiredArgsConstructor
public class SysExtFieldController {

    private final SysExtFieldService extFieldService;

    @Operation(summary = "查询模块扩展字段列表（按 sort_order 升序）")
    @GetMapping
    public R<?> list(@RequestParam String moduleCode) {
        return R.ok(extFieldService.listByModule(moduleCode));
    }

    @Operation(summary = "查询单个扩展字段详情")
    @GetMapping("/{id}")
    public R<?> getById(@PathVariable Long id) {
        return R.ok(extFieldService.getById(id));
    }

    @Operation(summary = "新增扩展字段定义")
    @PostMapping
    public R<?> create(@Valid @RequestBody ExtFieldCreateDTO dto) {
        return R.ok(extFieldService.create(dto));
    }

    @Operation(summary = "修改扩展字段定义（fieldKey 不可修改）")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody ExtFieldCreateDTO dto) {
        dto.setId(id);
        extFieldService.update(dto);
        return R.ok();
    }

    @Operation(summary = "删除扩展字段定义（逻辑删除，历史数据不受影响）")
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        extFieldService.delete(id);
        return R.ok();
    }

    @Operation(summary = "批量更新排序（拖拽排序后调用）")
    @PutMapping("/sort")
    public R<?> updateSort(@RequestBody List<ExtFieldSortItem> items) {
        extFieldService.updateSort(items);
        return R.ok();
    }
}
