package com.asset.report.common.param;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.util.List;

/**
 * 统一报表查询参数对象
 * <p>
 * 所有报表查询接口共享此参数类，通过 {@link com.asset.report.common.permission.ReportPermissionContext}
 * 自动注入当前用户可见的 projectIds（数据权限过滤，业务层通过 permittedProjectIds 拼接 SQL）。
 * </p>
 */
@Data
@Accessors(chain = true)
public class ReportQueryParam {

    // ==================== 数据权限（由权限切面自动注入，业务层只读）====================

    /**
     * 当前用户可见的项目ID列表（由 ReportDataPermissionAspect 注入）
     * - null  = 管理员，无限制
     * - empty = 该用户无任何项目权限，查询应返回空
     */
    private List<Long> permittedProjectIds;

    // ==================== 主要过滤维度 ====================

    /** 指定单个项目ID（可选，叠加到 permittedProjectIds 权限之上） */
    private Long projectId;

    /** 楼栋ID（0 或 null = 不过滤） */
    private Long buildingId;

    /** 楼层ID（0 或 null = 不过滤） */
    private Long floorId;

    /** 业态类型（null 或空串 = 不过滤） */
    private String formatType;

    /** 商家ID（财务/账龄报表用） */
    private Long merchantId;

    /** 费项ID（财务报表用） */
    private Long feeItemId;

    /** 招商负责人ID（招商报表用） */
    private Long investmentManagerId;

    // ==================== 时间范围 ====================

    /** 统计开始日期（日报用，含边界） */
    private LocalDate startDate;

    /** 统计结束日期（日报用，含边界） */
    private LocalDate endDate;

    /** 统计开始月份（月报用，YYYY-MM 格式） */
    private String startMonth;

    /** 统计结束月份（月报用，YYYY-MM 格式） */
    private String endMonth;

    /** 单个统计日期（精确查询用） */
    private LocalDate statDate;

    /** 单个统计月份（精确查询用，YYYY-MM 格式） */
    private String statMonth;

    // ==================== 时间维度与对比模式 ====================

    /**
     * 时间聚合维度
     * DAY / WEEK / MONTH / YEAR（默认 DAY）
     */
    private TimeUnit timeUnit = TimeUnit.DAY;

    /**
     * 同比/环比模式
     * NONE / YOY（同比，Year-On-Year）/ MOM（环比，Month-On-Month）
     */
    private CompareMode compareMode = CompareMode.NONE;

    // ==================== 排序与分页 ====================

    /** 排序字段（如 stat_date DESC） */
    private String orderBy;

    /** 分页：页码（从 1 开始） */
    private Integer pageNum = 1;

    /** 分页：每页条数 */
    private Integer pageSize = 20;

    // ==================== 内部枚举 ====================

    public enum TimeUnit {
        DAY, WEEK, MONTH, YEAR
    }

    public enum CompareMode {
        NONE,
        /** 同比：与去年同期对比 */
        YOY,
        /** 环比：与上一周期对比 */
        MOM
    }

    // ==================== 便捷工厂方法 ====================

    /** 按单日查询（无分页，通常用于看板） */
    public static ReportQueryParam ofDate(Long projectId, LocalDate statDate) {
        return new ReportQueryParam()
                .setProjectId(projectId)
                .setStatDate(statDate);
    }

    /** 按月查询 */
    public static ReportQueryParam ofMonth(Long projectId, String statMonth) {
        return new ReportQueryParam()
                .setProjectId(projectId)
                .setStatMonth(statMonth);
    }

    /** 按日期范围查询 */
    public static ReportQueryParam ofRange(Long projectId, LocalDate startDate, LocalDate endDate) {
        return new ReportQueryParam()
                .setProjectId(projectId)
                .setStartDate(startDate)
                .setEndDate(endDate);
    }
}
