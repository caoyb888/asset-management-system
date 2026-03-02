package com.asset.report.vo.inv;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 客户跟进漏斗单阶段数据 VO
 * <p>
 * 漏斗从上到下：意向登记 → 已签意向 → 已签合同。
 * 每条记录代表漏斗的一个阶段，前端据此渲染 ECharts 漏斗图。
 * </p>
 */
@Data
@Accessors(chain = true)
public class FunnelVO {

    /**
     * 漏斗阶段编码
     * <ul>
     *   <li>INTENTION_TOTAL  - 意向登记（总意向数）</li>
     *   <li>INTENTION_SIGNED - 已签意向（已缴意向金）</li>
     *   <li>CONTRACT_SIGNED  - 已签合同（正式租赁合同）</li>
     * </ul>
     */
    private String stage;

    /** 阶段名称（供前端展示） */
    private String stageName;

    /** 阶段数量 */
    private Integer count;

    /** 合同总金额（元），仅 CONTRACT_SIGNED 阶段有值 */
    private BigDecimal amount;

    /** 签约面积（㎡），仅 CONTRACT_SIGNED 阶段有值 */
    private BigDecimal area;

    /**
     * 本阶段相对于上一阶段的转化率（%）
     * <ul>
     *   <li>INTENTION_TOTAL  → 100%（基准）</li>
     *   <li>INTENTION_SIGNED → intentionSigned / intentionTotal × 100</li>
     *   <li>CONTRACT_SIGNED  → contractCount / intentionSigned × 100</li>
     * </ul>
     */
    private BigDecimal conversionRate;

    /** 本阶段相对于漏斗顶部（意向总数）的整体转化率（%） */
    private BigDecimal overallConversionRate;
}
