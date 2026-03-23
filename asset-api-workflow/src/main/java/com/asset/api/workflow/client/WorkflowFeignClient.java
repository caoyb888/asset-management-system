package com.asset.api.workflow.client;

import com.asset.api.workflow.dto.ApprovalRecordVO;
import com.asset.api.workflow.dto.ApprovalSubmitDTO;
import com.asset.api.workflow.dto.ProcessInstanceVO;
import com.asset.common.model.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作流 Feign 接口 — 业务模块调用 workflow 服务
 */
@FeignClient(name = "asset-workflow", path = "/wf")
public interface WorkflowFeignClient {

    /** 发起审批 */
    @PostMapping("/approvals/submit")
    R<String> submitApproval(@RequestBody ApprovalSubmitDTO dto);

    /** 查询流程状态 */
    @GetMapping("/approvals/{processInstanceId}/status")
    R<Integer> queryStatus(@PathVariable("processInstanceId") String processInstanceId);

    /** 撤回审批（仅发起人，且第一个节点未操作时可撤回） */
    @PostMapping("/approvals/{processInstanceId}/revoke")
    R<Void> revokeApproval(@PathVariable("processInstanceId") String processInstanceId);

    /** 查询审批记录（timeline） */
    @GetMapping("/approvals/{processInstanceId}/records")
    R<List<ApprovalRecordVO>> getRecords(@PathVariable("processInstanceId") String processInstanceId);

    /** 按业务单据查流程 */
    @GetMapping("/approvals/by-business")
    R<ProcessInstanceVO> getByBusiness(@RequestParam("businessType") String businessType,
                                       @RequestParam("businessId") Long businessId);
}
