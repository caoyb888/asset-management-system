package com.asset.system.sysconfig;

import com.asset.system.common.exception.SysBizException;
import com.asset.system.sysconfig.dto.SysConfigCreateDTO;
import com.asset.system.sysconfig.entity.SysConfig;
import com.asset.system.sysconfig.mapper.SysConfigMapper;
import com.asset.system.sysconfig.service.impl.SysConfigServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * §4.12 系统配置 — Service 单元测试
 * CFG-U-01 ~ CFG-U-05
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("§4.12 系统配置 Service 单元测试")
class SysConfigServiceTest {

    @Mock SysConfigMapper configMapper;
    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    @InjectMocks SysConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", configMapper);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ─── 辅助 ─────────────────────────────────────────────────────────────────

    private SysConfig config(Long id, String key, String value, int isBuiltIn) {
        SysConfig c = new SysConfig();
        c.setId(id);
        c.setConfigKey(key);
        c.setConfigName("配置-" + key);
        c.setConfigValue(value);
        c.setIsBuiltIn(isBuiltIn);
        c.setConfigGroup("basic");
        return c;
    }

    // ─── CFG-U-01 ─────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("CFG-U-01 按Key查询-命中缓存：不查DB")
    void getByKey_hitCache_returnFromRedis() {
        when(valueOps.get("sys:config:test.login.maxRetry")).thenReturn("5");

        String value = service.getValueByKey("test.login.maxRetry");

        assertThat(value).isEqualTo("5");
        verify(configMapper, never()).selectOne(any());
    }

    // ─── CFG-U-02 ─────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("CFG-U-02 按Key查询-未命中缓存：查DB并写入Redis(TTL=10min)")
    void getByKey_missCache_queryDb() {
        when(valueOps.get("sys:config:test.login.maxRetry")).thenReturn(null);
        when(configMapper.selectOne(any()))
                .thenReturn(config(91001L, "test.login.maxRetry", "5", 1));

        String value = service.getValueByKey("test.login.maxRetry");

        assertThat(value).isEqualTo("5");
        verify(configMapper).selectOne(any());
        verify(valueOps).set(eq("sys:config:test.login.maxRetry"), eq("5"), eq(10L), eq(TimeUnit.MINUTES));
    }

    // ─── CFG-U-03 ─────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("CFG-U-03 新增-Key重复：抛出参数键已存在异常")
    void createConfig_duplicateKey_throws() {
        when(configMapper.selectCount(any())).thenReturn(1L);

        SysConfigCreateDTO dto = new SysConfigCreateDTO();
        dto.setConfigKey("test.login.maxRetry");
        dto.setConfigName("登录失败最大次数");
        dto.setConfigValue("5");

        assertThatThrownBy(() -> service.createConfig(dto))
                .isInstanceOf(SysBizException.class)
                .hasMessageContaining("已存在");
    }

    // ─── CFG-U-04 ─────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("CFG-U-04 删除-系统内置：抛出内置参数不可删除异常")
    void deleteConfig_builtIn_throws() {
        when(configMapper.selectById(91001L))
                .thenReturn(config(91001L, "test.login.maxRetry", "5", 1));

        assertThatThrownBy(() -> service.deleteConfig(91001L))
                .isInstanceOf(SysBizException.class)
                .hasMessageContaining("内置");
    }

    // ─── CFG-U-05 ─────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("CFG-U-05 编辑配置-刷新缓存：updateById + 缓存驱逐")
    void updateConfig_refreshCache() {
        when(configMapper.selectById(91003L))
                .thenReturn(config(91003L, "test.notice.enabled", "true", 0));
        when(configMapper.updateById(any(SysConfig.class))).thenReturn(1);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        SysConfigCreateDTO dto = new SysConfigCreateDTO();
        dto.setId(91003L);
        dto.setConfigKey("test.notice.enabled");
        dto.setConfigName("通知开关");
        dto.setConfigValue("false");

        service.updateConfig(dto);

        verify(configMapper).updateById(any(SysConfig.class));
        verify(redisTemplate).delete("sys:config:test.notice.enabled");
    }
}
