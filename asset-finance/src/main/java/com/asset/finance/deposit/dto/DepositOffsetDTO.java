package com.asset.finance.deposit.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 保证金冲抵应收 DTO
 */
@Data
public class DepositOffsetDTO {

    /** 合同ID（定位账户） */
    @NotNull(message = "合同ID不能为空")
    private Long contractId;

    /** 应收记录ID（冲抵哪条应收） */
    @NotNull(message = "应收记录ID不能为空")
    private Long receivableId;

    /** 冲抵金额 */
    @NotNull(message = "冲抵金额不能为空")
    @DecimalMin(value = "0.01", message = "冲抵金额必须大于0")
    private BigDecimal amount;

    /** 冲抵原因 */
    private String reason;
}
