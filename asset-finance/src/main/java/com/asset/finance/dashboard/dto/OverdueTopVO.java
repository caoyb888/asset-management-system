package com.asset.finance.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 欠费 TOP10 商家数据点
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "欠费TOP10商家")
public class OverdueTopVO {

    @Schema(description = "商家ID")
    private Long merchantId;

    @Schema(description = "商家名称")
    private String merchantName;

    @Schema(description = "欠费金额")
    private BigDecimal overdueAmount;
}
