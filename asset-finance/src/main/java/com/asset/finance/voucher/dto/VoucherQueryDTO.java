package com.asset.finance.voucher.dto;

import com.asset.common.model.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 凭证分页查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VoucherQueryDTO extends PageQuery {

    /** 凭证编号（模糊） */
    private String voucherCode;

    /** 项目ID */
    private Long projectId;

    /** 账套 */
    private String accountSet;

    /** 收付类型：1收款/2付款 */
    private Integer payType;

    /** 状态：0待审核/1已审核/2已上传 */
    private Integer status;

    /** 凭证日期起 */
    private LocalDate dateFrom;

    /** 凭证日期止 */
    private LocalDate dateTo;
}
