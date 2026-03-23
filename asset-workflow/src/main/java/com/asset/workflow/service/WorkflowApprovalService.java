package com.asset.workflow.service;

import com.asset.api.workflow.dto.*;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 统一审批服务 — 核心业务接口
 */
public interface WorkflowApprovalService {

    /** 发起审批，返回流程实例 ID（flowableInstanceId） */
    String submit(ApprovalSubmitDTO dto);

    /** 通过审批 */
    void approve(Long instanceId, String comment, Long approverId, String approverName);

    /** 驳回审批 */
    void reject(Long instanceId, String comment, Long approverId, String approverName);

    /** 撤回审批（发起人操作，仅第一个节点未审批时可撤回） */
    void revoke(Long instanceId);

    /** 转办 */
    void reassign(Long instanceId, Long targetUserId, String comment, Long operatorId, String operatorName);

    /** 催办（发送通知） */
    void urge(Long instanceId);

    /** 查询流程详情 */
    ProcessInstanceVO getDetail(Long instanceId);

    /** 查询审批记录 */
    List<ApprovalRecordVO> getRecords(Long instanceId);

    /** 按业务单据查流程 */
    ProcessInstanceVO getByBusiness(String businessType, Long businessId);

    /** 我的待办 */
    IPage<ProcessInstanceVO> todoPage(TaskPageQuery query, Long userId);

    /** 我的已办 */
    IPage<ProcessInstanceVO> donePage(TaskPageQuery query, Long userId);

    /** 我发起的 */
    IPage<ProcessInstanceVO> initiatedPage(TaskPageQuery query, Long userId);

    /** 待办数量 */
    int todoCount(Long userId);
}
