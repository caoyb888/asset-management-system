package com.asset.common.log.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * <p>标注在 Controller 方法上，AOP 拦截后异步写入操作日志表</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperLog {

    /** 模块名称 */
    String module() default "";

    /** 操作描述 */
    String action() default "";

    /** 操作类型 */
    OperType type() default OperType.OTHER;

    enum OperType {
        CREATE, UPDATE, DELETE, QUERY, EXPORT, IMPORT, OTHER
    }
}
