package com.asset.finance.receivable.entity;
import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data; import lombok.EqualsAndHashCode;
import java.math.BigDecimal; import java.time.LocalDate;
@Data @EqualsAndHashCode(callSuper = true) @TableName("fin_receivable")
public class FinReceivable extends BaseEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private String receivableCode; private Long contractId; private Long ledgerId; private Long planId;
    private Long projectId; private Long merchantId; private Long brandId; private Long shopId;
    private Long feeItemId; private String feeName;
    private LocalDate billingStart; private LocalDate billingEnd; private LocalDate dueDate;
    private String accrualMonth;
    private BigDecimal originalAmount; private BigDecimal actualAmount;
    private BigDecimal receivedAmount; private BigDecimal reductionAmount;
    private Integer status; private Integer overdueDays;
}
