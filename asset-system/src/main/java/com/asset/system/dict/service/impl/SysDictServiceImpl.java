package com.asset.system.dict.service.impl;

import com.asset.system.common.exception.SysBizException;
import com.asset.system.common.exception.SysErrorCode;
import com.asset.system.dict.dto.DictDataCreateDTO;
import com.asset.system.dict.dto.DictQueryDTO;
import com.asset.system.dict.dto.DictTypeCreateDTO;
import com.asset.system.dict.entity.SysDictData;
import com.asset.system.dict.entity.SysDictType;
import com.asset.system.dict.mapper.SysDictDataMapper;
import com.asset.system.dict.mapper.SysDictTypeMapper;
import com.asset.system.dict.service.SysDictService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/** 业务字典 ServiceImpl */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDictServiceImpl extends ServiceImpl<SysDictTypeMapper, SysDictType>
        implements SysDictService {

    private final SysDictDataMapper dictDataMapper;

    @Override
    public IPage<SysDictType> pageQueryType(DictQueryDTO query) {
        return baseMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<SysDictType>()
                        .like(StringUtils.hasText(query.getDictName()), SysDictType::getDictName, query.getDictName())
                        .like(StringUtils.hasText(query.getDictType()), SysDictType::getDictType, query.getDictType())
                        .eq(query.getStatus() != null, SysDictType::getStatus, query.getStatus())
                        .orderByDesc(SysDictType::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createType(DictTypeCreateDTO dto) {
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getDictType, dto.getDictType()));
        if (count > 0) throw new SysBizException(SysErrorCode.DICT_TYPE_EXISTS);
        SysDictType type = new SysDictType();
        type.setDictName(dto.getDictName());
        type.setDictType(dto.getDictType());
        type.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        type.setRemark(dto.getRemark());
        baseMapper.insert(type);
        return type.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateType(DictTypeCreateDTO dto) {
        if (baseMapper.selectById(dto.getId()) == null) throw new SysBizException(SysErrorCode.DICT_TYPE_NOT_FOUND);
        update(new LambdaUpdateWrapper<SysDictType>()
                .eq(SysDictType::getId, dto.getId())
                .set(StringUtils.hasText(dto.getDictName()), SysDictType::getDictName, dto.getDictName())
                .set(dto.getStatus() != null, SysDictType::getStatus, dto.getStatus())
                .set(dto.getRemark() != null, SysDictType::getRemark, dto.getRemark()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteType(Long id) {
        if (baseMapper.selectById(id) == null) throw new SysBizException(SysErrorCode.DICT_TYPE_NOT_FOUND);
        removeById(id);
        // 同步删除字典数据（通过dictType级联，这里不做物理删除，保留历史数据）
    }

    @Override
    public List<SysDictData> listData(String dictType) {
        return dictDataMapper.selectByDictType(dictType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createData(DictDataCreateDTO dto) {
        SysDictData data = new SysDictData();
        data.setDictType(dto.getDictType());
        data.setDictLabel(dto.getDictLabel());
        data.setDictValue(dto.getDictValue());
        data.setCssClass(dto.getCssClass());
        data.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        data.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        data.setRemark(dto.getRemark());
        dictDataMapper.insert(data);
        return data.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateData(DictDataCreateDTO dto) {
        if (dictDataMapper.selectById(dto.getId()) == null) throw new SysBizException(SysErrorCode.DICT_TYPE_NOT_FOUND);
        SysDictData data = new SysDictData();
        data.setId(dto.getId());
        data.setDictLabel(dto.getDictLabel());
        data.setDictValue(dto.getDictValue());
        data.setCssClass(dto.getCssClass());
        data.setSortOrder(dto.getSortOrder());
        data.setStatus(dto.getStatus());
        data.setRemark(dto.getRemark());
        dictDataMapper.updateById(data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteData(Long id) {
        dictDataMapper.deleteById(id);
    }
}
