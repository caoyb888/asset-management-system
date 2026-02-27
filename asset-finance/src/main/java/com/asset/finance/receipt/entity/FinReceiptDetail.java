package com.asset.finance.receipt.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_receipt_detail")
public class FinReceiptDetail extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 收款单ID */
    private Long receiptId;

    /** 费项ID */
    private Long feeItemId;

    /** 费项名称（冗余） */
    private String feeName;

    /** 拆分金额 */
    private BigDecimal amount;

    /** 备注 */
    private String remark;
}
