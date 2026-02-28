package com.asset.finance.deposit.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 保证金账户 VO（余额卡片数据）
 */
@Data
public class DepositAccountVO {

    private Long id;

    /** 合同ID */
    private Long contractId;
    private String contractCode;
    private String contractName;

    /** 商家ID */
    private Long merchantId;
    private String merchantName;

    /** 项目ID */
    private Long projectId;
    private String projectName;

    /** 费项ID */
    private Long feeItemId;
    private String feeItemName;

    /** 当前可用余额 */
    private BigDecimal balance;

    /** 累计收入 */
    private BigDecimal totalIn;

    /** 累计冲抵 */
    private BigDecimal totalOffset;

    /** 累计退款 */
    private BigDecimal totalRefund;

    /** 累计罚没 */
    private BigDecimal totalForfeit;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
