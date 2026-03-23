package com.asset.workflow.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 流程实例表 wf_process_instance
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_process_instance")
public class WfProcessInstance extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 流程定义 key */
    private String processKey;

    /** Flowable 引擎流程实例 ID */
    private String flowableInstanceId;

    /** 业务类型枚举 */
    private String businessType;

    /** 业务单据 ID */
    private Long businessId;

    /** 审批标题 */
    private String title;

    /** 发起人用户 ID */
    private Long initiatorId;

    /** 发起人姓名（冗余） */
    private String initiatorName;

    /** 所属项目 ID（数据权限过滤） */
    private Long projectId;

    /** 当前待审批人 ID */
    private Long currentAssigneeId;

    /** 当前审批节点名称 */
    private String currentNodeName;

    /** 状态: 0待审批 1审批中 2已通过 3已驳回 4已撤回 5已作废 */
    private Integer status;

    /** 最终审批意见 */
    private String resultComment;

    /** 优先级: 0普通 1紧急 2加急 */
    private Integer priority;

    /** 扩展变量 JSON */
    private String variablesJson;

    /** 回调地址 */
    private String callbackUrl;

    /** 流程发起时间 */
    private LocalDateTime startedAt;

    /** 流程完成时间 */
    private LocalDateTime finishedAt;

    /** 总耗时（毫秒） */
    private Long durationMs;
}
