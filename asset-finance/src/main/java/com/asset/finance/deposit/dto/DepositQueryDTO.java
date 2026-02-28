package com.asset.finance.deposit.dto;

import com.asset.common.model.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 保证金流水分页查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DepositQueryDTO extends PageQuery {

    /** 合同ID */
    private Long contractId;

    /** 保证金账户ID */
    private Long accountId;

    /** 交易类型：1收入/2冲抵/3退款/4罚没 */
    private Integer transType;

    /** 状态：0待审核/1已审核/2驳回 */
    private Integer status;
}
