package com.asset.system.category.controller;

import com.asset.common.model.R;
import com.asset.system.category.dto.CategoryCreateDTO;
import com.asset.system.category.dto.CategoryQueryDTO;
import com.asset.system.category.service.SysCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 系统分类管理接口 */
@Tag(name = "09-分类管理")
@RestController
@RequestMapping("/sys/categories")
@RequiredArgsConstructor
public class SysCategoryController {

    private final SysCategoryService categoryService;

    @Operation(summary = "查询分类树（按维度）")
    @GetMapping("/tree")
    public R<?> tree(CategoryQueryDTO query) {
        return R.ok(categoryService.treeQuery(query));
    }

    @Operation(summary = "获取所有分类维度列表")
    @GetMapping("/types")
    public R<?> listTypes() {
        return R.ok(categoryService.listCategoryTypes());
    }

    @Operation(summary = "新增分类节点")
    @PostMapping
    public R<?> create(@Valid @RequestBody CategoryCreateDTO dto) {
        return R.ok(categoryService.createCategory(dto));
    }

    @Operation(summary = "更新分类节点")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody CategoryCreateDTO dto) {
        dto.setId(id);
        categoryService.updateCategory(dto);
        return R.ok();
    }

    @Operation(summary = "删除分类节点（叶子节点）")
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return R.ok();
    }

    @Operation(summary = "修改分类状态")
    @PutMapping("/{id}/status")
    public R<?> changeStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        categoryService.changeStatus(id, body.get("status"));
        return R.ok();
    }
}
