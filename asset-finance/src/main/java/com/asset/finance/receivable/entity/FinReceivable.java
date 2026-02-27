package com.asset.finance.receivable.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_receivable")
public class FinReceivable extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 应收编码 */
    private String receivableCode;

    /** 合同ID */
    private Long contractId;

    /** 合同台账ID */
    private Long ledgerId;

    /** 项目ID */
    private Long projectId;

    /** 商家ID */
    private Long merchantId;

    /** 商铺ID */
    private Long shopId;

    /** 费项ID */
    private Long feeItemId;

    /** 费项名称（冗余） */
    private String feeName;

    /** 账期开始 */
    private LocalDate billingStart;

    /** 账期结束 */
    private LocalDate billingEnd;

    /** 权责月 YYYY-MM */
    private String accrualMonth;

    /** 应收日期 */
    private LocalDate dueDate;

    /** 原始应收金额（不可修改） */
    private BigDecimal originalAmount;

    /** 累计调整金额 */
    private BigDecimal adjustAmount;

    /** 累计减免金额 */
    private BigDecimal deductionAmount;

    /** 实际应收 = 原始 + 调整 - 减免 */
    private BigDecimal actualAmount;

    /** 已收金额 */
    private BigDecimal receivedAmount;

    /** 欠费金额 = 实际应收 - 已收 */
    private BigDecimal outstandingAmount;

    /** 状态：0待收/1部分收款/2已收清/3已减免/4已作废 */
    private Integer status;

    /** 是否已打印：0否/1是 */
    private Integer isPrinted;

    /** 是否已开票：0否/1是 */
    private Integer isInvoiced;

    /** 乐观锁版本号 */
    @Version
    private Integer version;
}
