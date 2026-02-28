package com.asset.finance.deposit.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 保证金缴纳 DTO（记录收入，直接生效，无需审批）
 */
@Data
public class DepositPayInDTO {

    /** 合同ID */
    @NotNull(message = "合同ID不能为空")
    private Long contractId;

    /** 缴纳金额 */
    @NotNull(message = "缴纳金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于0")
    private BigDecimal amount;

    /** 关联收款单号（选填） */
    private String sourceCode;

    /** 备注 */
    private String reason;
}
