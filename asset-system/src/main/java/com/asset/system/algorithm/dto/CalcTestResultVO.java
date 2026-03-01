package com.asset.system.algorithm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 算法试算结果 VO */
@Data
@AllArgsConstructor
public class CalcTestResultVO {

    /** 最终计算结果（保留2位小数） */
    private String result;

    /** 展开后的公式（变量替换为实际值） */
    private String expandedFormula;

    /** 计算过程说明 */
    private String detail;
}
