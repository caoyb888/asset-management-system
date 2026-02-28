package com.asset.finance.deposit.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 保证金退款 DTO
 */
@Data
public class DepositRefundDTO {

    /** 合同ID */
    @NotNull(message = "合同ID不能为空")
    private Long contractId;

    /** 退款金额 */
    @NotNull(message = "退款金额不能为空")
    @DecimalMin(value = "0.01", message = "退款金额必须大于0")
    private BigDecimal amount;

    /** 退款原因 */
    private String reason;

    /** 退款银行 */
    private String bankName;

    /** 退款账号 */
    private String bankAccount;

    /** 收款人 */
    private String payee;
}
