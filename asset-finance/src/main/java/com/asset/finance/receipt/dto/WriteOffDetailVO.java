package com.asset.finance.receipt.dto;

import com.asset.finance.receipt.entity.FinWriteOffDetail;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 核销单详情 VO
 */
@Data
public class WriteOffDetailVO {

    // ─── 核销单主记录 ───────────────────────────────────────
    private Long id;
    private String writeOffCode;
    private Long receiptId;
    private String receiptCode;
    private Long contractId;
    private String contractCode;
    private String contractName;
    private Long merchantId;
    private String merchantName;
    private Long projectId;
    private String projectName;

    /** 核销类型：1收款核销/2保证金核销/3预收款核销/4负数核销 */
    private Integer writeOffType;
    private String writeOffTypeName;

    /** 核销总金额 */
    private BigDecimal totalAmount;

    /** 状态：0待审核/1审核通过/2驳回 */
    private Integer status;
    private String statusName;

    /** OA审批流程ID */
    private String approvalId;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // ─── 核销明细行 ─────────────────────────────────────────
    private List<FinWriteOffDetail> details;
}
