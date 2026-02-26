package com.asset.operation.change.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 合同变更新增/编辑 DTO
 * changeFields 为动态字段 Map，按变更类型携带不同内容：
 *   RENT/FEE  → rentAmount(BigDecimal), rentUnit(String)
 *   TERM      → newContractEnd(LocalDate), newContractStart(LocalDate)
 *   AREA      → newRentArea(BigDecimal)
 *   BRAND     → newBrandId(Long), newBrandName(String)
 *   TENANT    → newMerchantId(Long), newMerchantName(String)
 *   COMPANY   → newCompanyName(String)
 *   CLAUSE    → clauseContent(String)
 */
@Data
@Schema(description = "合同变更新增/编辑参数")
public class ChangeCreateDTO {

    @NotNull(message = "合同ID不能为空")
    @Schema(description = "原合同ID", required = true)
    private Long contractId;

    @Schema(description = "关联台账ID")
    private Long ledgerId;

    @NotNull(message = "变更类型不能为空")
    @Schema(description = "变更类型编码列表（可多选）", required = true)
    private List<String> changeTypeCodes;

    @NotNull(message = "变更生效日期不能为空")
    @Schema(description = "变更生效日期", required = true)
    private LocalDate effectiveDate;

    @Schema(description = "变更原因")
    private String reason;

    @Schema(description = "动态变更字段（按变更类型携带不同 key-value）")
    private Map<String, Object> changeFields;
}
