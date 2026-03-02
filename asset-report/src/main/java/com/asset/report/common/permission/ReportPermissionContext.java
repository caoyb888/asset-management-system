package com.asset.report.common.permission;

import java.util.Collections;
import java.util.List;

/**
 * 报表数据权限上下文（ThreadLocal）
 * <p>
 * 由 {@link ReportDataPermissionAspect} 在请求开始时写入，在请求结束时清理。
 * Service 层查询前读取，将允许的项目 ID 列表拼入 SQL WHERE 条件。
 * </p>
 *
 * <h3>权限语义</h3>
 * <ul>
 *   <li>{@code null}：管理员，无限制（不需要 project_id IN ... 过滤）</li>
 *   <li>非空列表：只能查看列表内的项目</li>
 *   <li>空列表：该用户没有任何项目权限，查询应返回空结果</li>
 * </ul>
 */
public final class ReportPermissionContext {

    /** 允许为 null（管理员无限制）或 List（空=无权限，非空=可见范围） */
    private static final ThreadLocal<List<Long>> PERMITTED_PROJECTS = new ThreadLocal<>();

    private ReportPermissionContext() {}

    /**
     * 设置当前请求的可见项目列表
     *
     * @param projectIds null=不限制（管理员），空列表=无权限，非空=有限范围
     */
    public static void set(List<Long> projectIds) {
        PERMITTED_PROJECTS.set(projectIds);
    }

    /**
     * 获取当前请求的可见项目列表
     *
     * @return null=管理员（不限制），空列表=无权限，非空=有限范围
     */
    public static List<Long> get() {
        return PERMITTED_PROJECTS.get();
    }

    /**
     * 清除（在 AOP 切面的 finally 块中调用，避免内存泄漏）
     */
    public static void clear() {
        PERMITTED_PROJECTS.remove();
    }

    /**
     * 判断当前用户是否是管理员（无数据权限限制）
     */
    public static boolean isAdmin() {
        return PERMITTED_PROJECTS.get() == null;
    }

    /**
     * 判断当前用户是否没有任何项目权限
     */
    public static boolean hasNoPermission() {
        List<Long> ids = PERMITTED_PROJECTS.get();
        return ids != null && ids.isEmpty();
    }

    /**
     * 获取不可变的可见项目列表（管理员返回 null，无权限返回空列表）
     */
    public static List<Long> getImmutable() {
        List<Long> ids = PERMITTED_PROJECTS.get();
        if (ids == null) return null;
        return Collections.unmodifiableList(ids);
    }
}
