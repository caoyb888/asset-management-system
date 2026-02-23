package com.asset.investment.decomposition.controller;

import com.asset.common.model.R;
import com.asset.investment.decomposition.entity.InvRentDecomposition;
import com.asset.investment.decomposition.service.InvRentDecompositionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 租金分解 Controller */
@Tag(name = "租金分解管理", description = "租金分解CRUD与导入导出")
@RestController
@RequestMapping("/inv/rent-decomps")
@RequiredArgsConstructor
public class InvRentDecompositionController {

    private final InvRentDecompositionService decompositionService;

    @Operation(summary = "分页查询租金分解列表")
    @GetMapping
    public R<IPage<InvRentDecomposition>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<InvRentDecomposition> wrapper = new LambdaQueryWrapper<InvRentDecomposition>()
                .eq(projectId != null, InvRentDecomposition::getProjectId, projectId)
                .eq(status != null, InvRentDecomposition::getStatus, status)
                .orderByDesc(InvRentDecomposition::getCreatedAt);
        return R.ok(decompositionService.page(new Page<>(pageNum, pageSize), wrapper));
    }

    @Operation(summary = "查询详情")
    @GetMapping("/{id}")
    public R<InvRentDecomposition> detail(@PathVariable Long id) {
        return R.ok(decompositionService.getById(id));
    }

    @Operation(summary = "新增租金分解")
    @PostMapping
    public R<Long> create(@RequestBody InvRentDecomposition entity) {
        entity.setStatus(0);
        decompositionService.save(entity);
        return R.ok(entity.getId());
    }

    @Operation(summary = "编辑租金分解")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody InvRentDecomposition entity) {
        entity.setId(id);
        decompositionService.updateById(entity);
        return R.ok(null);
    }

    @Operation(summary = "删除租金分解")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        decompositionService.removeById(id);
        return R.ok(null);
    }
}
