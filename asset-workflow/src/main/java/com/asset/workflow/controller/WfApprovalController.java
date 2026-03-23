package com.asset.workflow.controller;

import com.asset.api.workflow.dto.ApprovalActionDTO;
import com.asset.api.workflow.dto.ApprovalSubmitDTO;
import com.asset.common.model.R;
import com.asset.workflow.service.WorkflowApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 审批操作接口 — /wf/approvals
 */
@Tag(name = "01-审批操作")
@RestController
@RequestMapping("/wf/approvals")
@RequiredArgsConstructor
public class WfApprovalController {

    private final WorkflowApprovalService approvalService;

    @Operation(summary = "WA-01 发起审批")
    @PostMapping("/submit")
    public R<?> submit(@Valid @RequestBody ApprovalSubmitDTO dto) {
        return R.ok(approvalService.submit(dto));
    }

    @Operation(summary = "WA-02 通过审批")
    @PostMapping("/{id}/approve")
    public R<?> approve(@PathVariable Long id, @RequestBody ApprovalActionDTO dto) {
        approvalService.approve(id, dto.getComment(), dto.getTaskId(), null);
        return R.ok();
    }

    @Operation(summary = "WA-03 驳回审批")
    @PostMapping("/{id}/reject")
    public R<?> reject(@PathVariable Long id, @RequestBody ApprovalActionDTO dto) {
        approvalService.reject(id, dto.getComment(), dto.getTaskId(), null);
        return R.ok();
    }

    @Operation(summary = "WA-04 撤回审批")
    @PostMapping("/{id}/revoke")
    public R<?> revoke(@PathVariable Long id) {
        approvalService.revoke(id);
        return R.ok();
    }

    @Operation(summary = "WA-05 转办")
    @PostMapping("/{id}/reassign")
    public R<?> reassign(@PathVariable Long id, @RequestBody ApprovalActionDTO dto) {
        approvalService.reassign(id, dto.getReassignUserId(), dto.getComment(), dto.getTaskId(), null);
        return R.ok();
    }

    @Operation(summary = "WA-06 查询流程详情")
    @GetMapping("/{id}")
    public R<?> detail(@PathVariable Long id) {
        return R.ok(approvalService.getDetail(id));
    }

    @Operation(summary = "WA-07 查询审批记录(timeline)")
    @GetMapping("/{id}/records")
    public R<?> records(@PathVariable Long id) {
        return R.ok(approvalService.getRecords(id));
    }

    @Operation(summary = "WA-08 按业务单据查流程")
    @GetMapping("/by-business")
    public R<?> byBusiness(@RequestParam String businessType, @RequestParam Long businessId) {
        return R.ok(approvalService.getByBusiness(businessType, businessId));
    }

    @Operation(summary = "WA-09 催办")
    @PostMapping("/{id}/urge")
    public R<?> urge(@PathVariable Long id) {
        approvalService.urge(id);
        return R.ok();
    }
}
