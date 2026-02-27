package com.asset.finance.receipt.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 收款拆分明细项（新增/编辑时使用）
 */
@Data
public class ReceiptDetailItemDTO {

    /** 费项ID */
    private Long feeItemId;

    /** 费项名称（手填或从费项带出） */
    private String feeName;

    /** 拆分金额，必须 > 0 */
    @NotNull(message = "拆分金额不能为空")
    @DecimalMin(value = "0.01", message = "拆分金额必须大于0")
    private BigDecimal amount;

    /** 备注 */
    private String remark;
}
