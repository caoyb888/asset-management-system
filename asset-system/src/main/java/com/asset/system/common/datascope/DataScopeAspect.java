package com.asset.system.common.datascope;

import com.asset.common.security.util.SecurityUtil;
import com.asset.system.common.enums.DataScope;
import com.asset.system.dept.mapper.SysDeptMapper;
import com.asset.system.role.entity.SysRole;
import com.asset.system.role.mapper.SysRoleDataMapper;
import com.asset.system.role.mapper.SysRoleMapper;
import com.asset.system.user.entity.SysUser;
import com.asset.system.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据权限 AOP 切面
 * <p>
 * 拦截所有标注了 {@link com.asset.system.common.datascope.DataScope} 的 Controller 方法，
 * 解析当前登录用户在各角色下的数据权限范围，将结果写入 {@link DataScopeContext}（ThreadLocal），
 * 业务方法执行完毕后自动清除。
 * </p>
 *
 * <h3>权限优先级（多角色取最宽松）</h3>
 * <ol>
 *   <li>ANY 角色为 SUPER_ADMIN 或 dataScope=1（全部）→ 直接返回 admin=true，不过滤</li>
 *   <li>dataScope=4（本部门及以下）→ 用户部门 + 所有后代部门</li>
 *   <li>dataScope=3（本部门）→ 用户部门</li>
 *   <li>dataScope=2（自定义）→ sys_role_data 中配置的部门列表</li>
 *   <li>dataScope=5（仅本人）→ selfOnly=true，服务层过滤 created_by=userId</li>
 * </ol>
 * 多角色情况下，对所有有效部门 ID 取并集，以最宽松权限为准。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DataScopeAspect {

    private final SysUserMapper    userMapper;
    private final SysRoleMapper    roleMapper;
    private final SysRoleDataMapper roleDataMapper;
    private final SysDeptMapper    deptMapper;

    @Around("@annotation(com.asset.system.common.datascope.DataScope)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        try {
            DataScopeInfo scopeInfo = resolveScope();
            DataScopeContext.set(scopeInfo);
            log.debug("[数据权限] userId={} admin={} selfOnly={} deptIds={}",
                    scopeInfo.getUserId(), scopeInfo.isAdmin(), scopeInfo.isSelfOnly(), scopeInfo.getDeptIds());
            return point.proceed();
        } finally {
            DataScopeContext.clear();
        }
    }

    // ─── 解析逻辑 ─────────────────────────────────────────────────────────────

    private DataScopeInfo resolveScope() {
        Long userId = SecurityUtil.getCurrentUserId();

        // 未登录或测试场景：不限制
        if (userId == null || userId == 0L) {
            return DataScopeInfo.ofAdmin(0L);
        }

        // 查询用户信息（需要 deptId）
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            return DataScopeInfo.ofAdmin(userId);
        }

        // 查询用户所有角色
        List<SysRole> roles = roleMapper.selectByUserId(userId);
        if (roles == null || roles.isEmpty()) {
            // 无角色：仅本人
            return DataScopeInfo.ofSelf(userId);
        }

        // 超级管理员 或 任一角色 dataScope=1（全部数据）→ 不限制
        boolean hasAdminScope = roles.stream().anyMatch(r ->
                "SUPER_ADMIN".equals(r.getRoleCode()) || Integer.valueOf(1).equals(r.getDataScope()));
        if (hasAdminScope) {
            return DataScopeInfo.ofAdmin(userId);
        }

        // 收集所有角色的部门集合（取并集）
        Set<Long> deptIdSet = new LinkedHashSet<>();
        boolean selfOnly = false;

        for (SysRole role : roles) {
            Integer ds = role.getDataScope();
            if (ds == null) {
                ds = DataScope.ALL.getCode();
            }

            switch (DataScope.of(ds)) {
                case ALL:
                    // 已在前面判断，不会到这里
                    return DataScopeInfo.ofAdmin(userId);

                case DEPT_AND_CHILD:
                    // 本部门及所有后代
                    if (user.getDeptId() != null) {
                        deptIdSet.add(user.getDeptId());
                        deptMapper.selectDescendants(user.getDeptId())
                                .forEach(d -> deptIdSet.add(d.getId()));
                    }
                    break;

                case DEPT:
                    // 仅本部门
                    if (user.getDeptId() != null) {
                        deptIdSet.add(user.getDeptId());
                    }
                    break;

                case CUSTOM:
                    // 自定义：从 sys_role_data 读取
                    List<Long> customIds = roleDataMapper.selectDeptIdsByRoleId(role.getId());
                    if (customIds != null) {
                        deptIdSet.addAll(customIds);
                    }
                    break;

                case SELF:
                    selfOnly = true;
                    break;
            }
        }

        // 如果已有部门范围，selfOnly 降级（有部门范围比仅本人宽松）
        if (!deptIdSet.isEmpty()) {
            return DataScopeInfo.ofDepts(userId, new ArrayList<>(deptIdSet));
        }

        if (selfOnly) {
            return DataScopeInfo.ofSelf(userId);
        }

        // 所有角色都没有有效的部门范围 → 给空列表（完全无权限）
        return DataScopeInfo.ofDepts(userId, List.of());
    }
}
