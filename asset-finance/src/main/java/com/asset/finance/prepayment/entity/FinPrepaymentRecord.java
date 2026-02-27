package com.asset.finance.prepayment.entity;
import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data; import lombok.EqualsAndHashCode;
import java.math.BigDecimal; import java.time.LocalDate;
@Data @EqualsAndHashCode(callSuper = true) @TableName("fin_prepayment_record")
public class FinPrepaymentRecord extends BaseEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long prepaymentId; private Long contractId; private Integer operationType;
    private BigDecimal amount; private BigDecimal balanceAfter;
    private LocalDate operationDate; private Long refReceivableId; private String remark;
}
