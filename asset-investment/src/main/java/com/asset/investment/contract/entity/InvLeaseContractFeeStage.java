package com.asset.investment.contract.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 租赁合同-分铺计租阶段表实体 - 对应 inv_lease_contract_fee_stage 表 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inv_lease_contract_fee_stage")
public class InvLeaseContractFeeStage extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contractFeeId;
    private Long shopId;
    private LocalDate stageStart;
    private LocalDate stageEnd;
    private BigDecimal unitPrice;
    private BigDecimal commissionRate;
    private BigDecimal minCommissionAmount;
    private BigDecimal amount;
}
