package com.asset.operation.revenue.dto;

import com.asset.common.model.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 营收填报列表查询参数 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RevenueReportQueryDTO extends PageQuery {
    /** 项目ID */
    private Long projectId;
    /** 合同ID */
    private Long contractId;
    /** 商家ID */
    private Long merchantId;
    /** 商铺ID */
    private Long shopId;
    /** 填报月份（YYYY-MM） */
    private String reportMonth;
    /** 填报日期范围-开始（YYYY-MM-DD） */
    private String reportDateFrom;
    /** 填报日期范围-结束（YYYY-MM-DD） */
    private String reportDateTo;
    /** 状态（0待确认/1已确认） */
    private Integer status;
}
