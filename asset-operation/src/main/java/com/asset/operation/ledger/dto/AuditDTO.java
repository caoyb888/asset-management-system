package com.asset.operation.ledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 台账审核请求 DTO
 */
@Data
@Schema(description = "台账审核参数")
public class AuditDTO {

    @Schema(description = "审核结果（1通过/2驳回）", required = true)
    private Integer auditStatus;

    @Schema(description = "审核意见")
    private String comment;
}
