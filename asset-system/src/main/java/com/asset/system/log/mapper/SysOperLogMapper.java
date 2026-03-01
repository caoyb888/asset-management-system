package com.asset.system.log.mapper;

import com.asset.system.log.entity.SysOperLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 操作日志 Mapper */
@Mapper
public interface SysOperLogMapper extends BaseMapper<SysOperLog> {
}
