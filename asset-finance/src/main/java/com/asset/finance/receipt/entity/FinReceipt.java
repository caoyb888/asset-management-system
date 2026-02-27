package com.asset.finance.receipt.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_receipt")
public class FinReceipt extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 收款单号 */
    private String receiptCode;

    /** 合同ID */
    private Long contractId;

    /** 项目ID */
    private Long projectId;

    /** 商家ID */
    private Long merchantId;

    /** 品牌ID */
    private Long brandId;

    /** 店铺编号 */
    private String shopCode;

    /** 实收总金额 */
    private BigDecimal totalAmount;

    /** 收款方式：1银行转账/2现金/3支票/4POS */
    private Integer paymentMethod;

    /** 银行流水号 */
    private String bankSerialNo;

    /** 付款方名称 */
    private String payerName;

    /** 收款银行 */
    private String bankName;

    /** 收款账号 */
    private String bankAccount;

    /** 是否未名款项：0否/1是 */
    private Integer isUnnamed;

    /** 核算主体 */
    private String accountingEntity;

    /** 收款日期 */
    private LocalDate receiptDate;

    /** 收款人 */
    private String receiver;

    /** 状态：0待核销/1部分核销/2已全部核销/3已作废 */
    private Integer status;

    /** 已核销金额 */
    private BigDecimal writeOffAmount;

    /** 转预存款金额 */
    private BigDecimal prepayAmount;

    /** 乐观锁版本号 */
    @Version
    private Integer version;
}
