package com.asset.report.vo.inv;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 招商业绩对比 VO
 * <p>
 * 按项目或招商负责人维度展示业绩数据，用于业绩显差看板的柱状图对比。
 * </p>
 */
@Data
@Accessors(chain = true)
public class PerformanceVO {

    /** 统计日期 */
    private LocalDate statDate;

    /** 项目ID */
    private Long projectId;

    /** 招商负责人ID（0 = 项目整体，非0 = 指定人员） */
    private Long investmentManagerId;

    /** 业态类型（空串 = 全业态汇总） */
    private String formatType;

    /** 意向协议数（累计） */
    private Integer intentionCount;

    /** 已签意向数 */
    private Integer intentionSigned;

    /** 当期新增意向 */
    private Integer newIntention;

    /** 租赁合同数（累计） */
    private Integer contractCount;

    /** 合同总金额（元） */
    private BigDecimal contractAmount;

    /** 签约面积（㎡） */
    private BigDecimal contractArea;

    /** 当期新增合同 */
    private Integer newContract;

    /** 意向转化率（%） */
    private BigDecimal conversionRate;

    /** 平均租金单价（元/㎡/月） */
    private BigDecimal avgRentPrice;
}
