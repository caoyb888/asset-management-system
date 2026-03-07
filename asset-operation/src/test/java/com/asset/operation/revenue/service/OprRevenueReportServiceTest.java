package com.asset.operation.revenue.service;

import com.asset.common.exception.BizException;
import com.asset.operation.engine.FloatingRentCalculator;
import com.asset.operation.revenue.dto.GenerateFloatingRentDTO;
import com.asset.operation.revenue.dto.RevenueReportCreateDTO;
import com.asset.operation.revenue.entity.OprRevenueReport;
import com.asset.operation.revenue.mapper.OprRevenueReportMapper;
import com.asset.operation.revenue.service.impl.OprRevenueReportServiceImpl;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 营收填报 Service 单元测试（REV-U）
 * 框架：Mockito，@InjectMocks OprRevenueReportServiceImpl
 *
 * <p>Mock 列表：
 * <ul>
 *   <li>OprRevenueReportMapper - 营收 Mapper（同时作为 baseMapper）</li>
 *   <li>FloatingRentCalculator - 浮动租金计算引擎</li>
 *   <li>JdbcTemplate - 跨模块查询</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("营收填报 Service（REV-U）")
class OprRevenueReportServiceTest {

    @Mock
    private OprRevenueReportMapper revenueMapper;

    @Mock
    private FloatingRentCalculator floatingRentCalculator;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private OprRevenueReportServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        TableInfoHelper.initTableInfo(assistant, OprRevenueReport.class);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", revenueMapper);
    }

    // ── 辅助方法 ──────────────────────────────────────────────────

    private RevenueReportCreateDTO buildCreateDTO() {
        RevenueReportCreateDTO dto = new RevenueReportCreateDTO();
        dto.setContractId(91003L);
        dto.setReportDate(LocalDate.of(2026, 3, 15));
        dto.setRevenueAmount(new BigDecimal("88000.00"));
        return dto;
    }

    // ── REV-U-01：新增营收-唯一性校验通过 ─────────────────────────

    @Test
    @DisplayName("REV-U-01: 新增营收-唯一性校验通过")
    void testSaveReportSuccess() {
        // Mock: 同合同同日无记录
        when(revenueMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        // Mock: 补全合同信息
        when(jdbcTemplate.queryForMap(contains("inv_lease_contract"), eq(91003L)))
                .thenReturn(Map.of("project_id", 90001L, "merchant_id", 90002L, "shop_id", 91001L));
        // Mock: insert
        when(revenueMapper.insert(argThat((OprRevenueReport r) -> r != null))).thenAnswer(inv -> {
            OprRevenueReport r = inv.getArgument(0);
            r.setId(1L);
            return 1;
        });

        RevenueReportCreateDTO dto = buildCreateDTO();
        OprRevenueReport result = service.saveReport(dto);

        assertNotNull(result);
        verify(revenueMapper).insert(argThat((OprRevenueReport r) -> {
            assertEquals(91003L, r.getContractId());
            assertEquals(0, r.getStatus(), "status 应为 0(待确认)");
            assertEquals("2026-03", r.getReportMonth(), "reportMonth 应自动计算");
            assertEquals(0, new BigDecimal("88000.00").compareTo(r.getRevenueAmount()));
            assertEquals(LocalDate.of(2026, 3, 15), r.getReportDate());
            return true;
        }));
    }

    // ── REV-U-02：新增营收-重复日期拒绝 ───────────────────────────

    @Test
    @DisplayName("REV-U-02: 新增营收-重复日期拒绝")
    void testSaveReportDuplicate() {
        // Mock: 同合同同日已有记录
        when(revenueMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        RevenueReportCreateDTO dto = buildCreateDTO();

        BizException ex = assertThrows(BizException.class,
                () -> service.saveReport(dto));
        assertTrue(ex.getMessage().contains("重复"), "应提示重复录入");
        verify(revenueMapper, never()).insert(any(OprRevenueReport.class));
    }

    // ── REV-U-03：编辑营收-待确认可编辑 ───────────────────────────

    @Test
    @DisplayName("REV-U-03: 编辑营收-待确认可编辑")
    void testUpdateReportPending() {
        OprRevenueReport exist = new OprRevenueReport();
        exist.setId(1L);
        exist.setContractId(91003L);
        exist.setReportDate(LocalDate.of(2026, 3, 15));
        exist.setRevenueAmount(new BigDecimal("88000.00"));
        exist.setStatus(0); // 待确认

        when(revenueMapper.selectById(1L)).thenReturn(exist);
        when(revenueMapper.updateById(argThat((OprRevenueReport r) -> r != null))).thenReturn(1);

        RevenueReportCreateDTO dto = buildCreateDTO();
        dto.setRevenueAmount(new BigDecimal("95000.00"));

        service.updateReport(1L, dto);

        verify(revenueMapper).updateById(argThat((OprRevenueReport r) -> {
            assertEquals(0, new BigDecimal("95000.00").compareTo(r.getRevenueAmount()),
                    "金额应更新为 95000");
            return true;
        }));
    }

    // ── REV-U-04：编辑营收-已确认不可编辑 ─────────────────────────

    @Test
    @DisplayName("REV-U-04: 编辑营收-已确认不可编辑")
    void testUpdateReportConfirmed() {
        OprRevenueReport exist = new OprRevenueReport();
        exist.setId(1L);
        exist.setStatus(1); // 已确认

        when(revenueMapper.selectById(1L)).thenReturn(exist);

        RevenueReportCreateDTO dto = buildCreateDTO();

        BizException ex = assertThrows(BizException.class,
                () -> service.updateReport(1L, dto));
        assertTrue(ex.getMessage().contains("待确认"), "应提示仅待确认可修改");
        verify(revenueMapper, never()).updateById(any(OprRevenueReport.class));
    }

    // ── REV-U-05：触发浮动租金-委托引擎 ──────────────────────────

    @Test
    @DisplayName("REV-U-05: 触发浮动租金-委托引擎")
    void testGenerateFloatingRent() {
        when(floatingRentCalculator.calculate(91003L, "2026-03")).thenReturn(1001L);

        GenerateFloatingRentDTO dto = new GenerateFloatingRentDTO();
        dto.setContractId(91003L);
        dto.setCalcMonth("2026-03");

        Long floatingRentId = service.generateFloatingRent(dto);

        assertEquals(1001L, floatingRentId);
        verify(floatingRentCalculator).calculate(91003L, "2026-03");
    }
}
