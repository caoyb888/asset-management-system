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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 业务字典 ServiceImpl
 * <p>字典数据读取走 Redis 缓存（Key: sys:dict:{dictType}，TTL 60 分钟），
 * 任何写操作都会主动驱逐对应 dictType 的缓存。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDictServiceImpl extends ServiceImpl<SysDictTypeMapper, SysDictType>
        implements SysDictService {

    private static final String CACHE_PREFIX   = "sys:dict:";
    private static final long   CACHE_TTL_MIN  = 60L;

    private final SysDictDataMapper     dictDataMapper;
    private final StringRedisTemplate   redisTemplate;
    private final ObjectMapper          objectMapper;

    // ─── 字典类型 ─────────────────────────────────────────────────────────────

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
        log.info("[字典] 新增字典类型 {} - {}", dto.getDictType(), dto.getDictName());
        return type.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateType(DictTypeCreateDTO dto) {
        SysDictType exist = baseMapper.selectById(dto.getId());
        if (exist == null) throw new SysBizException(SysErrorCode.DICT_TYPE_NOT_FOUND);
        update(new LambdaUpdateWrapper<SysDictType>()
                .eq(SysDictType::getId, dto.getId())
                .set(StringUtils.hasText(dto.getDictName()), SysDictType::getDictName, dto.getDictName())
                .set(dto.getStatus() != null, SysDictType::getStatus, dto.getStatus())
                .set(dto.getRemark() != null, SysDictType::getRemark, dto.getRemark()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteType(Long id) {
        SysDictType type = baseMapper.selectById(id);
        if (type == null) throw new SysBizException(SysErrorCode.DICT_TYPE_NOT_FOUND);
        removeById(id);
        // 级联逻辑删除该类型下所有字典数据
        int deleted = dictDataMapper.logicDeleteByDictType(type.getDictType());
        // 驱逐缓存
        evictCache(type.getDictType());
        log.info("[字典] 删除字典类型 {} 同步清理数据 {} 条", type.getDictType(), deleted);
    }

    @Override
    public void changeStatusType(Long id, Integer status) {
        if (baseMapper.selectById(id) == null) throw new SysBizException(SysErrorCode.DICT_TYPE_NOT_FOUND);
        update(new LambdaUpdateWrapper<SysDictType>().eq(SysDictType::getId, id).set(SysDictType::getStatus, status));
    }

    // ─── 字典数据 ─────────────────────────────────────────────────────────────

    @Override
    public List<SysDictData> listData(String dictType) {
        String cacheKey = CACHE_PREFIX + dictType;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<List<SysDictData>>() {});
            }
        } catch (Exception e) {
            log.warn("[字典缓存] 读取失败 dictType={} err={}", dictType, e.getMessage());
        }

        List<SysDictData> data = dictDataMapper.selectByDictType(dictType);

        try {
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL_MIN, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("[字典缓存] 写入失败 dictType={} err={}", dictType, e.getMessage());
        }
        return data;
    }

    @Override
    public List<SysDictData> listAllData(String dictType) {
        return dictDataMapper.selectAllByDictType(dictType);
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
        evictCache(dto.getDictType());
        return data.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateData(DictDataCreateDTO dto) {
        SysDictData exist = dictDataMapper.selectById(dto.getId());
        if (exist == null) throw new SysBizException(SysErrorCode.DICT_DATA_NOT_FOUND);
        SysDictData data = new SysDictData();
        data.setId(dto.getId());
        data.setDictLabel(dto.getDictLabel());
        data.setDictValue(dto.getDictValue());
        data.setCssClass(dto.getCssClass());
        data.setSortOrder(dto.getSortOrder());
        data.setStatus(dto.getStatus());
        data.setRemark(dto.getRemark());
        dictDataMapper.updateById(data);
        evictCache(exist.getDictType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteData(Long id) {
        SysDictData exist = dictDataMapper.selectById(id);
        if (exist == null) throw new SysBizException(SysErrorCode.DICT_DATA_NOT_FOUND);
        dictDataMapper.deleteById(id);
        evictCache(exist.getDictType());
    }

    @Override
    public void changeStatusData(Long id, Integer status) {
        SysDictData exist = dictDataMapper.selectById(id);
        if (exist == null) throw new SysBizException(SysErrorCode.DICT_DATA_NOT_FOUND);
        SysDictData update = new SysDictData();
        update.setId(id);
        update.setStatus(status);
        dictDataMapper.updateById(update);
        evictCache(exist.getDictType());
    }

    @Override
    public void refreshCache(String dictType) {
        evictCache(dictType);
        // 预热：重新查询并写入缓存
        listData(dictType);
        log.info("[字典缓存] 已刷新 dictType={}", dictType);
    }

    // ─── 私有辅助 ─────────────────────────────────────────────────────────────

    private void evictCache(String dictType) {
        redisTemplate.delete(CACHE_PREFIX + dictType);
    }
}
