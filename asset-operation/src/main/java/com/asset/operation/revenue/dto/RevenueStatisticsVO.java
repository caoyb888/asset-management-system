package com.asset.operation.revenue.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 营收月度汇总统计 VO */
@Data
public class RevenueStatisticsVO {
    /** 查询月份（YYYY-MM） */
    private String reportMonth;
    /** 月度总营业额 */
    private BigDecimal totalRevenue;
    /** 已填报合同数 */
    private Integer reportedContractCount;
    /** 月总天数 */
    private Integer totalDays;
    /** 按合同/商铺汇总明细 */
    private List<ContractMonthlyVO> details;

    /** 合同月度汇总 */
    @Data
    public static class ContractMonthlyVO {
        private Long contractId;
        private String contractCode;
        private Long shopId;
        private String shopCode;
        private String merchantName;
        /** 月累计营业额 */
        private BigDecimal monthlyRevenue;
        /** 已填报天数 */
        private Integer reportDays;
        /** 月总天数 */
        private Integer totalDays;
        /** 是否完整填报（已填报天数=月总天数） */
        private Boolean complete;
    }
}
