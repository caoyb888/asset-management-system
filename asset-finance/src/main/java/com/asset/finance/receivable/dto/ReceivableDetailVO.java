package com.asset.finance.receivable.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
@Data @Schema(description = "应收明细VO（含冗余展示字段）")
public class ReceivableDetailVO {
    private Long id;
    private String receivableCode;
    private Long contractId; private String contractCode; private String contractName;
    private Long projectId; private String projectName;
    private Long merchantId; private String merchantName;
    private Long feeItemId; private String feeName;
    private LocalDate billingStart; private LocalDate billingEnd; private LocalDate dueDate;
    private String accrualMonth;
    private BigDecimal originalAmount; private BigDecimal actualAmount;
    private BigDecimal receivedAmount; private BigDecimal reductionAmount;
    private BigDecimal outstandingAmount; // actualAmount - receivedAmount
    private Integer status; private String statusName;
    private Integer overdueDays;
    private Boolean isOverdue;
}
