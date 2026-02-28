package com.asset.finance.prepayment.dto;

import com.asset.common.model.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 预收款流水分页查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PrepayQueryDTO extends PageQuery {

    /** 合同ID */
    private Long contractId;

    /** 预收款账户ID */
    private Long accountId;

    /** 交易类型：1转入/2抵冲/3退款 */
    private Integer transType;
}
