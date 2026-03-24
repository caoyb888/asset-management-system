package com.asset.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 流程定义保存 DTO（新增/更新）
 * <p>
 * 支持两种模式：
 * 1. 可视化配置模式：传入 {@code nodeConfigs}，后端自动生成 BPMN XML
 * 2. XML 源码模式：直接传入 {@code bpmnXml}，两者互斥，nodeConfigs 优先
 */
@Data
@Schema(description = "流程定义保存请求")
public class WfDefinitionSaveDTO {

    @Schema(description = "主键 ID（更新时必填）")
    private Long id;

    @Schema(description = "流程 key（唯一标识，新增必填，更新后不可修改）", example = "INV_INTENTION")
    private String processKey;

    @Schema(description = "流程名称", example = "意向协议审批")
    private String processName;

    @Schema(description = "业务类型枚举（与 ApprovalBusinessType 对应）", example = "INV_INTENTION")
    private String businessType;

    @Schema(description = "是否启用：0 禁用 1 启用", example = "1")
    private Integer isEnabled;

    @Schema(description = "可视化节点配置列表（有值时优先，自动生成 BPMN XML）")
    private List<NodeConfigDTO> nodeConfigs;

    @Schema(description = "BPMN 2.0 XML（nodeConfigs 为空时使用，高级模式）")
    private String bpmnXml;
}
