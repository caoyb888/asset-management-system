package com.asset.operation.termination.service;

import com.asset.common.exception.BizException;
import com.asset.operation.change.dto.ApprovalCallbackDTO;
import com.asset.operation.engine.TerminationSettlementEngine;
import com.asset.operation.termination.dto.TerminationCreateDTO;
import com.asset.operation.termination.entity.OprContractTermination;
import com.asset.operation.termination.entity.OprTerminationSettlement;
import com.asset.operation.termination.mapper.OprContractTerminationMapper;
import com.asset.operation.termination.mapper.OprTerminationSettlementMapper;
import com.asset.operation.termination.service.impl.OprContractTerminationServiceImpl;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
 * 合同解约 Service 单元测试（TRM-U）
 * 框架：Mockito，@InjectMocks OprContractTerminationServiceImpl
 *
 * <p>Mock 列表：
 * <ul>
 *   <li>OprContractTerminationMapper - 解约主表 Mapper（同时作为 baseMapper）</li>
 *   <li>OprTerminationSettlementMapper - 清算明细 Mapper</li>
 *   <li>TerminationSettlementEngine - 解约清算引擎</li>
 *   <li>JdbcTemplate - 跨模块查询</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("合同解约 Service（TRM-U）")
class OprContractTerminationServiceTest {

    @Mock
    private OprContractTerminationMapper terminationMapper;

    @Mock
    private OprTerminationSettlementMapper settlementMapper;

