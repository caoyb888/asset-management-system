package com.asset.api.workflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审批动作 DTO — 前端提交审批操作时使用
 */
@Data
public class ApprovalActionDTO {

    /** 待办任务 ID */
    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    /** 审批动作（ApprovalAction 枚举值：1=通过 2=驳回 3=转办 4=加签 5=撤回） */
    @NotNull(message = "审批动作不能为空")
    private Integer action;

    /** 审批意见 */
    private String comment;

    /** 转办目标用户 ID（action=3 时必填） */
    private Long reassignUserId;
}
