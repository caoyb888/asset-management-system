package com.asset.investment.engine;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 租金计算上下文（封装计算所需全部入参）
 * 不同收费方式使用不同字段子集，未使用字段保持 null 即可
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentCalculateContext {

    // ─── 固定租金（charge_type=1）所需字段 ───
    /** 单价（元/㎡/月） */
    private BigDecimal unitPrice;
    /** 面积（㎡） */
    private BigDecimal area;

    // ─── 提成类（charge_type=2/3/4）所需字段 ───
    /** 提成比例（%，如 5.00 表示 5%） */
    private BigDecimal commissionRate;
    /** 最低提成金额（保底金额） */
    private BigDecimal minCommissionAmount;
    /** 营业额（提成类计算依据） */
    private BigDecimal revenue;

    // ─── 账期范围（所有策略通用，固定租金据此算月数） ───
    /** 账期/阶段开始日期 */
    private LocalDate stageStart;
    /** 账期/阶段结束日期（包含当天） */
    private LocalDate stageEnd;

    // ─── 扩展参数（JSON，阶梯提成各阶段、一次性金额等） ───
    /**
     * 公式参数（来自 inv_intention_fee.formula_params 或 cfg_rent_scheme.formula_json）
     * <ul>
     *   <li>一次性：{@code {"amount": 50000.00}}</li>
     *   <li>阶梯提成：{@code {"stages": [{...}, ...]}}</li>
     * </ul>
     */
    private JsonNode formulaParams;
}
