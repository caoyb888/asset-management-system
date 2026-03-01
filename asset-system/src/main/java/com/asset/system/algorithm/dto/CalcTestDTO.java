package com.asset.system.algorithm.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/** 算法试算请求 DTO */
@Data
public class CalcTestDTO {

    /** 目标算法 ID */
    @NotNull(message = "算法ID不能为空")
    private Long algoId;

    /**
     * 变量输入值映射
     * key = 变量 key（如 unit_price），value = 数值字符串
     */
    @NotNull(message = "变量值不能为空")
    private Map<String, String> inputs;
}
