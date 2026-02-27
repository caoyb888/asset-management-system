package com.asset.finance.receivable.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 应收减免申请 DTO
 */
@Data
public class DeductionCreateDTO {

    /** 应收记录ID */
    @NotNull(message = "应收记录ID不能为空")
    private Long receivableId;

    /** 减免金额（正数，不超过欠费额） */
    @NotNull(message = "减免金额不能为空")
    @DecimalMin(value = "0.01", message = "减免金额必须大于0")
    private BigDecimal deductionAmount;

    /** 减免原因 */
    @NotBlank(message = "减免原因不能为空")
    private String reason;
}
