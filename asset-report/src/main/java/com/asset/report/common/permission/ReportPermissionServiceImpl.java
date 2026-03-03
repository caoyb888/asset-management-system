package com.asset.report.common.permission;

import com.asset.report.mapper.perm.ReportPermMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 报表数据权限服务实现
 * <p>
 * 权限解析策略（优先级从高到低）：
 * <ol>
 *   <li>用户名=admin 或角色 data_scope=1（全部）→ 返回 null（不限制）</li>
 *   <li>用户是项目负责人（biz_project.manager_id=userId）→ 加入可见集合</li>
 *   <li>用户有自定义数据权限部门（sys_role_data）→ 加入可见集合</li>
 * </ol>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportPermissionServiceImpl implements ReportPermissionService {

    private final ReportPermMapper permMapper;

    @Override
    public List<Long> getPermittedProjectIds(Long userId) {
        if (userId == null || userId <= 0) {
            log.warn("[RptPerm] userId 为空或非法，返回空权限");
            return List.of();
        }

        // 1. 管理员判断（username=admin 或拥有 data_scope=1 角色）
        try {
            boolean admin = permMapper.isAdminUser(userId);
            if (admin) {
                log.debug("[RptPerm] userId={} 是管理员，返回 null（不限制）", userId);
                return null;  // null = 管理员，无需过滤
            }
        } catch (Exception e) {
            log.error("[RptPerm] 查询管理员标识失败，userId={}, 降级为空权限: {}", userId, e.getMessage());
            return List.of();
        }

        // 2. 合并多来源的项目 ID（去重）
        Set<Long> projectIds = new LinkedHashSet<>();

        // 2a. 用户是负责人的项目
        try {
            List<Long> managerProjects = permMapper.selectProjectIdsByManagerId(userId);
            if (managerProjects != null) {
                projectIds.addAll(managerProjects);
            }
        } catch (Exception e) {
            log.warn("[RptPerm] 查询负责人项目失败，userId={}: {}", userId, e.getMessage());
        }

        // 2b. 用户通过部门权限关联的项目
        try {
            List<Long> deptProjects = permMapper.selectProjectIdsByUserDeptScope(userId);
            if (deptProjects != null) {
                projectIds.addAll(deptProjects);
            }
        } catch (Exception e) {
            log.warn("[RptPerm] 查询部门关联项目失败，userId={}: {}", userId, e.getMessage());
        }

        log.debug("[RptPerm] userId={} 可见项目数: {}", userId, projectIds.size());
        return new ArrayList<>(projectIds);
    }

    @Override
    public boolean hasFinViewPermission(Long userId) {
        if (userId == null || userId <= 0) return false;
        try {
            // 管理员（data_scope=1 或 username=admin）拥有财务查看权限
            return permMapper.isAdminUser(userId);
        } catch (Exception e) {
            log.warn("[RptPerm] 查询财务查看权限失败，userId={}: {}", userId, e.getMessage());
            return false;
        }
    }
}
