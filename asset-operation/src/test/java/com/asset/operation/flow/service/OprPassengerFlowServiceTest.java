package com.asset.operation.flow.service;

import com.asset.common.exception.BizException;
import com.asset.operation.flow.dto.PassengerFlowCreateDTO;
import com.asset.operation.flow.entity.OprPassengerFlow;
import com.asset.operation.flow.mapper.OprPassengerFlowMapper;
import com.asset.operation.flow.service.impl.OprPassengerFlowServiceImpl;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 客流填报 Service 单元测试（PF-U）
 * 框架：Mockito，@InjectMocks OprPassengerFlowServiceImpl
 *
 * <p>Mock 列表：
 * <ul>
 *   <li>OprPassengerFlowMapper - 客流 Mapper（同时作为 baseMapper）</li>
 *   <li>JdbcTemplate - 跨模块查询</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@DisplayName("客流填报 Service（PF-U）")
class OprPassengerFlowServiceTest {

    @Mock
    private OprPassengerFlowMapper flowMapper;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private OprPassengerFlowServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        TableInfoHelper.initTableInfo(assistant, OprPassengerFlow.class);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", flowMapper);
    }

    // ── 辅助方法 ──────────────────────────────────────────────────

    private PassengerFlowCreateDTO buildCreateDTO() {
        PassengerFlowCreateDTO dto = new PassengerFlowCreateDTO();
        dto.setProjectId(90001L);
        dto.setBuildingId(90001L);
        dto.setFloorId(null);
        dto.setReportDate(LocalDate.of(2026, 3, 15));
        dto.setFlowCount(1500);
        return dto;
    }

    // ── PF-U-01：新增客流-唯一键校验通过 ─────────────────────────

    @Test
    @DisplayName("PF-U-01: 新增客流-唯一键校验通过")
    void testCreateSuccess() {
        // Mock: 无重复记录（getOne 内部调用 selectOne(wrapper, throwEx)）
        when(flowMapper.selectOne(any(Wrapper.class), anyBoolean())).thenReturn(null);
        // Mock: insert
        when(flowMapper.insert(argThat((OprPassengerFlow f) -> f != null))).thenAnswer(inv -> {
            OprPassengerFlow f = inv.getArgument(0);
            f.setId(1L);
            return 1;
        });

        PassengerFlowCreateDTO dto = buildCreateDTO();
        Long id = service.create(dto);

        assertNotNull(id);
        verify(flowMapper).insert(argThat((OprPassengerFlow f) -> {
            assertEquals(90001L, f.getProjectId());
            assertEquals(90001L, f.getBuildingId());
            assertEquals(LocalDate.of(2026, 3, 15), f.getReportDate());
            assertEquals(1500, f.getFlowCount());
            assertEquals(1, f.getSourceType(), "sourceType 应为 1(手动)");
            return true;
        }));
    }

    // ── PF-U-02：新增客流-重复拒绝 ───────────────────────────────

    @Test
    @DisplayName("PF-U-02: 新增客流-重复拒绝")
    void testCreateDuplicate() {
        // Mock: 已有记录（getOne 内部调用 selectOne(wrapper, throwEx)）
        OprPassengerFlow existing = new OprPassengerFlow();
        existing.setId(99L);
        when(flowMapper.selectOne(any(Wrapper.class), anyBoolean())).thenReturn(existing);

        PassengerFlowCreateDTO dto = buildCreateDTO();

        BizException ex = assertThrows(BizException.class,
                () -> service.create(dto));
        assertTrue(ex.getMessage().contains("重复") || ex.getMessage().contains("已存在"),
                "应提示重复填报");
        verify(flowMapper, never()).insert(any(OprPassengerFlow.class));
    }

    // ── PF-U-03：编辑客流-仅手动可编辑 ───────────────────────────

    @Test
    @DisplayName("PF-U-03: 编辑客流-仅手动可编辑")
    void testUpdateManualSource() {
        OprPassengerFlow existing = new OprPassengerFlow();
        existing.setId(1L);
        existing.setProjectId(90001L);
        existing.setBuildingId(90001L);
        existing.setReportDate(LocalDate.of(2026, 3, 15));
        existing.setFlowCount(1500);
        existing.setSourceType(1); // 手动

        when(flowMapper.selectById(1L)).thenReturn(existing);
        // Mock: checkDuplicate → getOne 返回自身（excludeId 匹配，不算重复）
        when(flowMapper.selectOne(any(Wrapper.class), anyBoolean())).thenReturn(existing);
        when(flowMapper.updateById(argThat((OprPassengerFlow f) -> f != null))).thenReturn(1);

        PassengerFlowCreateDTO dto = buildCreateDTO();
        dto.setFlowCount(2000);

        service.update(1L, dto);

        verify(flowMapper).updateById(argThat((OprPassengerFlow f) -> {
            assertEquals(2000, f.getFlowCount(), "客流应更新为 2000");
            return true;
        }));
    }

    // ── PF-U-04：编辑客流-导入来源不可编辑 ────────────────────────

    @Test
    @DisplayName("PF-U-04: 编辑客流-导入来源不可编辑")
    void testUpdateImportSource() {
        OprPassengerFlow existing = new OprPassengerFlow();
        existing.setId(1L);
        existing.setSourceType(2); // 导入

        when(flowMapper.selectById(1L)).thenReturn(existing);

        PassengerFlowCreateDTO dto = buildCreateDTO();

        BizException ex = assertThrows(BizException.class,
                () -> service.update(1L, dto));
        assertTrue(ex.getMessage().contains("手动"), "应提示仅手动录入可编辑");
        verify(flowMapper, never()).updateById(any(OprPassengerFlow.class));
    }
}
