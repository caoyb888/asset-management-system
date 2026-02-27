package com.asset.operation.termination.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 解约单分页查询参数 */
@Data
@Schema(description = "解约单分页查询参数")
public class TerminationQueryDTO {

    @Schema(description = "合同ID")
    private Long contractId;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "解约类型（1到期/2提前/3重签）")
    private Integer terminationType;

    @Schema(description = "状态（0草稿/1审批中/2已生效/3驳回）")
    private Integer status;

    @Schema(description = "解约单号（模糊）")
    private String terminationCode;

    @Schema(description = "页码", defaultValue = "1")
    private Integer pageNum;

    @Schema(description = "每页大小", defaultValue = "20")
    private Integer pageSize;
}
