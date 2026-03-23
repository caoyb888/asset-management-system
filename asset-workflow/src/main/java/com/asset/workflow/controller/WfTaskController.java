package com.asset.workflow.controller;

import com.asset.api.workflow.dto.TaskPageQuery;
import com.asset.common.model.R;
import com.asset.workflow.service.WorkflowApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 待办任务接口 — /wf/tasks
 */
@Tag(name = "02-待办任务")
@RestController
@RequestMapping("/wf/tasks")
@RequiredArgsConstructor
public class WfTaskController {

    private final WorkflowApprovalService approvalService;

    @Operation(summary = "WT-01 我的待办（分页）")
    @GetMapping("/todo")
    public R<?> todoPage(TaskPageQuery query, @RequestParam Long userId) {
        return R.ok(approvalService.todoPage(query, userId));
    }

    @Operation(summary = "WT-02 我的已办（分页）")
    @GetMapping("/done")
    public R<?> donePage(TaskPageQuery query, @RequestParam Long userId) {
        return R.ok(approvalService.donePage(query, userId));
    }

    @Operation(summary = "WT-03 我发起的（分页）")
    @GetMapping("/initiated")
    public R<?> initiatedPage(TaskPageQuery query, @RequestParam Long userId) {
        return R.ok(approvalService.initiatedPage(query, userId));
    }

    @Operation(summary = "WT-04 待办数量统计（Badge）")
    @GetMapping("/count")
    public R<?> count(@RequestParam Long userId) {
        return R.ok(approvalService.todoCount(userId));
    }
}
