package com.asset.finance.deposit.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_deposit_account")
public class FinDepositAccount extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 合同ID */
    private Long contractId;

    /** 商家ID */
    private Long merchantId;

    /** 项目ID */
    private Long projectId;

    /** 保证金费项ID（如租赁保证金/装修保证金） */
    private Long feeItemId;

    /** 当前余额 */
    private BigDecimal balance;

    /** 累计收入 */
    private BigDecimal totalIn;

    /** 累计冲抵 */
    private BigDecimal totalOffset;

    /** 累计退款 */
    private BigDecimal totalRefund;

    /** 累计罚没 */
    private BigDecimal totalForfeit;

    /** 乐观锁版本号 */
    @Version
    private Integer version;
}
