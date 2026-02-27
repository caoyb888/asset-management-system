package com.asset.finance.receipt.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_write_off")
public class FinWriteOff extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 核销单号 */
    private String writeOffCode;

    /** 关联收款单ID */
    private Long receiptId;

    /** 合同ID */
    private Long contractId;

    /** 商家ID */
    private Long merchantId;

    /** 项目ID */
    private Long projectId;

    /** 核销类型：1收款核销/2保证金核销/3预收款核销/4负数核销 */
    private Integer writeOffType;

    /** 核销总金额 */
    private BigDecimal totalAmount;

    /** 状态：0待审核/1审核通过/2驳回 */
    private Integer status;

    /** 上传状态：0未上传/1已上传 */
    private Integer uploadStatus;

    /** 上传时间 */
    private LocalDateTime uploadTime;

    /** OA审批流程ID */
    private String approvalId;

    /** 乐观锁版本号 */
    @Version
    private Integer version;
}
