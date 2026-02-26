package com.asset.operation.change.controller;

import com.asset.common.model.R;
import com.asset.operation.change.service.OprContractChangeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 合同变更 Controller
 * 阶段二（第3周）实现：变更CRUD/影响预览/审批提交/历史查询
 */
@Tag(name = "02-合同变更管理")
@RestController
@RequestMapping("/opr/contract-changes")
@RequiredArgsConstructor
public class OprContractChangeController {

    private final OprContractChangeService changeService;

    @Operation(summary = "变更单分页列表")
    @GetMapping
    public R<?> page() {
        // TODO 阶段二实现
        return R.ok();
    }

    @Operation(summary = "变更单详情（含明细/快照）")
    @GetMapping("/{id}")
    public R<?> getById(@PathVariable Long id) {
        // TODO 阶段二实现
        return R.ok();
    }

    @Operation(summary = "新增变更单")
    @PostMapping
    public R<?> create(@RequestBody Object dto) {
        // TODO 阶段二实现
        return R.ok();
    }

    @Operation(summary = "编辑变更单（仅草稿/驳回可改）")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @RequestBody Object dto) {
        // TODO 阶段二实现
        return R.ok();
    }

    @Operation(summary = "预览变更影响（应收笔数/金额差异）")
    @PostMapping("/{id}/preview-impact")
    public R<?> previewImpact(@PathVariable Long id) {
        // TODO 阶段二实现
        return R.ok();
    }

    @Operation(summary = "提交OA审批")
    @PostMapping("/{id}/submit-approval")
    public R<?> submitApproval(@PathVariable Long id) {
        // TODO 阶段二实现
        return R.ok();
    }

    @Operation(summary = "审批回调（应收重算引擎触发点）")
    @PostMapping("/{id}/approval-callback")
    public R<?> approvalCallback(@PathVariable Long id, @RequestBody Object dto) {
        // TODO 阶段二实现
        return R.ok();
    }

    @Operation(summary = "合同变更历史时间线")
    @GetMapping("/history/{contractId}")
    public R<?> history(@PathVariable Long contractId) {
        // TODO 阶段二实现
        return R.ok();
    }
}
