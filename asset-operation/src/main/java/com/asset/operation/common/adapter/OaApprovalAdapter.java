package com.asset.operation.common.adapter;

/**
 * OA 审批系统适配器接口
 * 隔离业务代码与外部审批系统的耦合，支持多种 OA 实现替换
 */
public interface OaApprovalAdapter {

    /**
     * 提交审批申请
     *
     * @param businessType 业务类型标识（如 "CONTRACT_CHANGE"、"CONTRACT_TERMINATION"）
     * @param businessId   业务主键ID
     * @param title        审批流程标题（如"合同变更审批-ZB20240101001"）
     * @return 外部审批系统返回的流程实例ID（approvalId），后续用于查询和回调匹配
     */
    String submitApproval(String businessType, Long businessId, String title);

    /**
     * 查询审批状态
     *
     * @param approvalId 外部审批流程实例ID
     * @return 审批状态：0-待审批 / 1-已通过 / 2-已驳回
     */
    Integer queryStatus(String approvalId);

    /**
     * 处理审批回调（OA 系统主动回调本系统时调用）
     *
     * @param approvalId 外部审批流程实例ID
     * @param status     回调状态：1-通过 / 2-驳回
     * @param comment    审批意见
     */
    void processCallback(String approvalId, Integer status, String comment);
}
