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

    List<SysDictData> listData(String dictType);
    Long createData(DictDataCreateDTO dto);
    void updateData(DictDataCreateDTO dto);
    void deleteData(Long id);
}
