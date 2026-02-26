package com.asset.operation.revenue.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 营收填报新增/修改 DTO */
@Data
public class RevenueReportCreateDTO {
    /** 合同ID（必填） */
    @NotNull(message = "合同ID不能为空")
    private Long contractId;
    /** 项目ID（可由合同自动带出） */
    private Long projectId;
    /** 商铺ID（可由合同自动带出） */
    private Long shopId;
    /** 商家ID（可由合同自动带出） */
    private Long merchantId;
    /** 填报日期（必填，具体某天） */
    @NotNull(message = "填报日期不能为空")
    private LocalDate reportDate;
    /** 营业额（必填，非负） */
    @NotNull(message = "营业额不能为空")
    @DecimalMin(value = "0.00", message = "营业额不能为负数")
    private BigDecimal revenueAmount;
}
