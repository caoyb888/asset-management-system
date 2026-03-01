package com.asset.system.log.service;

import com.asset.system.log.dto.OperLogQueryDTO;
import com.asset.system.log.entity.SysOperLog;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/** 操作日志 Service */
public interface SysOperLogService extends IService<SysOperLog> {
    IPage<SysOperLog> pageQuery(OperLogQueryDTO query);
    void clearAll();
}
