package com.asset.finance.deposit.entity;
import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data; import lombok.EqualsAndHashCode;
import java.math.BigDecimal; import java.time.LocalDate;
@Data @EqualsAndHashCode(callSuper = true) @TableName("fin_deposit_record")
public class FinDepositRecord extends BaseEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long depositId; private Long contractId; private Integer operationType;
    private BigDecimal amount; private BigDecimal balanceAfter;
    private LocalDate operationDate; private String remark;
}
