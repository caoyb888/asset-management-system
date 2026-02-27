package com.asset.finance.receivable.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 应收调整申请 DTO
 */
@Data
public class AdjustmentCreateDTO {

    /** 应收记录ID */
    @NotNull(message = "应收记录ID不能为空")
    private Long receivableId;

    /** 调整类型：1增加/2减少 */
    @NotNull(message = "调整类型不能为空")
    private Integer adjustType;

    /** 调整金额（正数） */
    @NotNull(message = "调整金额不能为空")
    @DecimalMin(value = "0.01", message = "调整金额必须大于0")
    private BigDecimal adjustAmount;

    /** 调整原因 */
    @NotBlank(message = "调整原因不能为空")
    private String reason;
}
