package com.asset.system.algorithm.dto;

import com.asset.common.model.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 租费算法分页查询参数 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FeeAlgorithmQueryDTO extends PageQuery {

    /** 算法名称关键字 */
    private String algoName;

    /** 算法类型筛选 */
    private Integer algoType;

    /** 计算方式筛选 */
    private Integer calcMode;

    /** 状态筛选 */
    private Integer status;
}
