package com.asset.system.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志实体
 */
@Data
@Builder
@TableName("sys_login_log")
public class SysLoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 */
    private String username;

    /** IP 地址 */
    private String ipAddr;

    /** 浏览器 */
    private String browser;

    /** 操作系统 */
    private String os;

    /** 状态：0=成功 1=失败 */
    private Integer status;

    /** 消息/失败原因 */
    private String msg;

    /** 登录时间 */
    private LocalDateTime loginTime;
}
