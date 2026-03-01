package com.asset.system.log.dto;

import com.asset.common.model.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 登录日志查询参数 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LoginLogQueryDTO extends PageQuery {

    /** 用户名（模糊） */
    private String username;

    /** IP地址（模糊） */
    private String ipAddr;

    /** 状态: 0成功 1失败 */
    private Integer status;

    /** 查询开始时间 */
    private LocalDateTime timeFrom;

    /** 查询结束时间 */
    private LocalDateTime timeTo;
}
