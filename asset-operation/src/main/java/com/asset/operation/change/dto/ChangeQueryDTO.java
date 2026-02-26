package com.asset.operation.change.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 合同变更列表查询参数 */
@Data
@Schema(description = "合同变更查询参数")
public class ChangeQueryDTO {

    @Schema(description = "原合同ID")
    private Long contractId;

    @Schema(description = "台账ID")
    private Long ledgerId;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "变更单号（模糊）")
    private String changeCode;

    @Schema(description = "状态（0草稿/1审批中/2通过/3驳回）")
    private Integer status;

    @Schema(description = "变更类型编码（RENT/BRAND/TENANT/FEE/CLAUSE/TERM/AREA/COMPANY）")
    private String changeTypeCode;

    @Schema(description = "页码，默认1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数，默认20")
    private Integer pageSize = 20;
}
