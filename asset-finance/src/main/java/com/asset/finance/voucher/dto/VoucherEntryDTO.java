package com.asset.finance.voucher.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 凭证分录 DTO（手动录入时使用）
 */
@Data
public class VoucherEntryDTO {

    /** 来源类型：1收款单/2核销单/3应收单（选填） */
    private Integer sourceType;

    /** 来源单据ID（选填） */
    private Long sourceId;

    /** 会计科目编码 */
    @NotBlank(message = "科目编码不能为空")
    private String accountCode;

    /** 会计科目名称 */
    @NotBlank(message = "科目名称不能为空")
    private String accountName;

    /** 借方金额（与贷方二选一，另一方为 0） */
    @NotNull(message = "借方金额不能为空")
    private BigDecimal debitAmount;

    /** 贷方金额 */
    @NotNull(message = "贷方金额不能为空")
    private BigDecimal creditAmount;

    /** 分录摘要 */
    private String summary;
}
