package com.asset.finance.voucher.entity;
import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data; import lombok.EqualsAndHashCode;
import java.math.BigDecimal; import java.time.LocalDate; import java.time.LocalDateTime;
@Data @EqualsAndHashCode(callSuper = true) @TableName("fin_voucher")
public class FinVoucher extends BaseEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private String voucherNo; private Integer voucherType; private Long sourceId; private Integer sourceType;
    private LocalDate voucherDate; private BigDecimal totalDebit; private BigDecimal totalCredit;
    private String summary; private Integer status;
    private Long auditBy; private LocalDateTime auditTime;
}
