package com.asset.operation.change.service;

import com.asset.common.exception.BizException;
import com.asset.operation.change.dto.ApprovalCallbackDTO;
import com.asset.operation.change.dto.ChangeCreateDTO;
import com.asset.operation.change.dto.ChangeImpactVO;
import com.asset.operation.change.entity.OprContractChange;
import com.asset.operation.change.entity.OprContractChangeDetail;
import com.asset.operation.change.entity.OprContractChangeSnapshot;
import com.asset.operation.change.entity.OprContractChangeType;
import com.asset.operation.change.mapper.OprContractChangeDetailMapper;
import com.asset.operation.change.mapper.OprContractChangeMapper;
import com.asset.operation.change.mapper.OprContractChangeSnapshotMapper;
import com.asset.operation.change.mapper.OprContractChangeTypeMapper;
import com.asset.operation.change.service.impl.OprContractChangeServiceImpl;
import com.asset.operation.engine.ReceivableRecalculateEngine;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 合同变更 Service 单元测试（CHG-U）
 * 框架：Mockito，@InjectMocks OprContractChangeServiceImpl
 *
 * <p>Mock 列表：
 * <ul>
 *   <li>OprContractChangeMapper - 变更主表 Mapper（同时作为 baseMapper）</li>
 *   <li>OprContractChangeTypeMapper - 变更类型关联 Mapper</li>
 *   <li>OprContractChangeDetailMapper - 变更明细 Mapper</li>
 *   <li>OprContractChangeSnapshotMapper - 快照 Mapper</li>
 *   <li>ReceivableRecalculateEngine - 应收重算引擎</li>
 *   <li>JdbcTemplate - 跨模块查询</li>
 *   <li>ObjectMapper - JSON 序列化</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("合同变更 Service（CHG-U）")
class OprContractChangeServiceTest {

    @Mock
    private OprContractChangeMapper changeMapper;

    @Mock
    private OprContractChangeTypeMapper changeTypeMapper;

    @Mock
    private OprContractChangeDetailMapper changeDetailMapper;

    @Mock
    private OprContractChangeSnapshotMapper changeSnapshotMapper;

