package com.asset.system.extfield.service.impl;

import com.asset.system.common.exception.SysBizException;
import com.asset.system.common.exception.SysErrorCode;
import com.asset.system.extfield.dto.ExtFieldCreateDTO;
import com.asset.system.extfield.dto.ExtFieldSortItem;
import com.asset.system.extfield.dto.ExtFieldVO;
import com.asset.system.extfield.entity.SysExtFieldDef;
import com.asset.system.extfield.mapper.SysExtFieldDefMapper;
import com.asset.system.extfield.service.SysExtFieldService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import java.util.stream.Collectors;

/**
 * 用户自定义扩展字段管理 ServiceImpl
 * <p>
 * 缓存策略：
 *   Key: sys:ext:field:{moduleCode}
 *   TTL: 60 分钟
 *   失效时机: 新增/修改/删除/排序时主动 delete
 * 单模块最大字段数: 20
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysExtFieldServiceImpl extends ServiceImpl<SysExtFieldDefMapper, SysExtFieldDef>
        implements SysExtFieldService {

    private static final String CACHE_PREFIX  = "sys:ext:field:";
    private static final long   CACHE_TTL_MIN = 60L;
    private static final int    MAX_FIELDS    = 20;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper        objectMapper;

    // ─── 查询 ──────────────────────────────────────────────────────────────────

    @Override
    public List<ExtFieldVO> listByModule(String moduleCode) {
        String cacheKey = CACHE_PREFIX + moduleCode;

        // 命中缓存直接返回
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(cached)) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<ExtFieldVO>>() {});
            } catch (Exception e) {
                log.warn("[扩展字段] 缓存反序列化失败 moduleCode={}, 降级查库", moduleCode, e);
            }
        }

        // 查库并写缓存
        List<SysExtFieldDef> defs = baseMapper.selectList(
                new LambdaQueryWrapper<SysExtFieldDef>()
                        .eq(SysExtFieldDef::getModuleCode, moduleCode)
                        .orderByAsc(SysExtFieldDef::getSortOrder, SysExtFieldDef::getId));

        List<ExtFieldVO> vos = defs.stream().map(this::toVO).collect(Collectors.toList());
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(vos),
                    CACHE_TTL_MIN, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("[扩展字段] 写入缓存失败 moduleCode={}", moduleCode, e);
        }
        return vos;
    }

    @Override
    public ExtFieldVO getById(Long id) {
        SysExtFieldDef def = baseMapper.selectById(id);
        if (def == null) throw new SysBizException(SysErrorCode.EXT_FIELD_NOT_FOUND);
        return toVO(def);
    }

    // ─── 新增 ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ExtFieldCreateDTO dto) {
        // 新增必填字段校验
        if (!StringUtils.hasText(dto.getModuleCode()))
            throw new SysBizException(SysErrorCode.SYS_5001, "模块编码不能为空");
        if (!StringUtils.hasText(dto.getFieldKey()))
            throw new SysBizException(SysErrorCode.SYS_5001, "字段标识不能为空");
        if (!StringUtils.hasText(dto.getFieldLabel()))
            throw new SysBizException(SysErrorCode.SYS_5001, "字段显示名称不能为空");
        if (!StringUtils.hasText(dto.getFieldType()))
            throw new SysBizException(SysErrorCode.SYS_5001, "字段类型不能为空");

        // 单模块字段数限制
        long count = baseMapper.selectCount(new LambdaQueryWrapper<SysExtFieldDef>()
                .eq(SysExtFieldDef::getModuleCode, dto.getModuleCode()));
        if (count >= MAX_FIELDS) {
            throw new SysBizException(SysErrorCode.EXT_FIELD_LIMIT_EXCEEDED);
        }

        // (moduleCode, fieldKey) 唯一性校验
        long keyCount = baseMapper.selectCount(new LambdaQueryWrapper<SysExtFieldDef>()
                .eq(SysExtFieldDef::getModuleCode, dto.getModuleCode())
                .eq(SysExtFieldDef::getFieldKey, dto.getFieldKey()));
        if (keyCount > 0) throw new SysBizException(SysErrorCode.EXT_FIELD_KEY_EXISTS);

        SysExtFieldDef def = toEntity(dto);
        baseMapper.insert(def);
        evictCache(dto.getModuleCode());
        log.info("[扩展字段] 新增字段 moduleCode={} fieldKey={} id={}", dto.getModuleCode(), dto.getFieldKey(), def.getId());
        return def.getId();
    }

    // ─── 修改 ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ExtFieldCreateDTO dto) {
        SysExtFieldDef exist = baseMapper.selectById(dto.getId());
        if (exist == null) throw new SysBizException(SysErrorCode.EXT_FIELD_NOT_FOUND);

        // fieldKey 不允许修改
        LambdaUpdateWrapper<SysExtFieldDef> wrapper = new LambdaUpdateWrapper<SysExtFieldDef>()
                .eq(SysExtFieldDef::getId, dto.getId())
                .set(StringUtils.hasText(dto.getFieldLabel()),   SysExtFieldDef::getFieldLabel,  dto.getFieldLabel())
                .set(StringUtils.hasText(dto.getFieldType()),    SysExtFieldDef::getFieldType,   dto.getFieldType())
                .set(dto.getOptionsJson() != null,               SysExtFieldDef::getOptionsJson, dto.getOptionsJson())
                .set(dto.getRequired()    != null,               SysExtFieldDef::getRequired,    dto.getRequired())
                .set(dto.getSortOrder()   != null,               SysExtFieldDef::getSortOrder,   dto.getSortOrder())
                .set(dto.getShowInList()  != null,               SysExtFieldDef::getShowInList,  dto.getShowInList())
                .set(dto.getShowInForm()  != null,               SysExtFieldDef::getShowInForm,  dto.getShowInForm())
                .set(dto.getDefaultVal()  != null,               SysExtFieldDef::getDefaultVal,  dto.getDefaultVal())
                .set(dto.getPlaceholder() != null,               SysExtFieldDef::getPlaceholder, dto.getPlaceholder())
                .set(dto.getMaxLength()   != null,               SysExtFieldDef::getMaxLength,   dto.getMaxLength())
                .set(dto.getMinVal()      != null,               SysExtFieldDef::getMinVal,      dto.getMinVal())
                .set(dto.getMaxVal()      != null,               SysExtFieldDef::getMaxVal,      dto.getMaxVal());
        update(wrapper);

        evictCache(exist.getModuleCode());
        log.info("[扩展字段] 修改字段 id={} moduleCode={}", dto.getId(), exist.getModuleCode());
    }

    // ─── 删除 ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysExtFieldDef exist = baseMapper.selectById(id);
        if (exist == null) throw new SysBizException(SysErrorCode.EXT_FIELD_NOT_FOUND);
        removeById(id);
        evictCache(exist.getModuleCode());
        log.info("[扩展字段] 删除字段 id={} moduleCode={} fieldKey={}", id, exist.getModuleCode(), exist.getFieldKey());
    }

    // ─── 排序 ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSort(List<ExtFieldSortItem> items) {
        if (items == null || items.isEmpty()) return;
        for (ExtFieldSortItem item : items) {
            update(new LambdaUpdateWrapper<SysExtFieldDef>()
                    .eq(SysExtFieldDef::getId, item.getId())
                    .set(SysExtFieldDef::getSortOrder, item.getSortOrder()));
        }
        // 排序变更后清除所有可能涉及的模块缓存（通过查库获取 moduleCode 集合）
        List<Long> ids = items.stream().map(ExtFieldSortItem::getId).collect(Collectors.toList());
        baseMapper.selectBatchIds(ids).stream()
                .map(SysExtFieldDef::getModuleCode)
                .distinct()
                .forEach(this::evictCache);
        log.info("[扩展字段] 批量排序 items={}", items.size());
    }

    // ─── 私有辅助 ─────────────────────────────────────────────────────────────

    private void evictCache(String moduleCode) {
        redisTemplate.delete(CACHE_PREFIX + moduleCode);
    }

    private SysExtFieldDef toEntity(ExtFieldCreateDTO dto) {
        SysExtFieldDef def = new SysExtFieldDef();
        def.setModuleCode(dto.getModuleCode());
        def.setFieldKey(dto.getFieldKey());
        def.setFieldLabel(dto.getFieldLabel());
        def.setFieldType(dto.getFieldType());
        def.setOptionsJson(dto.getOptionsJson());
        def.setRequired(dto.getRequired() != null ? dto.getRequired() : Boolean.FALSE);
        def.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        def.setShowInList(dto.getShowInList() != null ? dto.getShowInList() : Boolean.FALSE);
        def.setShowInForm(dto.getShowInForm() != null ? dto.getShowInForm() : Boolean.TRUE);
        def.setDefaultVal(dto.getDefaultVal());
        def.setPlaceholder(dto.getPlaceholder());
        def.setMaxLength(dto.getMaxLength());
        def.setMinVal(dto.getMinVal());
        def.setMaxVal(dto.getMaxVal());
        return def;
    }

    private ExtFieldVO toVO(SysExtFieldDef def) {
        ExtFieldVO vo = new ExtFieldVO();
        vo.setId(def.getId());
        vo.setModuleCode(def.getModuleCode());
        vo.setFieldKey(def.getFieldKey());
        vo.setFieldLabel(def.getFieldLabel());
        vo.setFieldType(def.getFieldType());
        vo.setOptionsJson(def.getOptionsJson());
        vo.setRequired(def.getRequired());
        vo.setSortOrder(def.getSortOrder());
        vo.setShowInList(def.getShowInList());
        vo.setShowInForm(def.getShowInForm());
        vo.setDefaultVal(def.getDefaultVal());
        vo.setPlaceholder(def.getPlaceholder());
        vo.setMaxLength(def.getMaxLength());
        vo.setMinVal(def.getMinVal());
        vo.setMaxVal(def.getMaxVal());
        vo.setCreatedAt(def.getCreatedAt());
        vo.setUpdatedAt(def.getUpdatedAt());
        return vo;
    }
}
