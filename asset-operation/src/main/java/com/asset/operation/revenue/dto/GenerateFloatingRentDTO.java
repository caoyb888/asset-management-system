package com.asset.operation.revenue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 触发浮动租金计算参数 */
@Data
public class GenerateFloatingRentDTO {
    /** 合同ID */
    @NotNull(message = "合同ID不能为空")
    private Long contractId;
    /** 计算月份（格式：YYYY-MM） */
    @NotBlank(message = "计算月份不能为空")
    private String calcMonth;
}
