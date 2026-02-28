package com.asset.finance.voucher.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 手动创建凭证 DTO
 */
@Data
public class VoucherCreateDTO {

    /** 项目ID */
    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    /** 账套（选填，默认"默认账套"） */
    private String accountSet;

    /** 收付类型：1收款/2付款 */
    @NotNull(message = "收付类型不能为空")
    private Integer payType;

    /** 凭证日期 */
    @NotNull(message = "凭证日期不能为空")
    private LocalDate voucherDate;

    /** 摘要 */
    private String remark;

    /** 分录列表（至少2条，借贷必须平衡） */
    @NotEmpty(message = "分录列表不能为空")
    @Valid
    private List<VoucherEntryDTO> entries;
}
