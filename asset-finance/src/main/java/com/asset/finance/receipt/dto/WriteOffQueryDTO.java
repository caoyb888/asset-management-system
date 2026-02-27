package com.asset.finance.receipt.dto;

import com.asset.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 核销单分页查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WriteOffQueryDTO extends PageQuery {

    /** 核销单号 */
    private String writeOffCode;

    /** 收款单ID */
    private Long receiptId;

    /** 合同ID */
    private Long contractId;

    /** 商家ID */
    private Long merchantId;

    /** 项目ID */
    private Long projectId;

    /** 核销类型：1收款核销/2保证金核销/3预收款核销/4负数核销 */
    private Integer writeOffType;

    /** 状态：0待审核/1审核通过/2驳回 */
    private Integer status;
}
