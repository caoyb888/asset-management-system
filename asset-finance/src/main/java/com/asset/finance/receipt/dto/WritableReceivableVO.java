package com.asset.finance.receipt.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 可核销应收记录 VO（供核销弹窗选择使用）
 */
@Data
public class WritableReceivableVO {

    /** 应收记录ID */
    private Long id;

    /** 应收编码 */
    private String receivableCode;

    /** 费项ID */
    private Long feeItemId;

    /** 费项名称 */
    private String feeName;

    /** 权责月 */
    private String accrualMonth;

    /** 账期开始 */
    private LocalDate billingStart;

    /** 账期结束 */
    private LocalDate billingEnd;

    /** 应收日期 */
    private LocalDate dueDate;

    /** 实际应收金额 */
    private BigDecimal actualAmount;

    /** 已收金额 */
    private BigDecimal receivedAmount;

    /** 欠费金额（待核销余额） */
    private BigDecimal outstandingAmount;

    /** 状态：0待收/1部分收款 */
    private Integer status;
}
