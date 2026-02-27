package com.asset.finance.voucher.entity;
import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data; import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
@Data @EqualsAndHashCode(callSuper = true) @TableName("fin_voucher_entry")
public class FinVoucherEntry extends BaseEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long voucherId; private Integer lineNo;
    private String accountCode; private String accountName; private String summary;
    private BigDecimal debitAmount; private BigDecimal creditAmount;
}
