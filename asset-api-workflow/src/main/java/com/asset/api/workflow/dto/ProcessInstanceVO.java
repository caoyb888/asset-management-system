package com.asset.api.workflow.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 流程实例视图
 */
@Data
public class ProcessInstanceVO {

    private Long id;

    /** 流程定义 key */
    private String processKey;

    /** Flowable 引擎流程实例 ID */
    private String flowableInstanceId;

    /** 业务类型 */
    private String businessType;

    /** 业务类型名称 */
    private String businessTypeName;

    /** 业务单据 ID */
    private Long businessId;

    /** 审批标题 */
    private String title;

    /** 发起人 ID */
    private Long initiatorId;

    /** 发起人姓名 */
    private String initiatorName;

    /** 所属项目 ID */
    private Long projectId;

    /** 当前待审批人 ID */
    private Long currentAssigneeId;

    /** 当前审批节点名称 */
    private String currentNodeName;

    /** 状态：0=待审批 1=审批中 2=已通过 3=已驳回 4=已撤回 5=已作废 */
    private Integer status;

    /** 状态名称 */
    private String statusName;

    /** 最终审批意见 */
    private String resultComment;

    /** 优先级 */
    private Integer priority;

    /** 扩展变量 */
    private Map<String, Object> variables;

    /** 流程发起时间 */
    private LocalDateTime startedAt;

    /** 流程完成时间 */
    private LocalDateTime finishedAt;

    /** 总耗时（毫秒） */
    private Long durationMs;

    private LocalDateTime createdAt;
}
