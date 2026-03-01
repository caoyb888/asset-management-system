package com.asset.system.log.saver;

import com.asset.common.log.saver.OperLogRecord;
import com.asset.common.log.saver.OperLogSaver;
import com.asset.system.log.entity.SysOperLog;
import com.asset.system.log.mapper.SysOperLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 操作日志持久化实现（asset-system）
 * <p>将 {@link OperLogRecord} 映射为 {@link SysOperLog} 写入数据库。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SysOperLogSaverImpl implements OperLogSaver {

    private final SysOperLogMapper operLogMapper;

    @Override
    public void save(OperLogRecord record) {
        SysOperLog entity = new SysOperLog();
        entity.setModule(record.getModule());
        entity.setBizType(record.getBizType());
        entity.setMethod(record.getMethod());
        entity.setRequestMethod(record.getRequestMethod());
        entity.setRequestUrl(record.getRequestUrl());
        entity.setRequestParam(record.getRequestParam());
        entity.setOperUser(record.getOperUser());
        entity.setOperIp(record.getOperIp());
        entity.setStatus(record.getStatus());
        entity.setErrorMsg(record.getErrorMsg());
        entity.setCostTime(record.getCostTime());
        entity.setOperTime(record.getOperTime() != null ? record.getOperTime() : LocalDateTime.now());
        operLogMapper.insert(entity);
    }
}
