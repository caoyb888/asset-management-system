package com.asset.finance.common.aspect;

import com.asset.finance.common.annotation.OptimisticLockRetry;
import com.asset.finance.common.exception.FinBizException;
import com.asset.finance.common.exception.FinErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

/**
 * 乐观锁冲突自动重试切面
 *
 * <p>捕获 MyBatis-Plus 乐观锁冲突异常（version 字段不匹配导致的更新失败），
 * 按指数退避策略重试：50ms → 100ms → 200ms，超限后抛出 {@link FinErrorCode#FIN_5002}。
 *
 * <p>使用方式：在需要乐观锁保护的 Service 方法上标注 {@link OptimisticLockRetry}。
 */
@Slf4j
@Aspect
@Component
public class OptimisticLockRetryAspect {

    /** 退避时间序列（毫秒），索引对应第 n 次重试 */
    private static final long[] BACKOFF_MS = {50, 100, 200};

    @Around("@annotation(retry)")
    public Object around(ProceedingJoinPoint pjp, OptimisticLockRetry retry) throws Throwable {
        int maxRetries = retry.maxRetries();
        String methodName = ((MethodSignature) pjp.getSignature()).getMethod().getName();
        int attempt = 0;

        while (true) {
            try {
                return pjp.proceed();
            } catch (OptimisticLockingFailureException | com.baomidou.mybatisplus.core.exceptions.MybatisPlusException ex) {
                // 仅捕获乐观锁相关异常，其他异常直接向上抛
                if (!isOptimisticLockConflict(ex)) {
                    throw ex;
                }
                attempt++;
                if (attempt >= maxRetries) {
                    log.warn("[乐观锁重试] 方法={} 达到最大重试次数 {}，放弃", methodName, maxRetries);
                    throw new FinBizException(FinErrorCode.FIN_5002);
                }
                long backoff = BACKOFF_MS[Math.min(attempt - 1, BACKOFF_MS.length - 1)];
                log.warn("[乐观锁重试] 方法={} 第 {}/{} 次冲突，{}ms 后重试", methodName, attempt, maxRetries, backoff);
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new FinBizException(FinErrorCode.FIN_5002);
                }
            }
        }
    }

    /**
     * 判断是否为乐观锁冲突：
     * MyBatis-Plus @Version 更新失败时抛出 OptimisticLockingFailureException，
     * 部分版本也可能是 MybatisPlusException，通过消息文本辅助识别
     */
    private boolean isOptimisticLockConflict(Exception ex) {
        if (ex instanceof OptimisticLockingFailureException) {
            return true;
        }
        String msg = ex.getMessage();
        return msg != null && (msg.contains("optimistic") || msg.contains("version") || msg.contains("乐观锁"));
    }
}
