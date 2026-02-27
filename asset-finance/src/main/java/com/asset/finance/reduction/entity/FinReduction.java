package com.asset.finance.reduction.entity;
import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data; import lombok.EqualsAndHashCode;
import java.math.BigDecimal; import java.time.LocalDate;
@Data @EqualsAndHashCode(callSuper = true) @TableName("fin_reduction")
public class FinReduction extends BaseEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private String reductionCode; private Long receivableId; private Long contractId;
    private Long projectId; private Long merchantId;
    private Integer reductionType; private BigDecimal reductionAmount;
    private String reason; private String approvalId; private Integer status;
    private LocalDate effectiveDate;
}
