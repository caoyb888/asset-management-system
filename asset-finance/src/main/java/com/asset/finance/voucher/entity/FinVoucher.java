package com.asset.finance.voucher.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_voucher")
public class FinVoucher extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 凭证编号 */
    private String voucherCode;

    /** 项目ID */
    private Long projectId;

    /** 账套 */
    private String accountSet;

    /** 收付类型：1收款/2付款 */
    private Integer payType;

    /** 凭证日期 */
    private LocalDate voucherDate;

    /** 借方合计 */
    private BigDecimal totalDebit;

    /** 贷方合计 */
    private BigDecimal totalCredit;

    /** 状态：0待审核/1已审核/2已上传 */
    private Integer status;

    /** 上传时间 */
    private LocalDateTime uploadTime;

    /** 摘要 */
    private String remark;

    /** 乐观锁版本号 */
    @Version
    private Integer version;
}
