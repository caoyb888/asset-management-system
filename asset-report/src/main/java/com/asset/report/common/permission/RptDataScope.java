package com.asset.report.common.permission;

import java.lang.annotation.*;

/**
 * 报表数据权限注解
 * <p>
 * 标注在 Controller 方法上，由 {@link ReportDataPermissionAspect} 拦截：
 * 自动解析当前用户可见项目列表，写入 {@link ReportPermissionContext}（ThreadLocal）。
 * Service 层通过 {@code ReportPermissionContext.get()} 读取后注入 SQL 的 IN 条件。
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @RptDataScope
 * @GetMapping("/asset/dashboard")
 * public R<?> dashboard(ReportQueryParam param) {
 *     // 此时 ReportPermissionContext.get() 已包含当前用户可见项目
 *     return R.ok(assetReportService.dashboard(param));
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RptDataScope {
}
