package com.asset.finance.prepayment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预收款账户 VO（余额卡片数据）
 */
@Data
public class PrepayAccountVO {

    private Long id;

    /** 合同信息 */
    private Long contractId;
    private String contractCode;
    private String contractName;

    /** 商家信息 */
    private Long merchantId;
    private String merchantName;

    /** 项目信息 */
    private Long projectId;
    private String projectName;

    /** 费项ID（为空表示通用账户） */
    private Long feeItemId;

    /** 当前可用余额 */
    private BigDecimal balance;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
