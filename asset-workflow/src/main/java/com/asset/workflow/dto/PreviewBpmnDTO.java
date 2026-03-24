package com.asset.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * WD-06 预览 BPMN XML 请求体
 */
@Data
@Schema(description = "预览 BPMN XML 请求")
public class PreviewBpmnDTO {

    @Schema(description = "流程 key", example = "OPR_CONTRACT_CHANGE")
    private String processKey;

    @Schema(description = "流程名称", example = "合同变更审批")
    private String processName;

    @Schema(description = "节点配置列表")
    private List<NodeConfigDTO> nodeConfigs;
}
