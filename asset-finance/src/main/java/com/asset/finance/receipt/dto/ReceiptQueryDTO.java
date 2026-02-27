package com.asset.finance.receipt.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 收款单分页查询参数
 */
@Data
public class ReceiptQueryDTO {

    /** 合同ID */
    private Long contractId;

    /** 商家ID */
    private Long merchantId;

    /** 项目ID */
    private Long projectId;

    /** 核销状态：0待核销/1部分核销/2已全部核销/3已作废 */
    private Integer status;

    /** 是否未名款项：0否/1是 */
    private Integer isUnnamed;

    /** 收款方式：1银行转账/2现金/3支票/4POS */
    private Integer paymentMethod;

    /** 收款日期起 */
    private LocalDate receiptDateFrom;

    /** 收款日期止 */
    private LocalDate receiptDateTo;

    /** 收款单号（模糊） */
    private String receiptCode;

    /** 页码，默认1 */
    private Integer pageNum = 1;

    /** 每页条数，默认20 */
    private Integer pageSize = 20;
}
