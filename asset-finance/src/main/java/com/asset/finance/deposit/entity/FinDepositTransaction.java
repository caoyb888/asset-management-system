package com.asset.finance.deposit.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_deposit_transaction")
public class FinDepositTransaction extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 保证金账户ID */
    private Long accountId;

    /** 交易类型：1收入/2冲抵/3退款/4罚没 */
    private Integer transType;

    /** 交易金额 */
    private BigDecimal amount;

    /** 交易后余额 */
    private BigDecimal balanceAfter;

    /** 交易日期 */
    private LocalDate transDate;

    /** 关联单据号 */
    private String sourceCode;

    /** 原因说明 */
    private String reason;

    /** 状态：0待审核/1已审核 */
    private Integer status;

    /** OA审批流程ID */
    private String approvalId;
}
