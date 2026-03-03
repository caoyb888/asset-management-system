package com.asset.report.drill.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 数据钻取结果 VO
 * <p>
 * 每次钻取返回下一层的完整视图：列定义 + 数据行 + 层级元信息。
 * </p>
 */
@Data
@Builder
public class DrillDownResultVO {

    // ─── 层级元信息 ──────────────────────────────────────────────────────────

    /** 当前返回数据所在的层级（父 fromLevel + 1） */
    private int currentLevel;

    /** 当前层级的名称（如「楼栋」「楼层」） */
    private String levelName;

    /** 下一层级的名称（null 表示已到叶子节点） */
    private String nextLevelName;

    /** 是否还可以继续下钻 */
    private boolean canDrillDown;

    // ─── 父级信息（面包屑用） ─────────────────────────────────────────────────

    /** 父节点 ID */
    private Long parentId;

    /** 父节点显示名称 */
    private String parentName;

    // ─── 表格数据 ────────────────────────────────────────────────────────────

    /** 列定义列表（顺序与 rows 字段顺序对应） */
    private List<DrillColumnVO> columns;

    /**
     * 数据行列表（每行是一个 Map，key=列 prop，value=单元格值）
     * <p>value 类型可能是 String/Number/LocalDate/BigDecimal，
     * 前端 JSON 解析后统一为对应 JS 类型。</p>
     */
    private List<Map<String, Object>> rows;

    /** 数据总行数 */
    private int total;
}
