package com.asset.investment.contract.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 招商合同-分铺计租阶段 DTO
 * 通过 POST /inv/contracts/{id}/fee-stages 批量提交
 */
@Data
public class ContractFeeStageItemDTO {

    /** 所属合同费项ID（inv_lease_contract_fee.id，必填） */
    @NotNull(message = "合同费项ID不能为空")
    private Long contractFeeId;

    /** 商铺ID（分铺计租时填写；整体计租时可为空） */
    private Long shopId;

    /** 阶段开始日期（必填） */
    @NotNull(message = "阶段开始日期不能为空")
    private LocalDate stageStart;

    /** 阶段结束日期（必填） */
    @NotNull(message = "阶段结束日期不能为空")
    private LocalDate stageEnd;

    /** 该阶段单价(元/㎡/月) */
    private BigDecimal unitPrice;

    /** 提成比例(%) */
    private BigDecimal commissionRate;

    /** 最低提成金额（保底金额） */
    private BigDecimal minCommissionAmount;
}
