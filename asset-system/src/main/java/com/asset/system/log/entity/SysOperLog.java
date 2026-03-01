package com.asset.system.log.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 操作日志表 sys_oper_log */
@Data
@TableName("sys_oper_log")
public class SysOperLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模块名称 */
    private String module;

    /** 业务类型 */
    private String bizType;

    /** 请求方法（类.方法） */
    private String method;

    /** HTTP方法 */
    private String requestMethod;

    /** 请求URL */
    private String requestUrl;

    /** 请求参数 */
    private String requestParam;

    /** 响应结果 */
    private String responseResult;

    /** 操作人用户名 */
    private String operUser;

    /** 操作IP */
    private String operIp;

    /** 状态: 0成功 1失败 */
    private Integer status;

    /** 错误消息 */
    private String errorMsg;

    /** 耗时(ms) */
    private Long costTime;

    /** 操作时间 */
    private LocalDateTime operTime;
}
