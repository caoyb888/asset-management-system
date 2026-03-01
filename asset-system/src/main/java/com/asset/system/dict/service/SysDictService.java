package com.asset.system.dict.service;

import com.asset.system.dict.dto.DictDataCreateDTO;
import com.asset.system.dict.dto.DictQueryDTO;
import com.asset.system.dict.dto.DictTypeCreateDTO;
import com.asset.system.dict.entity.SysDictData;
import com.asset.system.dict.entity.SysDictType;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/** 业务字典 Service */
public interface SysDictService extends IService<SysDictType> {
    IPage<SysDictType> pageQueryType(DictQueryDTO query);
    Long createType(DictTypeCreateDTO dto);
    void updateType(DictTypeCreateDTO dto);
    void deleteType(Long id);

    /** 查询启用状态字典数据（业务下拉，走 Redis 缓存）*/
    List<SysDictData> listData(String dictType);
    /** 查询全部字典数据（含停用，管理界面用）*/
    List<SysDictData> listAllData(String dictType);
    Long createData(DictDataCreateDTO dto);
    void updateData(DictDataCreateDTO dto);
    void deleteData(Long id);
    void changeStatusType(Long id, Integer status);
    void changeStatusData(Long id, Integer status);
    /** 手动刷新指定字典类型的缓存 */
    void refreshCache(String dictType);
}
