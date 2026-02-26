package com.asset.operation.ledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 合同台账分页查询参数
 */
@Data
@Schema(description = "合同台账查询参数")
public class LedgerQueryDTO {

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "合同类型（1租赁/2联营/3临时）")
    private Integer contractType;

    @Schema(description = "台账状态（0进行中/1已完成/2已解约）")
    private Integer status;

    @Schema(description = "审核状态（0待审核/1通过/2驳回）")
    private Integer auditStatus;

    @Schema(description = "双签状态（0未完成/1已完成）")
    private Integer doubleSignStatus;

    @Schema(description = "台账编号（模糊查询）")
    private String ledgerCode;

    @Schema(description = "合同到期日期起（yyyy-MM-dd）")
    private String contractEndFrom;

    @Schema(description = "合同到期日期止（yyyy-MM-dd）")
    private String contractEndTo;

    @Schema(description = "页码（默认1）")
    private Integer pageNum = 1;

    @Schema(description = "每页条数（默认20）")
    private Integer pageSize = 20;
}
