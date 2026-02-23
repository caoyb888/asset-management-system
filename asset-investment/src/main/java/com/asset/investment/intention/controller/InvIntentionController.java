package com.asset.investment.intention.controller;

import com.asset.common.model.R;
import com.asset.investment.intention.entity.InvIntention;
import com.asset.investment.intention.service.InvIntentionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 意向协议 Controller（骨架，业务逻辑待第四阶段完善）
 */
@Tag(name = "意向协议管理", description = "招商-意向协议CRUD与审批流程")
@RestController
@RequestMapping("/inv/intentions")
@RequiredArgsConstructor
public class InvIntentionController {

    private final InvIntentionService intentionService;

    @Operation(summary = "分页查询意向协议列表")
    @GetMapping
    public R<IPage<InvIntention>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<InvIntention> wrapper = new LambdaQueryWrapper<InvIntention>()
                .eq(projectId != null, InvIntention::getProjectId, projectId)
                .eq(status != null, InvIntention::getStatus, status)
                .eq(InvIntention::getIsCurrent, 1)
                .orderByDesc(InvIntention::getCreatedAt);
        return R.ok(intentionService.page(new Page<>(pageNum, pageSize), wrapper));
    }

    @Operation(summary = "查询意向协议详情")
    @GetMapping("/{id}")
    public R<InvIntention> detail(@PathVariable Long id) {
        return R.ok(intentionService.getById(id));
    }

    @Operation(summary = "新增意向协议（草稿）")
    @PostMapping
    public R<Long> create(@RequestBody InvIntention entity) {
        entity.setStatus(0);
        entity.setVersion(1);
        entity.setIsCurrent(1);
        intentionService.save(entity);
        return R.ok(entity.getId());
    }

    @Operation(summary = "编辑意向协议")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody InvIntention entity) {
        entity.setId(id);
        intentionService.updateById(entity);
        return R.ok(null);
    }

    @Operation(summary = "删除意向协议")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        intentionService.removeById(id);
        return R.ok(null);
    }
}
