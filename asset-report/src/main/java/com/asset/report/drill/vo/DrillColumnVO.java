package com.asset.report.drill.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 钻取结果列定义 VO
 * <p>
 * 前端根据此定义渲染表格列，drillable=true 的列点击后触发下一层钻取。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrillColumnVO {

    /** 前端行数据的字段名（对应 rows 中 Map 的 key） */
    private String prop;

    /** 列表头显示文字 */
    private String label;

    /** 是否可下钻（点击触发钻取） */
    @Builder.Default
    private boolean drillable = false;

    /** 下钻时传入的 dimensionId 取自行数据的哪个字段（drillable=true 时有效） */
    private String drillIdField;

    /** 对齐方式：left/center/right */
    @Builder.Default
    private String align = "left";

    /** 列宽度（px，0表示自适应） */
    @Builder.Default
    private int width = 0;
}
