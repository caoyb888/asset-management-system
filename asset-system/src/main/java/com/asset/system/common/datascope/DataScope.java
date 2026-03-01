package com.asset.system.common.datascope;

import java.lang.annotation.*;

/**
 * 数据权限注解
 * <p>标注在 Controller 方法上，由 {@link DataScopeAspect} 拦截并将权限范围写入 {@link DataScopeContext}。</p>
 * <p>Service 层通过 {@code DataScopeContext.get()} 读取后注入查询条件。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {
    /** 部门表别名（SQL 拼接时用），默认空 */
    String deptAlias() default "";
    /** 用户表别名（SQL 拼接时用），默认空 */
    String userAlias() default "";
}
