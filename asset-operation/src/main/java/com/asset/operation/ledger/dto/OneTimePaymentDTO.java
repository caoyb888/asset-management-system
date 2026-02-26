package com.asset.operation.ledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 一次性首款录入 DTO
 * 支持单笔/多笔/历史账期三种录入模式
 */
@Data
@Schema(description = "一次性首款录入参数")
public class OneTimePaymentDTO {

    @Schema(description = "收款项目ID", required = true)
    private Long feeItemId;

    @Schema(description = "金额", required = true)
    private BigDecimal amount;

    @Schema(description = "账期开始日期")
    private LocalDate billingStart;

    @Schema(description = "账期结束日期")
    private LocalDate billingEnd;

    @Schema(description = "录入类型（1单笔/2多笔/3历史账期）", required = true)
    private Integer entryType;

    @Schema(description = "备注")
    private String remark;
}
