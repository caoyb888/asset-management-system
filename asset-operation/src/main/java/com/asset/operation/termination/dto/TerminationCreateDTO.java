package com.asset.operation.termination.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 新增/编辑解约单 DTO */
@Data
@Schema(description = "新增/编辑解约单参数")
public class TerminationCreateDTO {

    @Schema(description = "原合同ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long contractId;

    @Schema(description = "关联台账ID（可选，系统自动查找）")
    private Long ledgerId;

    @Schema(description = "解约类型（1到期/2提前/3重签）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer terminationType;

    @Schema(description = "解约日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate terminationDate;

    @Schema(description = "解约原因")
    private String reason;

    @Schema(description = "重签新合同ID（解约类型=3时必填）")
    private Long newContractId;

    @Schema(description = "违约金比例（提前解约时使用，0~1之间，例如0.3表示按30%计算）")
    private BigDecimal penaltyRate;
}
