package com.asset.report.vo.fin;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 凭证统计 VO（P1）
 * <p>
 * 接口 GET /rpt/fin/voucher-stats 返回值，
 * 直接查询 fin_voucher 业务表，按月份/项目维度汇总凭证状态分布及金额。
 * </p>
 */
@Data
@Accessors(chain = true)
public class FinVoucherStatsVO {

    /** 统计月份（YYYY-MM） */
    private String statMonth;

    /** 项目ID */
    private Long projectId;

    /** 凭证总数 */
    private Integer totalVouchers;

    /** 待审核凭证数（status=0） */
    private Integer pendingVouchers;

    /** 已审核凭证数（status=1） */
    private Integer approvedVouchers;

    /** 已上传凭证数（status=2） */
    private Integer uploadedVouchers;

    /** 借方合计（元） */
    private BigDecimal totalDebit;

    /** 贷方合计（元） */
    private BigDecimal totalCredit;
}
