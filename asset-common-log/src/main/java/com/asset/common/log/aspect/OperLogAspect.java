package com.asset.common.log.aspect;

import com.asset.common.log.annotation.OperLog;
import com.asset.common.log.saver.OperLogRecord;
import com.asset.common.log.saver.OperLogSaver;
import com.asset.common.security.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 操作日志 AOP 切面
 * <p>在主线程采集请求上下文（IP/URL/参数/用户），再异步写库，避免跨线程 RequestContext 丢失。</p>
 */
@Slf4j
@Aspect
@Component
public class OperLogAspect {

    /** 请求参数最大截断长度 */
    private static final int PARAM_MAX_LEN = 500;

    /** 可选注入：由 asset-system 提供实现；其他微服务无实现时跳过写库 */
    @Autowired(required = false)
    private OperLogSaver operLogSaver;

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint point, OperLog operLog) throws Throwable {
        long start = System.currentTimeMillis();

        // ── 在主线程采集请求上下文 ──────────────────────────────────────────
        String url = "", httpMethod = "", ip = "";
        ServletRequestAttributes sra =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (sra != null) {
            HttpServletRequest req = sra.getRequest();
            url        = req.getRequestURI();
            httpMethod = req.getMethod();
            ip         = extractClientIp(req);
        }
        String username = SecurityUtil.getCurrentUsername();
        String params   = extractParams(point);
        String methodSig = point.getSignature().getDeclaringTypeName() + "."
                + point.getSignature().getName();

        Object result  = null;
        Throwable error = null;
        try {
            result = point.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            long costMs = System.currentTimeMillis() - start;
            OperLogRecord record = buildRecord(operLog, methodSig,
                    url, httpMethod, ip, params, username, costMs, error);
            doSave(record);
        }
    }

    // ─── 异步写库（若 saver 未注入则仅打日志）──────────────────────────────

    @Async
    protected void doSave(OperLogRecord record) {
        if (operLogSaver != null) {
            try {
                operLogSaver.save(record);
            } catch (Exception e) {
                log.warn("[操作日志] 写库失败: {}", e.getMessage());
            }
        }
        log.info("[操作日志] 模块={} 操作={} 用户={} 耗时={}ms 状态={}",
                record.getModule(), record.getAction(), record.getOperUser(),
                record.getCostTime(), record.getStatus() == 1 ? "SUCCESS" : "FAIL");
    }

    // ─── 私有辅助 ─────────────────────────────────────────────────────────

    private OperLogRecord buildRecord(OperLog operLog, String methodSig,
                                      String url, String httpMethod, String ip,
                                      String params, String username, long costMs,
                                      Throwable error) {
        OperLogRecord r = new OperLogRecord();
        r.setModule(operLog.module());
        r.setAction(operLog.action());
        r.setBizType(operLog.type().name());
        r.setMethod(methodSig);
        r.setRequestMethod(httpMethod);
        r.setRequestUrl(url);
        r.setRequestParam(params);
        r.setOperUser(username);
        r.setOperIp(ip);
        r.setStatus(error == null ? 1 : 0);
        r.setErrorMsg(error != null ? truncate(error.getMessage(), 500) : null);
        r.setCostTime(costMs);
        r.setOperTime(LocalDateTime.now());
        return r;
    }

    private String extractParams(ProceedingJoinPoint point) {
        try {
            Object[] args = point.getArgs();
            if (args == null || args.length == 0) return "";
            // 跳过 HttpServletRequest / HttpServletResponse 等不可序列化参数
            StringBuilder sb = new StringBuilder();
            String[] paramNames = ((MethodSignature) point.getSignature()).getParameterNames();
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg instanceof HttpServletRequest) continue;
                if (arg instanceof jakarta.servlet.http.HttpServletResponse) continue;
                sb.append(paramNames != null ? paramNames[i] : "arg" + i)
                  .append("=").append(arg).append("; ");
            }
            return truncate(sb.toString(), PARAM_MAX_LEN);
        } catch (Exception e) {
            return "[参数解析失败]";
        }
    }

    /** 获取客户端真实 IP（兼容代理头） */
    private String extractClientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = req.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = req.getRemoteAddr();
        }
        // X-Forwarded-For 可能有多个，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip == null ? "" : ip;
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
