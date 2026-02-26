package com.asset.operation.change.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** 变更影响预览 VO */
@Data
@Schema(description = "变更影响预览结果")
public class ChangeImpactVO {

    @Schema(description = "受影响应收计划笔数")
    private int affectedPlanCount;

    @Schema(description = "原应收总金额")
    private BigDecimal originalTotalAmount;

    @Schema(description = "变更后预估总金额")
    private BigDecimal newTotalAmount;

    @Schema(description = "差额（正数为增加，负数为减少）")
    private BigDecimal amountDiff;

    @Schema(description = "变更字段前后对比（key=字段名, value=[旧值,新值]）")
    private List<Map<String, String>> fieldComparisons;

    @Schema(description = "影响说明文本")
    private String impactDesc;
}
