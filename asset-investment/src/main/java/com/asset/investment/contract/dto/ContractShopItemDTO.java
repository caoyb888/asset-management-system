package com.asset.investment.contract.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 招商合同-商铺关联 DTO
 * 通过 POST /inv/contracts/{id}/shops 批量提交
 */
@Data
public class ContractShopItemDTO {

    /** 商铺ID（必填） */
    @NotNull(message = "商铺ID不能为空")
    private Long shopId;

    /** 楼栋ID */
    private Long buildingId;

    /** 楼层ID */
    private Long floorId;

    /** 业态 */
    private String formatType;

    /** 租赁面积(㎡) */
    private BigDecimal area;

    /** 租金单价(元/㎡/月) */
    private BigDecimal rentUnitPrice;

    /** 物业费单价(元/㎡/月) */
    private BigDecimal propertyUnitPrice;
}
