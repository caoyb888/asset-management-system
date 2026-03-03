package com.asset.report.favorite.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 新增收藏请求 DTO
 */
@Data
@Schema(description = "新增报表收藏")
public class FavoriteAddDTO {

    @Schema(description = "报表编码（全局唯一），如 AST_VACANCY_DAILY", example = "AST_VACANCY_DAILY")
    @NotBlank(message = "reportCode 不能为空")
    private String reportCode;

    @Schema(description = "报表名称，如 空置率统计", example = "空置率统计")
    @NotBlank(message = "reportName 不能为空")
    private String reportName;

    @Schema(description = "前端路由路径，如 /rpt/asset/vacancy", example = "/rpt/asset/vacancy")
    @NotBlank(message = "routePath 不能为空")
    private String routePath;

    @Schema(description = "报表分类：1=资产，2=招商，3=营运，4=财务", example = "1")
    @NotNull(message = "category 不能为空")
    private Integer category;

    @Schema(description = "是否加入快捷入口", example = "false")
    private Boolean quickAccess = false;
}
