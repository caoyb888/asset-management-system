package com.asset.api.workflow;

import com.asset.api.workflow.dto.ApprovalSubmitDTO;

/**
 * 统一审批服务接口
 * <p>
 * 业务模块通过注入此接口提交审批，具体实现由配置 approval.engine 决定：
 * <ul>
 *   <li>mock（默认）：MockApprovalService，直接生成 Mock approvalId</li>
 *   <li>flowable：FlowableApprovalService，调用 asset-workflow 服务</li>
 * </ul>
 */
public interface ApprovalService {

    /**
     * 提交审批，返回流程实例 ID
     *
     * @param dto 审批提交请求
     * @return 流程实例 ID（Mock 模式返回 MOCK-{type}-{id}-{timestamp}）
     */
    String submit(ApprovalSubmitDTO dto);
}
