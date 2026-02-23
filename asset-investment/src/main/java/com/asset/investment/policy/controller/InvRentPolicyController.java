package com.asset.investment.policy.controller;

import com.asset.common.model.R;
import com.asset.investment.policy.entity.InvRentPolicy;
import com.asset.investment.policy.service.InvRentPolicyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 租决政策 Controller */
@Tag(name = "租决政策管理", description = "租决政策CRUD与审批")
@RestController
@RequestMapping("/inv/rent-policies")
@RequiredArgsConstructor
public class InvRentPolicyController {

    private final InvRentPolicyService policyService;

    @Operation(summary = "分页查询租决政策列表")
    @GetMapping
    public R<IPage<InvRentPolicy>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<InvRentPolicy> wrapper = new LambdaQueryWrapper<InvRentPolicy>()
                .eq(projectId != null, InvRentPolicy::getProjectId, projectId)
                .eq(status != null, InvRentPolicy::getStatus, status)
                .orderByDesc(InvRentPolicy::getCreatedAt);
        return R.ok(policyService.page(new Page<>(pageNum, pageSize), wrapper));
    }

    @Operation(summary = "查询详情")
    @GetMapping("/{id}")
    public R<InvRentPolicy> detail(@PathVariable Long id) {
        return R.ok(policyService.getById(id));
    }

    @Operation(summary = "新增租决政策")
    @PostMapping
    public R<Long> create(@RequestBody InvRentPolicy entity) {
        entity.setStatus(0);
        policyService.save(entity);
        return R.ok(entity.getId());
    }

    @Operation(summary = "编辑租决政策")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody InvRentPolicy entity) {
        entity.setId(id);
        policyService.updateById(entity);
        return R.ok(null);
    }

    @Operation(summary = "删除租决政策")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        policyService.removeById(id);
        return R.ok(null);
    }
}
