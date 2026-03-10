package com.asset.report.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 报表 Redis 缓存配置
 * <p>
 * 缓存策略：
 * <ul>
 *   <li>{@code rpt:dashboard} - 看板聚合接口，TTL 10 分钟（高频访问，数据允许轻微延迟）</li>
 *   <li>{@code rpt:summary}   - 各类汇总报表，TTL 30 分钟</li>
 *   <li>{@code rpt:config}    - 报表配置数据，TTL 60 分钟（低频变更）</li>
 *   <li>默认缓存              - TTL 5 分钟</li>
 * </ul>
 * </p>
 *
 * <h3>Cache Key 规范</h3>
 * 格式：{cacheName}::{keyParam}，例如：
 * <pre>
 * rpt:dashboard::asset:proj1:2026-03-01
 * rpt:summary::finance:proj1:2026-03
 * </pre>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * @Cacheable(value = "rpt:dashboard", key = "'asset:' + #projectId + ':' + #statDate")
 * public AssetDashboardVO getAssetDashboard(Long projectId, LocalDate statDate) { ... }
 *
 * @CacheEvict(value = "rpt:dashboard", allEntries = true)
 * public void refreshDashboard() { ... }
 * }</pre>
 */
@EnableCaching
@Configuration
public class ReportCacheConfig {

    /** 看板缓存名称（10 分钟） */
    public static final String CACHE_DASHBOARD = "rpt:dashboard";
    /** 汇总报表缓存名称（30 分钟） */
    public static final String CACHE_SUMMARY   = "rpt:summary";
    /** 报表配置缓存名称（60 分钟） */
    public static final String CACHE_CONFIG    = "rpt:config";
    /** 权限缓存名称（5 分钟，权限变更后及时失效） */
    public static final String CACHE_PERMISSION = "rpt:permission";

    @Bean
    public CacheManager reportCacheManager(RedisConnectionFactory connectionFactory) {
        // 配置支持 Java 8 日期类型（LocalDate/LocalDateTime）的 ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL);

        // 默认序列化配置（JSON）
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))                           // 默认 TTL 5 分钟
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer(objectMapper)))
                .disableCachingNullValues();                               // 不缓存 null 值

        // 各缓存空间的差异化 TTL
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put(CACHE_DASHBOARD,  defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put(CACHE_SUMMARY,    defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigs.put(CACHE_CONFIG,     defaultConfig.entryTtl(Duration.ofMinutes(60)));
        cacheConfigs.put(CACHE_PERMISSION, defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()                                        // 事务内缓存操作随事务提交
                .build();
    }
}
