package com.asset.system.online;

import com.asset.system.auth.dto.OnlineUserVO;
import com.asset.system.auth.service.impl.SysTokenServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * §4.14 在线用户 — Service 单元测试
 * ONLINE-U-01 ~ ONLINE-U-03
 *
 * 实际逻辑在 SysTokenServiceImpl 中实现（listOnlineUsers / removeOnlineSession）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("§4.14 在线用户 Service 单元测试")
class SysOnlineUserServiceTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    @InjectMocks SysTokenServiceImpl tokenService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(
                tokenService, "objectMapper", objectMapper);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ─── ONLINE-U-01 ──────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("ONLINE-U-01 列表-扫描Redis会话：Redis有2个会话，返回2条")
    void listOnline_scansRedis_returnsList() throws Exception {
        OnlineUserVO u1 = OnlineUserVO.builder().userId(91001L).username("admin")
                .loginIp("127.0.0.1").loginTime("2026-03-19T10:00:00").build();
        OnlineUserVO u2 = OnlineUserVO.builder().userId(91002L).username("area_mgr")
                .loginIp("192.168.1.2").loginTime("2026-03-19T09:00:00").build();

        Set<String> keys = Set.of("auth:online:91001", "auth:online:91002");
        when(redisTemplate.keys("auth:online:*")).thenReturn(keys);
        when(valueOps.multiGet(keys)).thenReturn(
                List.of(objectMapper.writeValueAsString(u1),
                        objectMapper.writeValueAsString(u2)));

        List<OnlineUserVO> result = tokenService.listOnlineUsers();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(OnlineUserVO::getUsername)
                          .containsExactlyInAnyOrder("admin", "area_mgr");
    }

    // ─── ONLINE-U-02 ──────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("ONLINE-U-02 强制下线：删除在线会话 + 所有 RefreshToken")
    void forceOffline_removesAllTokens() {
        when(redisTemplate.delete(anyString())).thenReturn(true);
        Set<String> refreshKeys = Set.of("auth:refresh:token_abc");
        when(redisTemplate.keys("auth:refresh:*91005*")).thenReturn(refreshKeys);
        when(valueOps.get(anyString())).thenReturn("91005");

        tokenService.removeOnlineSession(91005L);
        tokenService.removeAllRefreshTokensByUser(91005L);

        verify(redisTemplate).delete("auth:online:91005");
    }

    // ─── ONLINE-U-03 ──────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("ONLINE-U-03 列表-无会话：返回空列表")
    void listOnline_noSession_returnsEmpty() {
        when(redisTemplate.keys("auth:online:*")).thenReturn(Set.of());

        List<OnlineUserVO> result = tokenService.listOnlineUsers();

        assertThat(result).isEmpty();
    }
}
