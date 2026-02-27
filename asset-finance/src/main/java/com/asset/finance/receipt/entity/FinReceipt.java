package com.asset.finance.receipt.entity;
import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data; import lombok.EqualsAndHashCode;
import java.math.BigDecimal; import java.time.LocalDate;
@Data @EqualsAndHashCode(callSuper = true) @TableName("fin_receipt")
public class FinReceipt extends BaseEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private String receiptCode; private Long contractId; private Long projectId; private Long merchantId;
    private BigDecimal totalAmount; private BigDecimal unwrittenAmount;
    private Integer paymentMethod; private LocalDate receiptDate;
    private String bankSerialNo; private String payerName; private String bankName;
    private String remark; private Integer status;
}