    @Mock
    private TerminationSettlementEngine settlementEngine;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private OprContractTerminationServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        TableInfoHelper.initTableInfo(assistant, OprContractTermination.class);
        TableInfoHelper.initTableInfo(assistant, OprTerminationSettlement.class);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", terminationMapper);
    }

    // ── 辅助方法 ──────────────────────────────────────────────────

    private OprContractTermination buildTermination(Long id, int status) {
        OprContractTermination t = new OprContractTermination();
        t.setId(id);
        t.setTerminationCode("JY2603070001");
        t.setContractId(91003L);
        t.setLedgerId(92001L);
        t.setProjectId(90001L);
        t.setMerchantId(90002L);
        t.setTerminationType(2); // 提前解约
        t.setTerminationDate(LocalDate.of(2026, 6, 30));
        t.setReason("经营困难");
        t.setStatus(status);
        return t;
    }

    private TerminationCreateDTO buildCreateDTO() {
        TerminationCreateDTO dto = new TerminationCreateDTO();
        dto.setContractId(91003L);
        dto.setTerminationType(2); // 提前解约
        dto.setTerminationDate(LocalDate.of(2026, 6, 30));
        dto.setReason("经营困难");
        return dto;
    }

    // ── TRM-U-01：新建解约单-草稿状态 ─────────────────────────────

    @Test
    @DisplayName("TRM-U-01: 新建解约单-草稿状态")
    void testCreateDraft() {
        // Mock: 无进行中解约
        when(terminationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        // Mock: 查台账ID
        when(jdbcTemplate.queryForObject(contains("opr_contract_ledger"), eq(Long.class), eq(91003L)))
                .thenReturn(92001L);
        // Mock: 查合同信息
        when(jdbcTemplate.queryForMap(contains("inv_lease_contract"), eq(91003L)))
                .thenReturn(Map.of("project_id", 90001L, "merchant_id", 90002L, "brand_id", 90001L));
        // Mock: 查商铺
        when(jdbcTemplate.queryForObject(contains("inv_lease_contract_shop"), eq(Long.class), eq(91003L)))
                .thenReturn(91001L);
        // Mock: 查当日最大编号
        when(jdbcTemplate.queryForObject(contains("MAX(termination_code)"), eq(String.class), anyString()))
                .thenReturn(null);
        // Mock: insert
        when(terminationMapper.insert(argThat((OprContractTermination t) -> t != null))).thenAnswer(inv -> {
            OprContractTermination t = inv.getArgument(0);
            t.setId(1L);
            return 1;
        });

        TerminationCreateDTO dto = buildCreateDTO();
        Long id = service.create(dto);

        assertNotNull(id);
        verify(terminationMapper).insert(argThat((OprContractTermination t) -> {
            assertTrue(t.getTerminationCode().startsWith("JY"), "编码应以 JY 开头");
            assertEquals(0, t.getStatus(), "status 应为 0(草稿)");
            assertEquals(91003L, t.getContractId());
            assertEquals(92001L, t.getLedgerId());
            assertEquals(90001L, t.getProjectId());
            return true;
        }));
    }

    // ── TRM-U-02：新建解约-同合同已有草稿拒绝 ────────────────────

    @Test
    @DisplayName("TRM-U-02: 新建解约-同合同已有草稿拒绝")
    void testCreateDuplicateDraft() {
        // Mock: 已有进行中解约
        when(terminationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        TerminationCreateDTO dto = buildCreateDTO();

        BizException ex = assertThrows(BizException.class,
                () -> service.create(dto));
        assertTrue(ex.getMessage().contains("已有") || ex.getMessage().contains("进行中"),
                "应提示已有进行中的解约");
        verify(terminationMapper, never()).insert(any(OprContractTermination.class));
    }

    // ── TRM-U-03：编辑解约-草稿可编辑 ─────────────────────────────

    @Test
    @DisplayName("TRM-U-03: 编辑解约-草稿可编辑")
    void testUpdateDraft() {
        OprContractTermination t = buildTermination(1L, 0);
        when(terminationMapper.selectById(1L)).thenReturn(t);
        when(terminationMapper.updateById(argThat((OprContractTermination x) -> x != null))).thenReturn(1);
        when(settlementMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(0);

        TerminationCreateDTO dto = buildCreateDTO();
        dto.setReason("更新原因");

        service.update(1L, dto);

        // 验证清算金额被清空
        verify(terminationMapper).updateById(argThat((OprContractTermination x) -> {
            assertNull(x.getSettlementAmount(), "清算金额应被清空");
            assertNull(x.getUnsettledAmount(), "未结算金额应被清空");
            return true;
        }));
        // 验证旧清算明细被删除
        verify(settlementMapper).delete(any(LambdaQueryWrapper.class));
    }

    // ── TRM-U-04：编辑解约-审批中不可编辑 ─────────────────────────

    @Test
    @DisplayName("TRM-U-04: 编辑解约-审批中不可编辑")
    void testUpdateInApproval() {
        OprContractTermination t = buildTermination(1L, 1); // 审批中
        when(terminationMapper.selectById(1L)).thenReturn(t);

        TerminationCreateDTO dto = buildCreateDTO();

        BizException ex = assertThrows(BizException.class,
                () -> service.update(1L, dto));
        assertTrue(ex.getMessage().contains("草稿") || ex.getMessage().contains("驳回"),
                "应提示仅草稿或驳回可修改");
        verify(terminationMapper, never()).updateById(any(OprContractTermination.class));
    }

    // ── TRM-U-05：计算清算-委托引擎 ──────────────────────────────

    @Test
    @DisplayName("TRM-U-05: 计算清算-委托引擎")
    void testCalculateSettlement() {
        OprContractTermination t = buildTermination(1L, 0);
        when(terminationMapper.selectById(1L)).thenReturn(t);

        service.calculateSettlement(1L);

        verify(settlementEngine).calculateSettlement(1L);
    }

    // ── TRM-U-06：提交审批-须先计算清算 ──────────────────────────

    @Test
    @DisplayName("TRM-U-06: 提交审批-须先计算清算")
    void testSubmitWithoutSettlement() {
        OprContractTermination t = buildTermination(1L, 0);
        t.setSettlementAmount(null); // 未计算清算
        when(terminationMapper.selectById(1L)).thenReturn(t);

        BizException ex = assertThrows(BizException.class,
                () -> service.submitApproval(1L));
        assertTrue(ex.getMessage().contains("清算"), "应提示请先计算清算");
    }

    // ── TRM-U-07：提交审批-草稿→审批中 ──────────────────────────

    @Test
    @DisplayName("TRM-U-07: 提交审批-草稿→审批中")
    void testSubmitApproval() {
        OprContractTermination t = buildTermination(1L, 0);
        t.setSettlementAmount(new BigDecimal("25000"));
        when(terminationMapper.selectById(1L)).thenReturn(t);
        when(terminationMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        service.submitApproval(1L);

        verify(terminationMapper).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    // ── TRM-U-08：审批回调-通过→执行解约 ─────────────────────────

    @Test
    @DisplayName("TRM-U-08: 审批回调-通过→执行解约")
    void testApprovalCallbackPass() {
        OprContractTermination t = buildTermination(1L, 1); // 审批中
        when(terminationMapper.selectById(1L)).thenReturn(t);

        ApprovalCallbackDTO dto = new ApprovalCallbackDTO();
        dto.setStatus(2); // 通过

        service.onApprovalCallback(1L, dto);

        // 验证引擎执行
        verify(settlementEngine).execute(1L);
        // 驳回更新不应被调用
        verify(terminationMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    // ── TRM-U-09：审批回调-驳回 ──────────────────────────────────

    @Test
    @DisplayName("TRM-U-09: 审批回调-驳回")
    void testApprovalCallbackReject() {
        OprContractTermination t = buildTermination(1L, 1); // 审批中
        when(terminationMapper.selectById(1L)).thenReturn(t);
        when(terminationMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        ApprovalCallbackDTO dto = new ApprovalCallbackDTO();
        dto.setStatus(3); // 驳回

        service.onApprovalCallback(1L, dto);

        // 验证状态更新为驳回
        verify(terminationMapper).update(isNull(), any(LambdaUpdateWrapper.class));
        // 引擎不应被调用
        verify(settlementEngine, never()).execute(anyLong());
    }
}
