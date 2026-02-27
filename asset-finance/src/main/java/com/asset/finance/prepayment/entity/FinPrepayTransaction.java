package com.asset.finance.prepayment.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_prepay_transaction")
public class FinPrepayTransaction extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预收款账户ID */
    private Long accountId;

    /** 类型：1转入（超额转预存）/2抵冲应收/3退款 */
    private Integer transType;

    /** 金额 */
    private BigDecimal amount;

    /** 交易后余额 */
    private BigDecimal balanceAfter;

    /** 交易日期 */
    private LocalDate transDate;

    /** 关联单据号 */
    private String sourceCode;

    /** 备注 */
    private String remark;
}
