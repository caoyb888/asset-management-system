package com.asset.system.log.service;

import com.asset.system.auth.entity.SysLoginLog;
import com.asset.system.log.dto.LoginLogQueryDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/** 登录日志 Service */
public interface SysLoginLogService extends IService<SysLoginLog> {

    /** 分页查询登录日志 */
    IPage<SysLoginLog> pageQuery(LoginLogQueryDTO query);

    /** 清空所有登录日志 */
    void clearAll();
}
