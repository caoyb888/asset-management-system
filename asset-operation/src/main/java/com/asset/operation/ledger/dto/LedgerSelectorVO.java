package com.asset.operation.ledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 合同台账选择器 VO（用于前端 ContractSelector 组件下拉展示）
 */
@Data
@Schema(description = "合同台账选择器简化信息")
public class LedgerSelectorVO {

    @Schema(description = "台账ID")
    private Long id;

    @Schema(description = "台账编号")
    private String ledgerCode;

    @Schema(description = "关联合同编号")
    private String contractCode;

    @Schema(description = "商家名称")
    private String merchantName;

    @Schema(description = "商铺编号（第一个商铺）")
    private String shopCode;

    @Schema(description = "合同开始日期")
    private LocalDate contractStart;

    @Schema(description = "合同到期日期")
    private LocalDate contractEnd;

    @Schema(description = "台账状态（0进行中/1已完成/2已解约）")
    private Integer status;
}
