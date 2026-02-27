package com.asset.finance.receivable.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
@Data @Schema(description = "欠费统计VO")
public class OverdueStatisticsVO {
    @Schema(description = "逾期30天内金额")
    private BigDecimal overdue30Amount;
    @Schema(description = "逾期30~90天金额")
    private BigDecimal overdue30To90Amount;
    @Schema(description = "逾期90天以上金额")
    private BigDecimal overdueOver90Amount;
    @Schema(description = "逾期总金额")
    private BigDecimal totalOverdueAmount;
    @Schema(description = "逾期总条数")
    private Integer totalOverdueCount;
    @Schema(description = "欠费租户 TOP10")
    private List<ReceivableSummaryVO> topDebtors;
}
