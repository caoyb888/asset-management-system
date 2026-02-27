package com.asset.finance.prepayment.entity;
import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data; import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
@Data @EqualsAndHashCode(callSuper = true) @TableName("fin_prepayment")
public class FinPrepayment extends BaseEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long contractId; private Long projectId; private Long merchantId;
    private BigDecimal balance; private BigDecimal totalIn; private BigDecimal totalOut;
    private Integer status;
}
