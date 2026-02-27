package com.asset.finance.receivable.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;
@Data @Schema(description = "应收明细查询参数")
public class ReceivableQueryDTO {
    private Long contractId;
    private Long projectId;
    private Long merchantId;
    private Long feeItemId;
    private Integer status;
    private String accrualMonth;
    private LocalDate dueDateFrom;
    private LocalDate dueDateTo;
    private Boolean overdue;  // true=仅逾期
    private String receivableCode;
    private Integer pageNum;
    private Integer pageSize;
}
