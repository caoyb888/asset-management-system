package com.asset.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审批操作记录表 wf_approval_record
 * <p>
 * 不继承 BaseEntity（无逻辑删除和审计字段，只有 created_at）
 */
@Data
@TableName("wf_approval_record")
public class WfApprovalRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 wf_process_instance.id */
    private Long instanceId;

    /** Flowable 任务 ID */
    private String flowableTaskId;

    /** 审批节点名称 */
    private String nodeName;

    /** 节点序号 */
    private Integer nodeOrder;

    /** 审批人用户 ID */
    private Long approverId;

    /** 审批人姓名 */
    private String approverName;

    /** 动作: 1通过 2驳回 3转办 4加签 5撤回 6催办 */
    private Integer action;

    /** 审批意见 */
    private String comment;

    /** 附件 URL（JSON 数组） */
    private String attachmentUrls;

    /** 该节点处理耗时（毫秒） */
    private Long durationMs;

    /** 操作时间 */
    private LocalDateTime createdAt;
}
