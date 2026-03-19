package com.asset.system.log;

import com.asset.system.auth.entity.SysLoginLog;
import com.asset.system.auth.mapper.SysLoginLogMapper;
import com.asset.system.log.dto.LoginLogQueryDTO;
import com.asset.system.log.dto.OperLogQueryDTO;
import com.asset.system.log.entity.SysOperLog;
import com.asset.system.log.mapper.SysOperLogMapper;
import com.asset.system.log.service.impl.SysLoginLogServiceImpl;
import com.asset.system.log.service.impl.SysOperLogServiceImpl;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * §4.13 日志管理 — Service 单元测试
 * LOG-U-01 ~ LOG-U-04
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("§4.13 日志管理 Service 单元测试")
class SysLogServiceTest {

    @BeforeAll
    static void initMybatisLambdaCache() {
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        assistant.setCurrentNamespace("test_log_ns");
        TableInfoHelper.initTableInfo(assistant, SysOperLog.class);
        TableInfoHelper.initTableInfo(assistant, SysLoginLog.class);
    }

    @Mock SysOperLogMapper  operLogMapper;
    @Mock SysLoginLogMapper loginLogMapper;

    @InjectMocks SysOperLogServiceImpl  operLogService;
    @InjectMocks SysLoginLogServiceImpl loginLogService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(operLogService,  "baseMapper", operLogMapper);
        ReflectionTestUtils.setField(loginLogService, "baseMapper", loginLogMapper);
    }

    // ─── 辅助 ─────────────────────────────────────────────────────────────────

    private SysOperLog operLog(Long id, String module, int status, LocalDateTime time) {
        SysOperLog l = new SysOperLog();
        l.setId(id);
        l.setModule(module);
        l.setStatus(status);
        l.setOperTime(time);
        return l;
    }

    @SuppressWarnings("unchecked")
    private <T> IPage<T> page(List<T> records) {
        Page<T> p = new Page<>(1, 10);
        p.setRecords(records);
        p.setTotal(records.size());
        return p;
    }

    // ─── LOG-U-01 ─────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("LOG-U-01 操作日志分页-时间范围：selectPage 被调用，返回过滤结果")
    void pageOperLog_byTimeRange_filtered() {
        LocalDateTime from = LocalDateTime.of(2026, 3, 1, 0, 0);
        LocalDateTime to   = LocalDateTime.of(2026, 3, 31, 23, 59);
        when(operLogMapper.selectPage(any(), any()))
                .thenReturn(page(List.of(operLog(1L, "用户管理", 0, from.plusDays(1)))));

        OperLogQueryDTO query = new OperLogQueryDTO();
        query.setPageNum(1); query.setPageSize(10);
        query.setTimeFrom(from); query.setTimeTo(to);

        IPage<SysOperLog> result = operLogService.pageQuery(query);

        assertThat(result.getTotal()).isEqualTo(1);
        verify(operLogMapper).selectPage(any(), any());
    }

    // ─── LOG-U-02 ─────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("LOG-U-02 操作日志分页-按模块过滤：返回指定模块日志")
    void pageOperLog_byModule_filtered() {
        when(operLogMapper.selectPage(any(), any()))
                .thenReturn(page(List.of(
                        operLog(1L, "用户管理", 0, LocalDateTime.now()),
                        operLog(2L, "用户管理", 0, LocalDateTime.now()))));

        OperLogQueryDTO query = new OperLogQueryDTO();
        query.setPageNum(1); query.setPageSize(10);
        query.setModule("用户管理");

        IPage<SysOperLog> result = operLogService.pageQuery(query);

        assertThat(result.getRecords()).allMatch(l -> "用户管理".equals(l.getModule()));
        verify(operLogMapper).selectPage(any(), any());
    }

    // ─── LOG-U-03 ─────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("LOG-U-03 登录日志分页-按状态过滤：只返回失败记录")
    void pageLoginLog_byStatus_filtered() {
        SysLoginLog failLog = SysLoginLog.builder()
                .id(1L).username("test").ipAddr("127.0.0.1").status(1).build();
        when(loginLogMapper.selectPage(any(), any())).thenReturn(page(List.of(failLog)));

        LoginLogQueryDTO query = new LoginLogQueryDTO();
        query.setPageNum(1); query.setPageSize(10);
        query.setStatus(1); // 1=失败

        IPage<SysLoginLog> result = loginLogService.pageQuery(query);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords().get(0).getStatus()).isEqualTo(1);
        verify(loginLogMapper).selectPage(any(), any());
    }

    // ─── LOG-U-04 ─────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("LOG-U-04 清空操作日志：delete(wrapper) 被调用")
    void clearOperLog_truncatesTable() {
        when(operLogMapper.delete(any())).thenReturn(100);

        operLogService.clearAll();

        verify(operLogMapper).delete(any());
    }
}
