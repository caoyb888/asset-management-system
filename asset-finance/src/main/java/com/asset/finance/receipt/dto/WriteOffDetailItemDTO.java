package com.asset.finance.receipt.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 核销明细项（提交核销申请时使用）
 */
@Data
public class WriteOffDetailItemDTO {

    /** 应收记录ID */
    @NotNull(message = "应收记录ID不能为空")
    private Long receivableId;

    /** 费项ID（从应收记录带出） */
    private Long feeItemId;

    /** 权责月（从应收记录带出） */
    private String accrualMonth;

    /** 本次核销金额（负数核销时为负值） */
    @NotNull(message = "核销金额不能为空")
    private BigDecimal writeOffAmount;
}
