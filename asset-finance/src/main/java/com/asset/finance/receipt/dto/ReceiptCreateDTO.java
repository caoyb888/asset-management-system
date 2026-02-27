package com.asset.finance.receipt.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 新增收款单请求体
 */
@Data
public class ReceiptCreateDTO {

    /** 合同ID（未名款项时可为空） */
    private Long contractId;

    /** 品牌ID */
    private Long brandId;

    /** 店铺编号 */
    private String shopCode;

    /** 实收总金额 */
    @NotNull(message = "收款总金额不能为空")
    @DecimalMin(value = "0.01", message = "收款总金额必须大于0")
    private BigDecimal totalAmount;

    /** 收款方式：1银行转账/2现金/3支票/4POS */
    private Integer paymentMethod = 1;

    /** 银行流水号 */
    private String bankSerialNo;

    /** 付款方名称 */
    private String payerName;

    /** 收款银行 */
    private String bankName;

    /** 收款账号 */
    private String bankAccount;

    /** 是否未名款项：0否/1是，默认0 */
    private Integer isUnnamed = 0;

    /** 核算主体 */
    private String accountingEntity;

    /** 收款日期 */
    @NotNull(message = "收款日期不能为空")
    private LocalDate receiptDate;

    /** 收款人 */
    private String receiver;

    /**
     * 费项拆分明细，合计必须等于 totalAmount
     * 允许为空（不做拆分时后端自动创建一条全额明细）
     */
    @Valid
    @Size(max = 20, message = "拆分明细最多20条")
    private List<ReceiptDetailItemDTO> details;
}
