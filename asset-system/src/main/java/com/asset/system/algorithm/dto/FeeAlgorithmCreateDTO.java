package com.asset.system.algorithm.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 租费算法 新增/编辑 DTO */
@Data
public class FeeAlgorithmCreateDTO {

    /** 编辑时携带 */
    private Long id;

    @NotBlank(message = "算法编码不能为空")
    private String algoCode;

    @NotBlank(message = "算法名称不能为空")
    private String algoName;

    /** 算法类型：1租金 2保证金 3服务费 4其他 */
    @NotNull(message = "算法类型不能为空")
    private Integer algoType;

    /** 计算方式：1固定金额 2比率计算 3阶梯计算 4自定义公式 */
    @NotNull(message = "计算方式不能为空")
    private Integer calcMode;

    @NotBlank(message = "公式不能为空")
    private String formula;

    /** 变量定义列表（JSON Array） */
    private JsonNode variables;

    /** 固定参数（JSON Object） */
    private JsonNode params;

    private String description;
    private Integer status;
}
