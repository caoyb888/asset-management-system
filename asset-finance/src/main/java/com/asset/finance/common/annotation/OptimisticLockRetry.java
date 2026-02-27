package com.asset.finance.common.annotation;

import java.lang.annotation.*;

/**
 * 乐观锁重试注解
 * 标注在 Service 方法上，{@link com.asset.finance.common.aspect.OptimisticLockRetryAspect}
 * 捕获乐观锁冲突并按退避策略自动重试
 *
 * <pre>
 * &#64;OptimisticLockRetry
 * public void updateBalance(...) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OptimisticLockRetry {

    /** 最大重试次数，默认 3 次 */
    int maxRetries() default 3;
}
