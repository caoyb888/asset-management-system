package com.asset.finance.prepayment.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_prepay_account")
public class FinPrepayAccount extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 合同ID */
    private Long contractId;

    /** 商家ID */
    private Long merchantId;

    /** 项目ID */
    private Long projectId;

    /** 费项ID（可按费项分别记余额，为空时通用） */
    private Long feeItemId;

    /** 当前余额 */
    private BigDecimal balance;

    /** 乐观锁版本号 */
    @Version
    private Integer version;
}
