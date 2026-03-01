package com.asset.system.log.service.impl;

import com.asset.system.auth.entity.SysLoginLog;
import com.asset.system.auth.mapper.SysLoginLogMapper;
import com.asset.system.log.dto.LoginLogQueryDTO;
import com.asset.system.log.service.SysLoginLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 登录日志 ServiceImpl */
@Slf4j
@Service
public class SysLoginLogServiceImpl extends ServiceImpl<SysLoginLogMapper, SysLoginLog>
        implements SysLoginLogService {

    @Override
    public IPage<SysLoginLog> pageQuery(LoginLogQueryDTO query) {
        return baseMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<SysLoginLog>()
                        .like(StringUtils.hasText(query.getUsername()), SysLoginLog::getUsername, query.getUsername())
                        .like(StringUtils.hasText(query.getIpAddr()), SysLoginLog::getIpAddr, query.getIpAddr())
                        .eq(query.getStatus() != null, SysLoginLog::getStatus, query.getStatus())
                        .ge(query.getTimeFrom() != null, SysLoginLog::getLoginTime, query.getTimeFrom())
                        .le(query.getTimeTo() != null, SysLoginLog::getLoginTime, query.getTimeTo())
                        .orderByDesc(SysLoginLog::getLoginTime));
    }

    @Override
    public void clearAll() {
        baseMapper.delete(new LambdaQueryWrapper<>());
        log.info("[登录日志] 已清空所有登录日志");
    }
}
