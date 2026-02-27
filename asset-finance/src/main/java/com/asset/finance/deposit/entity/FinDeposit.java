package com.asset.finance.deposit.entity;
import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data; import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
@Data @EqualsAndHashCode(callSuper = true) @TableName("fin_deposit")
public class FinDeposit extends BaseEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long contractId; private Long projectId; private Long merchantId;
    private BigDecimal requiredAmount; private BigDecimal paidAmount; private BigDecimal balance;
    private Integer status;
}
