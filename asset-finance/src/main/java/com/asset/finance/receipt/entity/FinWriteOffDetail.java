package com.asset.finance.receipt.entity;
import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data; import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
@Data @EqualsAndHashCode(callSuper = true) @TableName("fin_write_off_detail")
public class FinWriteOffDetail extends BaseEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long writeOffId; private Long receivableId; private BigDecimal amount;
}
