package com.asset.common.log.aspect;

import com.asset.common.log.annotation.OperLog;
import com.asset.common.security.util.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 操作日志 AOP 切面
 * <p>异步记录操作日志，避免影响主链路性能</p>
 */
@Slf4j
@Aspect
@Component
public class OperLogAspect {

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint point, OperLog operLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;
        try {
            result = point.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            saveLog(operLog, point, System.currentTimeMillis() - start, error);
        }
    }

    @Async
    protected void saveLog(OperLog operLog, ProceedingJoinPoint point, long costMs, Throwable error) {
        // TODO: 写入 sys_oper_log 表
        log.info("[操作日志] 模块={} 操作={} 类型={} 操作人={} 耗时={}ms 状态={}",
                operLog.module(), operLog.action(), operLog.type(),
                SecurityUtil.getCurrentUsername(), costMs,
                error == null ? "SUCCESS" : "FAIL");
    }
}
