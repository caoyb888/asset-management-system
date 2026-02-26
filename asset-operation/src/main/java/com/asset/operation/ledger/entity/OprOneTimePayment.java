package com.asset.operation.ledger.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 一次性首款记录表 - 对应 opr_one_time_payment */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("opr_one_time_payment")
public class OprOneTimePayment extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 合同台账ID */
    private Long ledgerId;
    /** 合同ID */
    private Long contractId;
    /** 收款项目ID */
    private Long feeItemId;
    /** 关联生成的应收计划ID（生成应收后回填） */
    private Long receivableId;
    /** 金额 */
    private BigDecimal amount;
    /** 账期开始 */
    private LocalDate billingStart;
    /** 账期结束 */
    private LocalDate billingEnd;
    /** 录入类型（1单笔/2多笔/3历史账期） */
    private Integer entryType;
    /** 备注 */
    private String remark;
}
