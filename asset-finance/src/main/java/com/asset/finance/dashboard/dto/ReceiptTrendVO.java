package com.asset.finance.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 收款趋势数据点（按月）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "近12个月收款趋势")
public class ReceiptTrendVO {

    @Schema(description = "月份，格式 YYYY-MM")
    private String month;

    @Schema(description = "当月实收金额")
    private BigDecimal amount;
}
