package com.asset.report.favorite.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户报表收藏 VO
 */
@Data
@Accessors(chain = true)
@Schema(description = "用户报表收藏")
public class FavoriteVO {

    @Schema(description = "收藏记录 ID")
    private Long id;

    @Schema(description = "报表编码", example = "AST_VACANCY_DAILY")
    private String reportCode;

    @Schema(description = "报表名称", example = "空置率统计")
    private String reportName;

    @Schema(description = "前端路由路径", example = "/rpt/asset/vacancy")
    private String routePath;

    @Schema(description = "报表分类：1=资产，2=招商，3=营运，4=财务")
    private Integer category;

    @Schema(description = "收藏排序（数字越小越靠前）")
    private Integer sortOrder;

    @Schema(description = "是否快捷入口")
    private Boolean quickAccess;

    @Schema(description = "收藏时间")
    private LocalDateTime createdAt;
}
