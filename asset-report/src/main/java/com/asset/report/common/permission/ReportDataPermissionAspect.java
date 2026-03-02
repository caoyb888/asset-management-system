package com.asset.report.common.permission;

import com.asset.common.security.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 报表数据权限 AOP 切面
 * <p>
 * 拦截所有标注了 {@link RptDataScope} 的 Controller 方法，
 * 在方法执行前解析当前用户的可见项目列表并写入 {@link ReportPermissionContext}（ThreadLocal），
 * 方法执行完毕后自动清理。
 * </p>
 * <p>
 * Service 层通过 {@code ReportPermissionContext.get()} 读取后注入 SQL 的 project_id IN (...) 条件。
 * </p>
 *
 * <h3>权限语义</h3>
 * <ul>
 *   <li>{@code null}：管理员，不需要 project_id 过滤</li>
 *   <li>空列表：无权限，查询应返回空结果（拦截器已处理，可直接返回）</li>
 *   <li>非空列表：只能查询列表内的项目数据</li>
 * </ul>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ReportDataPermissionAspect {

    private final ReportPermissionService permissionService;

    @Around("@annotation(com.asset.report.common.permission.RptDataScope)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        Long userId = SecurityUtil.getCurrentUserId();
        List<Long> permittedProjectIds = permissionService.getPermittedProjectIds(userId);

        log.debug("[RptDataScope] userId={} 进入权限切面，可见项目数={}",
                userId, permittedProjectIds == null ? "全部(admin)" : permittedProjectIds.size());

        try {
            ReportPermissionContext.set(permittedProjectIds);
            return point.proceed();
        } finally {
            ReportPermissionContext.clear();
        }
    }
}
