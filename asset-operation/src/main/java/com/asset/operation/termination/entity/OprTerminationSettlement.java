package com.asset.operation.termination.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

/** 解约清算明细表 - 对应 opr_termination_settlement */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("opr_termination_settlement")
public class OprTerminationSettlement extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 解约单ID */
    private Long terminationId;
    /** 明细类型（1未收租金/2违约金/3保证金退还/4其他） */
    private Integer itemType;
    /** 明细名称 */
    private String itemName;
    /** 金额（正数应收/负数应退） */
    private BigDecimal amount;
    /** 备注 */
    private String remark;
}
