package com.asset.system.sysconfig.service;

import com.asset.system.sysconfig.dto.SysConfigCreateDTO;
import com.asset.system.sysconfig.dto.SysConfigQueryDTO;
import com.asset.system.sysconfig.entity.SysConfig;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/** 系统参数配置 Service */
public interface SysConfigService extends IService<SysConfig> {

    /** 分页查询 */
    IPage<SysConfig> pageQuery(SysConfigQueryDTO query);

    /** 按分组查询 */
    List<SysConfig> listByGroup(String group);

    /**
     * 按键查询值（带缓存）
     *
     * @return 配置值，不存在返回 null
     */
    String getValueByKey(String key);

    /** 新增参数 */
    Long createConfig(SysConfigCreateDTO dto);

    /** 更新参数 */
    void updateConfig(SysConfigCreateDTO dto);

    /** 删除参数（内置参数禁止删除） */
    void deleteConfig(Long id);

    /** 刷新缓存（删除 Redis 中所有 sys:config:* key） */
    void refreshCache();
}
