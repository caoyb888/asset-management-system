package com.asset.investment.contract.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 租赁合同-账期表实体 - 对应 inv_lease_contract_billing 表 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inv_lease_contract_billing")
public class InvLeaseContractBilling extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contractId;
    private Long feeItemId;
    private LocalDate billingStart;
    private LocalDate billingEnd;
    private LocalDate dueDate;
    private BigDecimal amount;
    private Integer billingType;
    private Integer status;
}
