package com.asset.operation.change.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 审批回调 DTO */
@Data
@Schema(description = "审批回调参数")
public class ApprovalCallbackDTO {

    @Schema(description = "OA 审批实例ID")
    private String approvalId;

    @Schema(description = "审批结果（2通过/3驳回）")
    private Integer status;

    @Schema(description = "审批意见")
    private String comment;
}
