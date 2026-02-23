package com.asset.investment.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 账期明细（账期生成器 BillingGenerator 的输出单元）
 * 对应数据库表 inv_intention_billing / inv_lease_contract_billing 的单行数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingPeriod {

    /** 账期开始日期（含） */
    private LocalDate billingStart;

    /** 账期结束日期（含） */
    private LocalDate billingEnd;

    /**
     * 应收日期
     * <ul>
     *   <li>预付/当期：等于 billingStart（期初付款）</li>
     *   <li>后付：等于 billingEnd（期末付款）</li>
     * </ul>
     */
    private LocalDate dueDate;

    /**
     * 账期类型
     * <ul>
     *   <li>1 = 首账期（可能涉及免租期、部分月等特殊处理）</li>
     *   <li>2 = 正常账期</li>
     * </ul>
     */
    private int billingType;
}
