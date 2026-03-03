package com.asset.report.drill.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 数据钻取请求 DTO
 * <p>
 * 调用方式：从父级节点下钻到子级。<br>
 * 例：当前在项目级（fromLevel=1），点击项目101，请求其楼栋数据（fromLevel=1, dimensionId=101）
 * </p>
 */
@Data
@Schema(description = "数据钻取请求参数")
public class DrillDownRequestDTO {

    @Schema(description = "报表编码，用于判断钻取域（ASSET_*/FIN_*/OPR_*/INV_*）", required = true, example = "ASSET_VACANCY_RATE")
    private String reportCode;

    /**
     * 当前所在层级（从哪一层发起下钻）
     * <ul>
     *   <li>资产域：1=项目, 2=楼栋, 3=楼层, 4=商铺（叶子，不可再钻）</li>
     *   <li>财务域：1=项目, 2=费项, 3=应收明细（叶子）</li>
     * </ul>
     */
    @Schema(description = "当前层级（1=项目, 2=楼栋/费项, 3=楼层, 4=商铺）", required = true, example = "1")
    private Integer fromLevel;

    @Schema(description = "当前层级的维度ID（项目ID/楼栋ID/楼层ID）", required = true, example = "101")
    private Long dimensionId;

    @Schema(description = "统计日期（资产域用，格式 yyyy-MM-dd）", example = "2026-02-28")
    private String statDate;

    @Schema(description = "起始月份（财务域用，格式 yyyy-MM）", example = "2025-01")
    private String startMonth;

    @Schema(description = "结束月份（财务域用，格式 yyyy-MM）", example = "2025-12")
    private String endMonth;

    @Schema(description = "业态类型过滤（可选，用于财务域费项→明细层）")
    private String feeItemType;

    @Schema(description = "额外过滤参数（扩展用）")
    private Map<String, Object> extra;
}
