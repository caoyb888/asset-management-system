package com.asset.api.workflow.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批记录视图（用于 Timeline 展示）
 */
@Data
public class ApprovalRecordVO {

    private Long id;

    /** 关联流程实例 ID */
    private Long instanceId;

    /** Flowable 任务 ID */
    private String flowableTaskId;

    /** 审批节点名称 */
    private String nodeName;

    /** 节点序号 */
    private Integer nodeOrder;

    /** 审批人 ID */
    private Long approverId;

    /** 审批人姓名 */
    private String approverName;

    /** 动作：1=通过 2=驳回 3=转办 4=加签 5=撤回 */
    private Integer action;

    /** 动作名称 */
    private String actionName;

    /** 审批意见 */
    private String comment;

    /** 附件 URL */
    private String attachmentUrls;

    /** 该节点处理耗时（毫秒） */
    private Long durationMs;

    /** 操作时间 */
    private LocalDateTime createdAt;
}
