package com.asset.common.log.saver;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志记录 DTO
 * <p>由 OperLogAspect 在主线程中填充，传递给 OperLogSaver 异步写库，避免跨线程丢失 RequestContext。</p>
 */
@Data
public class OperLogRecord {

    /** 模块名称 */
    private String module;

    /** 操作描述 */
    private String action;

    /** 业务类型（枚举名称） */
    private String bizType;

    /** Java 方法（类名.方法名） */
    private String method;

    /** HTTP 请求方式 */
    private String requestMethod;

    /** 请求 URL */
    private String requestUrl;

    /** 请求参数（JSON截断，最多500字符） */
    private String requestParam;

    /** 操作人用户名 */
    private String operUser;

    /** 操作来源 IP */
    private String operIp;

    /** 执行状态: 1成功 0失败 */
    private Integer status;

    /** 错误消息（失败时） */
    private String errorMsg;

    /** 耗时（毫秒） */
    private Long costTime;

    /** 操作时间 */
    private LocalDateTime operTime;
}
