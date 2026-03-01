package com.asset.system.sysconfig.service.impl;

import com.asset.system.common.exception.SysBizException;
import com.asset.system.common.exception.SysErrorCode;
import com.asset.system.sysconfig.dto.SysConfigCreateDTO;
import com.asset.system.sysconfig.dto.SysConfigQueryDTO;
import com.asset.system.sysconfig.entity.SysConfig;
import com.asset.system.sysconfig.mapper.SysConfigMapper;
import com.asset.system.sysconfig.service.SysConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 系统参数配置 ServiceImpl
 * <p>getValueByKey 使用 Redis 缓存（Key: sys:config:{key}，TTL 10分钟），
 * 任何写操作主动清除对应 key 的缓存；refreshCache 清除所有 sys:config:* key。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig>
        implements SysConfigService {

    private static final String CACHE_PREFIX = "sys:config:";
    private static final long   CACHE_TTL    = 10L; // minutes

    private final StringRedisTemplate redisTemplate;

    // ─── 查询 ─────────────────────────────────────────────────────────────

    @Override
    public IPage<SysConfig> pageQuery(SysConfigQueryDTO query) {
        return baseMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<SysConfig>()
                        .like(StringUtils.hasText(query.getConfigKey()), SysConfig::getConfigKey, query.getConfigKey())
                        .like(StringUtils.hasText(query.getConfigName()), SysConfig::getConfigName, query.getConfigName())
                        .eq(StringUtils.hasText(query.getConfigGroup()), SysConfig::getConfigGroup, query.getConfigGroup())
                        .orderByAsc(SysConfig::getConfigGroup, SysConfig::getId));
    }

    @Override
    public List<SysConfig> listByGroup(String group) {
        return baseMapper.selectList(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigGroup, group)
                .orderByAsc(SysConfig::getId));
    }

    @Override
    public String getValueByKey(String key) {
        // 1. 先查 Redis
        String cacheKey = CACHE_PREFIX + key;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) return cached;

        // 2. 查库
        SysConfig config = baseMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key));
        if (config == null) return null;

        // 3. 写缓存
        redisTemplate.opsForValue().set(cacheKey, config.getConfigValue(), CACHE_TTL, TimeUnit.MINUTES);
        return config.getConfigValue();
    }

    // ─── 写操作 ───────────────────────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createConfig(SysConfigCreateDTO dto) {
        long exists = baseMapper.selectCount(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, dto.getConfigKey()));
        if (exists > 0) throw new SysBizException(SysErrorCode.CONFIG_KEY_EXISTS);

        SysConfig config = toEntity(dto);
        baseMapper.insert(config);
        log.info("[系统配置] 新增参数 key={}", config.getConfigKey());
        return config.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(SysConfigCreateDTO dto) {
        SysConfig existing = baseMapper.selectById(dto.getId());
        if (existing == null) throw new SysBizException(SysErrorCode.CONFIG_NOT_FOUND);

        existing.setConfigName(dto.getConfigName());
        existing.setConfigValue(dto.getConfigValue());
        if (StringUtils.hasText(dto.getConfigGroup())) existing.setConfigGroup(dto.getConfigGroup());
        if (dto.getDescription() != null) existing.setDescription(dto.getDescription());

        baseMapper.updateById(existing);
        // 驱逐缓存
        evictCache(existing.getConfigKey());
        log.info("[系统配置] 更新参数 key={}", existing.getConfigKey());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(Long id) {
        SysConfig config = baseMapper.selectById(id);
        if (config == null) throw new SysBizException(SysErrorCode.CONFIG_NOT_FOUND);
        if (config.getIsBuiltIn() != null && config.getIsBuiltIn() == 1) {
            throw new SysBizException(SysErrorCode.CONFIG_BUILT_IN);
        }
        baseMapper.deleteById(id);
        evictCache(config.getConfigKey());
        log.info("[系统配置] 删除参数 key={}", config.getConfigKey());
    }

    @Override
    public void refreshCache() {
        Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("[系统配置] 已刷新缓存，清除 {} 个 key", keys.size());
        }
    }

    // ─── 私有辅助 ─────────────────────────────────────────────────────────

    private SysConfig toEntity(SysConfigCreateDTO dto) {
        SysConfig c = new SysConfig();
        c.setConfigKey(dto.getConfigKey());
        c.setConfigName(dto.getConfigName());
        c.setConfigValue(dto.getConfigValue());
        c.setConfigGroup(StringUtils.hasText(dto.getConfigGroup()) ? dto.getConfigGroup() : "other");
        c.setDescription(dto.getDescription());
        c.setIsBuiltIn(dto.getIsBuiltIn() != null ? dto.getIsBuiltIn() : 0);
        return c;
    }

    private void evictCache(String configKey) {
        redisTemplate.delete(CACHE_PREFIX + configKey);
    }
}
