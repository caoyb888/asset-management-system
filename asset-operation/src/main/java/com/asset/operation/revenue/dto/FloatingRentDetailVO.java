package com.asset.operation.revenue.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 浮动租金详情 VO（含阶梯明细） */
@Data
public class FloatingRentDetailVO {
    private Long id;
    private Long contractId;
    private Long shopId;
    /** 计算月份（YYYY-MM） */
    private String calcMonth;
    /** 月营业额汇总 */
    private BigDecimal monthlyRevenue;
    /** 固定租金（用于取高策略比较） */
    private BigDecimal fixedRent;
    /** 提成比例（%） */
    private BigDecimal commissionRate;
    /** 提成金额 */
    private BigDecimal commissionAmount;
    /** 浮动租金（最终结果） */
    private BigDecimal floatingRent;
    /** 计算公式说明（面向业务人员） */
    private String calcFormula;
    /** 关联生成的应收记录ID（NULL表示未生成应收） */
    private Long receivableId;
    /** 收费方式类型（2固定提成/3阶梯提成/4两者取高） */
    private Integer chargeType;
    /** 收费方式名称 */
    private String chargeTypeName;
    /** 阶梯明细列表（仅阶梯提成时有值） */
    private List<TierDetailVO> tiers;

    /** 阶梯明细 */
    @Data
    public static class TierDetailVO {
        /** 档位序号 */
        private Integer tierNo;
        /** 本档起始营业额（NULL表示从0起） */
        private BigDecimal revenueFrom;
        /** 本档终止营业额（NULL表示无上限） */
        private BigDecimal revenueTo;
        /** 本档提成比例（%） */
        private BigDecimal rate;
        /** 本档提成金额 */
        private BigDecimal tierAmount;
    }
}
