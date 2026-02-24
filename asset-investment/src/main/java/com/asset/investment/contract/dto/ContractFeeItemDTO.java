package com.asset.investment.contract.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 招商合同-费项明细 DTO
 * 通过 POST /inv/contracts/{id}/fees 批量提交
 */
@Data
public class ContractFeeItemDTO {

    /** 收款项目ID（对应 cfg_fee_item.id） */
    private Long feeItemId;

    /** 费项名称（可自定义覆盖收款项目名称） */
    private String feeName;

    /**
     * 收费方式（必填）
     * 1固定租金/2固定提成/3阶梯提成/4两者取高/5一次性
     */
    @NotNull(message = "收费方式不能为空")
    private Integer chargeType;

    /** 单价(元/㎡/月)，固定租金/两者取高时必填 */
    private BigDecimal unitPrice;

    /** 面积(㎡)，固定租金/两者取高时必填 */
    private BigDecimal area;

    /** 费项开始日期（不填则使用合同开始日期） */
    private LocalDate startDate;

    /** 费项结束日期（不填则使用合同结束日期） */
    private LocalDate endDate;

    /** 租期阶段序号（分段租期时递增） */
    private Integer periodIndex;

    /**
     * 计算公式参数(JSON)
     * 存储 commission_rate、min_commission_amount、amount 等动态参数
     */
    private JsonNode formulaParams;
}
