package com.asset.report.common.permission;

import java.util.List;

/**
 * 报表数据权限服务接口
 * <p>
 * 根据当前用户身份解析其可见的项目 ID 列表。
 * 调用方：{@link ReportDataPermissionAspect}
 * </p>
 */
public interface ReportPermissionService {

    /**
     * 获取指定用户可见的项目 ID 列表
     * <p>
     * 权限语义：
     * <ul>
     *   <li>返回 {@code null}：管理员，可见全部项目，不需要 IN 过滤</li>
     *   <li>返回空列表：该用户没有任何项目权限</li>
     *   <li>返回非空列表：仅可见列表内的项目</li>
     * </ul>
     * </p>
     *
     * @param userId 当前登录用户ID
     * @return 可见项目ID列表（null=不限制）
     */
    List<Long> getPermittedProjectIds(Long userId);
}
