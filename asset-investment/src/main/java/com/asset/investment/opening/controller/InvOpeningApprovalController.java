package com.asset.investment.opening.controller;

import com.asset.common.model.R;
import com.asset.investment.opening.entity.InvOpeningApproval;
import com.asset.investment.opening.service.InvOpeningApprovalService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 开业审批 Controller */
@Tag(name = "开业审批管理", description = "招商-开业审批CRUD与审批流程")
@RestController
@RequestMapping("/inv/opening-approvals")
@RequiredArgsConstructor
public class InvOpeningApprovalController {

    private final InvOpeningApprovalService approvalService;

    @Operation(summary = "分页查询开业审批列表")
    @GetMapping
    public R<IPage<InvOpeningApproval>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<InvOpeningApproval> wrapper = new LambdaQueryWrapper<InvOpeningApproval>()
                .eq(projectId != null, InvOpeningApproval::getProjectId, projectId)
                .eq(status != null, InvOpeningApproval::getStatus, status)
                .orderByDesc(InvOpeningApproval::getCreatedAt);
        return R.ok(approvalService.page(new Page<>(pageNum, pageSize), wrapper));
    }

    @Operation(summary = "查询详情")
    @GetMapping("/{id}")
    public R<InvOpeningApproval> detail(@PathVariable Long id) {
        return R.ok(approvalService.getById(id));
    }

    @Operation(summary = "新增开业审批")
    @PostMapping
    public R<Long> create(@RequestBody InvOpeningApproval entity) {
        entity.setStatus(0);
        approvalService.save(entity);
        return R.ok(entity.getId());
    }

    @Operation(summary = "编辑开业审批")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody InvOpeningApproval entity) {
        entity.setId(id);
        approvalService.updateById(entity);
        return R.ok(null);
    }

    @Operation(summary = "删除开业审批")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        approvalService.removeById(id);
        return R.ok(null);
    }
}
