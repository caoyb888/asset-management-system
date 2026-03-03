package com.asset.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户报表收藏表（rpt_user_favorite）
 * <p>
 * 冗余存储 reportCode/reportName/routePath/category，
 * 避免 JOIN rpt_config（rpt_config 初始化数据可能不完整）。
 * </p>
 */
@Data
@Accessors(chain = true)
@TableName("rpt_user_favorite")
public class RptUserFavorite {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID（关联 sys_user.id） */
    private Long userId;

    /** 报表配置ID（关联 rpt_config.id，未初始化时为 0） */
    private Long reportId;

    /** 报表编码，如 AST_VACANCY_DAILY（全局唯一标识） */
    private String reportCode;

    /** 报表名称，如 "空置率统计" */
    private String reportName;

    /** 前端路由路径，如 /rpt/asset/vacancy */
    private String routePath;

    /** 报表分类：1=资产，2=招商，3=营运，4=财务 */
    private Integer category;

    /** 收藏排序（数字越小越靠前） */
    private Integer sortOrder;

    /** 是否快捷入口（0=否，1=是） */
    private Boolean quickAccess;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
