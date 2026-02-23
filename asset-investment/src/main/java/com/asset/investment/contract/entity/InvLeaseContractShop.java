package com.asset.investment.contract.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

/** 租赁合同-商铺关联表实体 - 对应 inv_lease_contract_shop 表 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inv_lease_contract_shop")
public class InvLeaseContractShop extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contractId;
    private Long shopId;
    private Long buildingId;
    private Long floorId;
    private String formatType;
    private BigDecimal area;
    private BigDecimal rentUnitPrice;
    private BigDecimal propertyUnitPrice;
}
