package com.asset.finance.receipt.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_write_off_detail")
public class FinWriteOffDetail extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 核销单ID */
    private Long writeOffId;

    /** 应收记录ID */
    private Long receivableId;

    /** 费项ID */
    private Long feeItemId;

    /** 权责月 YYYY-MM */
    private String accrualMonth;

    /** 本次核销金额 */
    private BigDecimal writeOffAmount;

    /** 超出转预存款金额 */
    private BigDecimal overpayAmount;
}
