package com.asset.finance.voucher.dto;

import com.asset.finance.voucher.entity.FinVoucherEntry;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 凭证详情 VO（凭证主信息 + 分录列表）
 */
@Data
public class VoucherDetailVO {

    private Long id;

    /** 凭证编号 */
    private String voucherCode;

    /** 项目ID */
    private Long projectId;
    private String projectName;

    /** 账套 */
    private String accountSet;

    /** 收付类型：1收款/2付款 */
    private Integer payType;
    private String payTypeName;

    /** 凭证日期 */
    private LocalDate voucherDate;

    /** 借方合计 */
    private BigDecimal totalDebit;

    /** 贷方合计 */
    private BigDecimal totalCredit;

    /** 状态：0待审核/1已审核/2已上传 */
    private Integer status;
    private String statusName;

    /** 上传时间 */
    private LocalDateTime uploadTime;

    /** 摘要 */
    private String remark;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 分录列表 */
    private List<FinVoucherEntry> entries;
}
