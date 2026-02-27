package com.asset.finance.voucher.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_voucher_entry")
public class FinVoucherEntry extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 凭证ID */
    private Long voucherId;

    /** 来源类型：1收款单/2核销单/3应收单 */
    private Integer sourceType;

    /** 来源单据ID */
    private Long sourceId;

    /** 会计科目编码 */
    private String accountCode;

    /** 会计科目名称 */
    private String accountName;

    /** 借方金额 */
    private BigDecimal debitAmount;

    /** 贷方金额 */
    private BigDecimal creditAmount;

    /** 分录摘要 */
    private String summary;
}
