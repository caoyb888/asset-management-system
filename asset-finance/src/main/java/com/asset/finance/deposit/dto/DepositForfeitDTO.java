package com.asset.finance.deposit.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 保证金罚没 DTO
 */
@Data
public class DepositForfeitDTO {

    /** 合同ID */
    @NotNull(message = "合同ID不能为空")
    private Long contractId;

    /** 罚没金额 */
    @NotNull(message = "罚没金额不能为空")
    @DecimalMin(value = "0.01", message = "罚没金额必须大于0")
    private BigDecimal amount;

    /** 罚没原因（违约事项说明） */
    @jakarta.validation.constraints.NotBlank(message = "罚没原因不能为空")
    private String reason;
}
