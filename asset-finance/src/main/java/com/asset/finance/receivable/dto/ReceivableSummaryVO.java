package com.asset.finance.receivable.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
@Data @Schema(description = "应收汇总VO（按合同/项目维度）")
public class ReceivableSummaryVO {
    private Long contractId;
    private String contractCode;
    private String contractName;
    private Long merchantId;
    private String merchantName;
    private Long projectId;
    private String projectName;
    private BigDecimal totalOriginal;    // 原始应收合计
    private BigDecimal totalActual;      // 实际应收合计
    private BigDecimal totalReceived;    // 已收合计
    private BigDecimal totalReduction;   // 减免合计
    private BigDecimal totalOutstanding; // 未收合计（actual - received）
    private Integer overdueCount;        // 逾期条数
    private BigDecimal overdueAmount;    // 逾期金额
}
