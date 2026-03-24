package com.asset.workflow.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 流程节点配置表 wf_node_config
 * <p>
 * 存储流程定义的审批链路节点，由 BpmnGeneratorService 将节点列表转换为 BPMN XML。
 * 每条记录对应链路中的一个节点（START / APPROVER / CONDITION / END）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_node_config")
public class WfNodeConfig extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 wf_process_definition.id */
    private Long definitionId;

    /** 节点标识（BPMN element id），同一流程内唯一 */
    private String nodeId;

    /** 节点类型：START / APPROVER / CONDITION / END */
    private String nodeType;

    /** 节点显示名称 */
    private String nodeName;

    /** 节点排序（从 0 开始，END 固定 99） */
    private Integer nodeOrder;

    // ── 审批节点（nodeType = APPROVER）专用字段 ──────────────────────────

    /** 审批人策略：DEPT_LEADER / ROLE / SPECIFIC_USER / INITIATOR_LEADER */
    private String approverStrategy;

    /** 角色编码（approverStrategy = ROLE 时使用，如 ROLE_VP） */
    private String roleCode;

    /** 指定用户 ID（approverStrategy = SPECIFIC_USER 时使用） */
    private Long userId;

    /** 审批超时时长（小时），NULL 表示不限 */
    private Integer timeoutHours;

    // ── 条件节点（nodeType = CONDITION）专用字段 ─────────────────────────

    /** 条件类型：AMOUNT（金额条件）/ CUSTOM（自定义 EL 表达式） */
    private String conditionType;

    /** 比较运算符：GT（>）/ GTE（>=）/ LT（<）/ LTE（<=）/ EQ（=） */
    private String conditionOp;

    /** 条件阈值（conditionType = AMOUNT 时使用） */
    private BigDecimal conditionValue;

    /** 自定义 EL 表达式（conditionType = CUSTOM 时使用，如 ${priority == 1}） */
    private String conditionExpr;

    // ── 通用字段 ─────────────────────────────────────────────────────────

    /** 节点备注 */
    private String remark;
}
