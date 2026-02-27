package com.asset.finance.receipt.dto;

import com.asset.finance.receipt.entity.FinReceiptDetail;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 收款单详情 VO（含明细列表和冗余展示字段）
 */
@Data
public class ReceiptDetailVO {

    private Long id;
    private String receiptCode;
    private Long contractId;
    private String contractCode;
    private String contractName;
    private Long projectId;
    private String projectName;
    private Long merchantId;
    private String merchantName;
    private Long brandId;
    private String shopCode;
    private BigDecimal totalAmount;
    private Integer paymentMethod;
    private String paymentMethodName;
    private String bankSerialNo;
    private String payerName;
    private String bankName;
    private String bankAccount;
    private Integer isUnnamed;
    private String accountingEntity;
    private LocalDate receiptDate;
    private String receiver;
    private Integer status;
    private String statusName;
    private BigDecimal writeOffAmount;
    private BigDecimal prepayAmount;

    /** 费项拆分明细列表 */
    private List<FinReceiptDetail> details;
}
