package com.asset.finance.prepayment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 预收款抵冲应收 DTO（直接生效）
 */
@Data
public class PrepayOffsetDTO {

    /** 合同ID（定位账户） */
    @NotNull(message = "合同ID不能为空")
    private Long contractId;

    /** 应收记录ID */
    @NotNull(message = "应收记录ID不能为空")
    private Long receivableId;

    /** 抵冲金额 */
    @NotNull(message = "抵冲金额不能为空")
    @DecimalMin(value = "0.01", message = "抵冲金额必须大于0")
    private BigDecimal amount;

    /** 备注 */
    private String remark;
}
