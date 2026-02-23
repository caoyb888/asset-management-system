package com.asset.investment.contract.controller;

import com.asset.common.model.R;
import com.asset.investment.contract.entity.InvLeaseContract;
import com.asset.investment.contract.service.InvLeaseContractService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 招商合同 Controller（骨架，业务逻辑待第五阶段完善） */
@Tag(name = "招商合同管理", description = "招商-合同CRUD与状态管理")
@RestController
@RequestMapping("/inv/contracts")
@RequiredArgsConstructor
public class InvLeaseContractController {

    private final InvLeaseContractService contractService;

    @Operation(summary = "分页查询合同列表")
    @GetMapping
    public R<IPage<InvLeaseContract>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<InvLeaseContract> wrapper = new LambdaQueryWrapper<InvLeaseContract>()
                .eq(projectId != null, InvLeaseContract::getProjectId, projectId)
                .eq(status != null, InvLeaseContract::getStatus, status)
                .eq(InvLeaseContract::getIsCurrent, 1)
                .orderByDesc(InvLeaseContract::getCreatedAt);
        return R.ok(contractService.page(new Page<>(pageNum, pageSize), wrapper));
    }

    @Operation(summary = "查询合同详情")
    @GetMapping("/{id}")
    public R<InvLeaseContract> detail(@PathVariable Long id) {
        return R.ok(contractService.getById(id));
    }

    @Operation(summary = "新增合同（草稿）")
    @PostMapping
    public R<Long> create(@RequestBody InvLeaseContract entity) {
        entity.setStatus(0);
        entity.setVersion(1);
        entity.setIsCurrent(1);
        contractService.save(entity);
        return R.ok(entity.getId());
    }

    @Operation(summary = "编辑合同")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody InvLeaseContract entity) {
        entity.setId(id);
        contractService.updateById(entity);
        return R.ok(null);
    }

    @Operation(summary = "删除合同")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        contractService.removeById(id);
        return R.ok(null);
    }
}
