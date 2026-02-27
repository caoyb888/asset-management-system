package com.asset.finance.receivable.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_receivable_adjustment")
public class FinReceivableAdjustment extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 调整单号 */
    private String adjustmentCode;

    /** 应收记录ID */
    private Long receivableId;

    /** 合同ID */
    private Long contractId;

    /** 调整类型：1增加/2减少 */
    private Integer adjustType;

    /** 调整金额（正数） */
    private BigDecimal adjustAmount;

    /** 调整原因 */
    private String reason;

    /** 状态：0待审批/1通过/2驳回 */
    private Integer status;

    /** OA审批流程ID */
    private String approvalId;

    /** 乐观锁版本号 */
    @Version
    private Integer version;
}
