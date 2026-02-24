package com.asset.investment.intention.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 意向协议-分铺计租阶段 DTO（单条阶段信息）
 * 通过 POST /inv/intentions/{id}/fee-stages 批量提交
 * 支持一次性提交多个费项的多个商铺阶段
 */
@Data
public class IntentionFeeStageItemDTO {

    /** 所属费项ID（inv_intention_fee.id，必填） */
    @NotNull(message = "费项ID不能为空")
    private Long intentionFeeId;

    /** 商铺ID（分铺计租时填写；整体计租时可为空） */
    private Long shopId;

    /** 阶段开始日期（必填） */
    @NotNull(message = "阶段开始日期不能为空")
    private LocalDate stageStart;

    /** 阶段结束日期（必填） */
    @NotNull(message = "阶段结束日期不能为空")
    private LocalDate stageEnd;

    /** 该阶段单价(元/㎡/月)，固定/两者取高收费时使用 */
    private BigDecimal unitPrice;

    /** 提成比例(%)，提成/两者取高时使用 */
    private BigDecimal commissionRate;

    /** 最低提成金额（保底金额），提成/两者取高时使用 */
    private BigDecimal minCommissionAmount;
}
