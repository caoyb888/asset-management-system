package com.asset.system.log.service.impl;

import com.asset.system.log.dto.OperLogQueryDTO;
import com.asset.system.log.entity.SysOperLog;
import com.asset.system.log.mapper.SysOperLogMapper;
import com.asset.system.log.service.SysOperLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 操作日志 ServiceImpl */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysOperLogServiceImpl extends ServiceImpl<SysOperLogMapper, SysOperLog>
        implements SysOperLogService {

    @Override
    public IPage<SysOperLog> pageQuery(OperLogQueryDTO query) {
        return baseMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<SysOperLog>()
                        .like(StringUtils.hasText(query.getModule()), SysOperLog::getModule, query.getModule())
                        .like(StringUtils.hasText(query.getOperUser()), SysOperLog::getOperUser, query.getOperUser())
                        .eq(query.getStatus() != null, SysOperLog::getStatus, query.getStatus())
                        .ge(query.getTimeFrom() != null, SysOperLog::getOperTime, query.getTimeFrom())
                        .le(query.getTimeTo() != null, SysOperLog::getOperTime, query.getTimeTo())
                        .orderByDesc(SysOperLog::getOperTime));
    }

    @Override
    public void clearAll() {
        baseMapper.delete(new LambdaQueryWrapper<>());
        log.info("[操作日志] 已清空所有操作日志");
    }
}
