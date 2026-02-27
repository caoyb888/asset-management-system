package com.asset.finance.receipt.entity;
import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data; import lombok.EqualsAndHashCode;
import java.math.BigDecimal; import java.time.LocalDate;
@Data @EqualsAndHashCode(callSuper = true) @TableName("fin_write_off")
public class FinWriteOff extends BaseEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private String writeOffCode; private Long receiptId; private Long contractId;
    private Long projectId; private Long merchantId;
    private BigDecimal writeOffAmount; private LocalDate writeOffDate;
    private Integer writeOffType; private String remark; private Integer status;
}
