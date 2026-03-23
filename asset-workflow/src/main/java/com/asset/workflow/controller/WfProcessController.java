package com.asset.workflow.controller;

import com.asset.api.workflow.dto.ProcessPageQuery;
import com.asset.api.workflow.enums.ProcessStatus;
import com.asset.common.exception.BizException;
import com.asset.common.model.R;
import com.asset.workflow.entity.WfProcessInstance;
import com.asset.workflow.service.WfProcessInstanceService;
import com.asset.workflow.service.WorkflowApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RuntimeService;
import org.springframework.web.bind.annotation.*;

/**
 * 流程监控接口（管理员） — /wf/processes
 */
@Tag(name = "03-流程监控")
@RestController
@RequestMapping("/wf/processes")
@RequiredArgsConstructor
public class WfProcessController {

    private final WfProcessInstanceService instanceService;
    private final WorkflowApprovalService approvalService;
    private final RuntimeService runtimeService;

    @Operation(summary = "WP-01 流程实例分页查询")
    @GetMapping
    public R<?> page(ProcessPageQuery query) {
        return R.ok(instanceService.pageQuery(query));
    }

    @Operation(summary = "WP-02 审批效率统计")
    @GetMapping("/statistics")
    public R<?> statistics() {
        return R.ok(instanceService.statistics());
    }

    @Operation(summary = "WP-03 作废流程（管理员强制终止）")
    @PostMapping("/{id}/cancel")
    public R<?> cancel(@PathVariable Long id) {
        WfProcessInstance inst = instanceService.getById(id);
        if (inst == null) throw new BizException("流程实例不存在");
        if (inst.getStatus() != ProcessStatus.IN_PROGRESS.getCode()
                && inst.getStatus() != ProcessStatus.PENDING.getCode()) {
            throw new BizException("仅审批中/待审批的流程可以作废");
        }
        // 终止 Flowable 流程
        try {
            runtimeService.deleteProcessInstance(inst.getFlowableInstanceId(), "管理员作废");
        } catch (Exception e) {
            // 流程可能已结束
        }
        inst.setStatus(ProcessStatus.CANCELLED.getCode());
        inst.setResultComment("管理员强制作废");
        instanceService.updateById(inst);
        return R.ok();
    }
}
