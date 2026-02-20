package com.asset.common.log.aspect;

import com.asset.common.log.annotation.OperLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 操作日志AOP切面 - 无侵入式采集，异步写入
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperLogAspect {

    private final ObjectMapper objectMapper;

    @AfterReturning(pointcut = "@annotation(operLog)", returning = "result")
    public void doAfterReturning(JoinPoint joinPoint, OperLog operLog, Object result) {
        handleLog(joinPoint, operLog, null, result);
    }

    @AfterThrowing(pointcut = "@annotation(operLog)", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, OperLog operLog, Exception e) {
        handleLog(joinPoint, operLog, e, null);
    }

    @Async
    protected void handleLog(JoinPoint joinPoint, OperLog operLog, Exception e, Object result) {
        try {
            HttpServletRequest request = ((ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes()).getRequest();

            // TODO: 构建OperLogEvent并发布到Spring EventBus或MQ
            // 包含: 操作人/IP/模块/业务类型/方法/参数/结果/耗时/异常信息
            log.info("[操作日志] {} - {} | {} {} | user={}",
                    operLog.title(), operLog.businessType(),
                    request.getMethod(), request.getRequestURI(),
                    "currentUser");
        } catch (Exception ex) {
            log.error("操作日志记录失败", ex);
        }
    }
}
