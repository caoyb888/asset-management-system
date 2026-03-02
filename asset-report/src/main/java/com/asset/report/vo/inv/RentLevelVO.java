package com.asset.report.vo.inv;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 租金水平分析 VO（P1）
 * <p>
 * 按项目/业态维度展示平均租金单价，用于均价热力图和楼层/业态分组柱状图。
 * </p>
 */
@Data
@Accessors(chain = true)
public class RentLevelVO {

    /** 项目ID */
    private Long projectId;

    /** 业态类型 */
    private String formatType;

    /** 招商负责人ID */
    private Long investmentManagerId;

    /** 平均租金单价（元/㎡/月） */
    private BigDecimal avgRentPrice;

    /** 签约面积（㎡） */
    private BigDecimal contractArea;

    /** 合同总金额（元） */
    private BigDecimal contractAmount;

    /** 合同数量 */
    private Integer contractCount;

    /** 同比平均租金（元/㎡/月），用于计算同比增长率 */
    private BigDecimal prevAvgRentPrice;

    /** 平均租金同比增长率（%） */
    private BigDecimal avgRentPriceYoY;
}
