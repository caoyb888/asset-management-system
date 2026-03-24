package com.asset.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 单个流程节点配置 DTO
 * <p>
 * 前端可视化设计器传入，后端将节点列表转换为 BPMN XML。
 */
@Data
@Schema(description = "流程节点配置")
public class NodeConfigDTO {

    @Schema(description = "节点标识（BPMN element id，同一流程内唯一）", example = "node_dept")
    private String nodeId;

    @Schema(description = "节点类型：START / APPROVER / CONDITION / END", example = "APPROVER")
    private String nodeType;

    @Schema(description = "节点显示名称", example = "部门主管审批")
    private String nodeName;

    @Schema(description = "节点排序（从 0 开始，END 固定 99）", example = "1")
    private Integer nodeOrder;

    // ── 审批节点（nodeType = APPROVER）字段 ──────────────────────────────

    @Schema(description = "审批人策略：DEPT_LEADER / ROLE / SPECIFIC_USER / INITIATOR_LEADER",
            example = "ROLE")
    private String approverStrategy;

    @Schema(description = "角色编码（strategy=ROLE 时使用）", example = "ROLE_VP")
    private String roleCode;

    @Schema(description = "指定用户 ID（strategy=SPECIFIC_USER 时使用）", example = "1001")
    private Long userId;

    @Schema(description = "审批超时时长（小时），null 表示不限", example = "48")
    private Integer timeoutHours;

    // ── 条件节点（nodeType = CONDITION）字段 ─────────────────────────────

    @Schema(description = "条件类型：AMOUNT（金额）/ CUSTOM（自定义）", example = "AMOUNT")
    private String conditionType;

    @Schema(description = "比较运算符：GT / GTE / LT / LTE / EQ", example = "GTE")
    private String conditionOp;

    @Schema(description = "条件阈值（金额条件时使用）", example = "100000")
    private BigDecimal conditionValue;

    @Schema(description = "自定义 EL 表达式（CUSTOM 条件时使用）", example = "${priority == 1}")
    private String conditionExpr;

    @Schema(description = "节点备注")
    private String remark;
}
