package com.asset.investment.intention.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 意向协议-商铺关联 DTO（单条商铺信息）
 * 通过 POST /inv/intentions/{id}/shops 批量提交
 */
@Data
public class IntentionShopItemDTO {

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
}