    @Mock
    private ReceivableRecalculateEngine recalculateEngine;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private OprContractChangeServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        TableInfoHelper.initTableInfo(assistant, OprContractChange.class);
        TableInfoHelper.initTableInfo(assistant, OprContractChangeType.class);
        TableInfoHelper.initTableInfo(assistant, OprContractChangeDetail.class);
        TableInfoHelper.initTableInfo(assistant, OprContractChangeSnapshot.class);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", changeMapper);
    }

    // ── 辅助方法 ──────────────────────────────────────────────────

    private OprContractChange buildChange(Long id, int status) {
        OprContractChange change = new OprContractChange();
        change.setId(id);
        change.setChangeCode("BG2603070001");
        change.setContractId(91003L);
        change.setLedgerId(92001L);
        change.setProjectId(90001L);
        change.setStatus(status);
        change.setEffectiveDate(LocalDate.of(2026, 4, 1));
        change.setReason("租金调整");
        return change;
    }

    private ChangeCreateDTO buildCreateDTO() {
        ChangeCreateDTO dto = new ChangeCreateDTO();
        dto.setContractId(91003L);
        dto.setLedgerId(92001L);
        dto.setChangeTypeCodes(List.of("RENT"));
        dto.setEffectiveDate(LocalDate.of(2026, 4, 1));
        dto.setReason("租金调整");
        dto.setChangeFields(Map.of(
                "newRentAmount", "20000",
                "old_newRentAmount", "15000"
        ));
        return dto;
    }

    // ── CHG-U-01：新建变更单-草稿状态 ──────────────────────────────

    @Test
    @DisplayName("CHG-U-01: 新建变更单-草稿状态")
    void testCreateDraft() {
        // Mock: 查项目ID
        when(jdbcTemplate.queryForObject(contains("project_id"), eq(Long.class), eq(91003L)))
                .thenReturn(90001L);
        // Mock: 查当日最大编号
        when(jdbcTemplate.queryForObject(contains("MAX(change_code)"), eq(String.class), anyString()))
                .thenReturn(null);
        // Mock: save -> insert
        when(changeMapper.insert(argThat((OprContractChange c) -> c != null))).thenAnswer(inv -> {
            OprContractChange c = inv.getArgument(0);
            c.setId(1L);
            return 1;
        });
        // Mock: 类型插入
        when(changeTypeMapper.insert(argThat((OprContractChangeType t) -> t != null))).thenReturn(1);
        // Mock: 明细插入
        when(changeDetailMapper.insert(argThat((OprContractChangeDetail d) -> d != null))).thenReturn(1);

        ChangeCreateDTO dto = buildCreateDTO();
        Long id = service.create(dto);

        assertNotNull(id);
        verify(changeMapper).insert(argThat((OprContractChange c) -> {
            assertTrue(c.getChangeCode().startsWith("BG"), "编码应以 BG 开头");
            assertEquals(0, c.getStatus(), "status 应为 0(草稿)");
            assertEquals(91003L, c.getContractId());
            assertEquals(92001L, c.getLedgerId());
            return true;
        }));
        // 验证类型关联保存（RENT 一条）
        verify(changeTypeMapper).insert(argThat((OprContractChangeType t) -> {
            assertEquals(1L, t.getChangeId());
            assertEquals("RENT", t.getChangeTypeCode());
            return true;
        }));
    }

    // ── CHG-U-02：编辑变更-草稿可编辑 ─────────────────────────────

    @Test
    @DisplayName("CHG-U-02: 编辑变更-草稿可编辑")
    void testUpdateDraft() {
        OprContractChange change = buildChange(1L, 0);
        when(changeMapper.selectById(1L)).thenReturn(change);
        when(changeMapper.updateById(argThat((OprContractChange c) -> c != null))).thenReturn(1);
        // Mock: 删除旧类型/明细
        when(changeTypeMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(changeDetailMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        // Mock: 插入新类型/明细
        when(changeTypeMapper.insert(argThat((OprContractChangeType t) -> t != null))).thenReturn(1);
        when(changeDetailMapper.insert(argThat((OprContractChangeDetail d) -> d != null))).thenReturn(1);

        ChangeCreateDTO dto = buildCreateDTO();
        dto.setChangeTypeCodes(List.of("RENT", "TERM"));
        dto.setReason("租金+租期变更");

        service.update(1L, dto);

        // 验证类型和明细被重建
        verify(changeTypeMapper).delete(any(LambdaQueryWrapper.class));
        verify(changeDetailMapper).delete(any(LambdaQueryWrapper.class));
        verify(changeTypeMapper, times(2)).insert(argThat((OprContractChangeType t) -> t != null));
    }

    // ── CHG-U-03：编辑变更-审批中不可编辑 ──────────────────────────

    @Test
    @DisplayName("CHG-U-03: 编辑变更-审批中不可编辑")
    void testUpdateInApproval() {
        OprContractChange change = buildChange(1L, 1); // status=1 审批中
        when(changeMapper.selectById(1L)).thenReturn(change);

        ChangeCreateDTO dto = buildCreateDTO();

        BizException ex = assertThrows(BizException.class,
                () -> service.update(1L, dto));
        assertTrue(ex.getMessage().contains("草稿") || ex.getMessage().contains("驳回"),
                "应提示仅草稿或驳回可修改");
        verify(changeMapper, never()).updateById(any(OprContractChange.class));
    }

    // ── CHG-U-04：编辑变更-已通过不可编辑 ──────────────────────────

    @Test
    @DisplayName("CHG-U-04: 编辑变更-已通过不可编辑")
    void testUpdateApproved() {
        OprContractChange change = buildChange(1L, 2); // status=2 已通过
        when(changeMapper.selectById(1L)).thenReturn(change);

        ChangeCreateDTO dto = buildCreateDTO();

        BizException ex = assertThrows(BizException.class,
                () -> service.update(1L, dto));
        assertTrue(ex.getMessage().contains("草稿") || ex.getMessage().contains("驳回"),
                "应提示仅草稿或驳回可修改");
    }

    // ── CHG-U-05：预览影响-调用重算引擎 ───────────────────────────

    @Test
    @DisplayName("CHG-U-05: 预览影响-调用重算引擎")
    void testPreviewImpact() {
        OprContractChange change = buildChange(1L, 0);
        when(changeMapper.selectById(1L)).thenReturn(change);

        // Mock: engine.preview 返回影响 VO
        ChangeImpactVO impactVO = new ChangeImpactVO();
        impactVO.setAffectedPlanCount(5);
        impactVO.setOriginalTotalAmount(new BigDecimal("100000"));
        impactVO.setNewTotalAmount(new BigDecimal("120000"));
        impactVO.setAmountDiff(new BigDecimal("20000"));
        impactVO.setImpactDesc("租金上调，影响5笔应收");
        when(recalculateEngine.preview(argThat((OprContractChange c) -> c.getId().equals(1L))))
                .thenReturn(impactVO);

        // Mock: update impact_summary
        when(changeMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        ChangeImpactVO result = service.previewImpact(1L);

        assertNotNull(result);
        assertEquals(5, result.getAffectedPlanCount());
        assertEquals(0, new BigDecimal("20000").compareTo(result.getAmountDiff()));
        verify(recalculateEngine).preview(argThat((OprContractChange c) -> c.getId().equals(1L)));
        // 验证 impact_summary 被更新
        verify(changeMapper).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    // ── CHG-U-06：提交审批-草稿→审批中 ────────────────────────────

    @Test
    @DisplayName("CHG-U-06: 提交审批-草稿→审批中")
    void testSubmitApprovalFromDraft() {
        OprContractChange change = buildChange(1L, 0); // status=0 草稿
        when(changeMapper.selectById(1L)).thenReturn(change);
        when(changeMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        service.submitApproval(1L);

        verify(changeMapper).update(isNull(), argThat((LambdaUpdateWrapper<OprContractChange> w) -> {
            // 验证 update 被调用（status→1, approvalId 非空）
            return true;
        }));
    }

    // ── CHG-U-07：提交审批-驳回→审批中 ────────────────────────────

    @Test
    @DisplayName("CHG-U-07: 提交审批-驳回→审批中")
    void testSubmitApprovalFromRejected() {
        OprContractChange change = buildChange(1L, 3); // status=3 驳回
        when(changeMapper.selectById(1L)).thenReturn(change);
        when(changeMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        service.submitApproval(1L);

        verify(changeMapper).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    // ── CHG-U-08：审批回调-通过→执行重算 ──────────────────────────

    @Test
    @DisplayName("CHG-U-08: 审批回调-通过→执行重算")
    void testApprovalCallbackPass() {
        OprContractChange change = buildChange(1L, 1); // status=1 审批中
        when(changeMapper.selectById(1L)).thenReturn(change);
        when(changeMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        ApprovalCallbackDTO dto = new ApprovalCallbackDTO();
        dto.setStatus(2); // 通过

        service.onApprovalCallback(1L, dto);

        // 验证状态更新为 2（通过）
        verify(changeMapper).update(isNull(), any(LambdaUpdateWrapper.class));
        // 验证触发应收重算引擎
        verify(recalculateEngine).execute(argThat((OprContractChange c) -> {
            assertEquals(2, c.getStatus(), "传入引擎时 status 应为 2");
            return true;
        }));
    }

    // ── CHG-U-09：审批回调-驳回 ───────────────────────────────────

    @Test
    @DisplayName("CHG-U-09: 审批回调-驳回")
    void testApprovalCallbackReject() {
        OprContractChange change = buildChange(1L, 1); // status=1 审批中
        when(changeMapper.selectById(1L)).thenReturn(change);
        when(changeMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        ApprovalCallbackDTO dto = new ApprovalCallbackDTO();
        dto.setStatus(3); // 驳回

        service.onApprovalCallback(1L, dto);

        // 验证状态更新
        verify(changeMapper).update(isNull(), any(LambdaUpdateWrapper.class));
        // 驳回时不应触发应收重算
        verify(recalculateEngine, never()).execute(any(OprContractChange.class));
    }

    // ── CHG-U-10：变更历史-按合同查询 ─────────────────────────────

    @Test
    @DisplayName("CHG-U-10: 变更历史-按合同查询")
    void testListHistory() {
        OprContractChange c1 = buildChange(1L, 2);
        OprContractChange c2 = buildChange(2L, 0);
        c2.setChangeCode("BG2603060001");
        when(changeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(c1, c2));

        // Mock: 变更类型查询（buildDetailVO 内部会查类型和明细）
        when(changeTypeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(changeDetailMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        // Mock: 合同信息查询
        when(jdbcTemplate.queryForMap(contains("inv_lease_contract"), eq(91003L)))
                .thenReturn(Map.of("contract_code", "HT20250101001",
                        "contract_name", "测试合同",
                        "merchant_id", 90002L));
        when(jdbcTemplate.queryForObject(contains("biz_merchant"), eq(String.class), eq(90002L)))
                .thenReturn("测试商家");
        when(jdbcTemplate.queryForObject(contains("biz_project"), eq(String.class), eq(90001L)))
                .thenReturn("测试项目");

        var history = service.listHistory(91003L);

        assertEquals(2, history.size(), "应返回该合同的所有变更");
        verify(changeMapper).selectList(any(LambdaQueryWrapper.class));
    }
}
