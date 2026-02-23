package com.asset.investment.contract.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 租赁合同-费项明细表实体 - 对应 inv_lease_contract_fee 表 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "inv_lease_contract_fee", autoResultMap = true)
public class InvLeaseContractFee extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contractId;
    private Long feeItemId;
    private String feeName;
    private Integer chargeType;
    private BigDecimal unitPrice;
    private BigDecimal area;
    private BigDecimal amount;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer periodIndex;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode formulaParams;
}
