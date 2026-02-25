package com.asset.investment.intention;

import com.asset.common.exception.BizException;
import com.asset.investment.common.enums.IntentionStatus;
import com.asset.investment.engine.BillingGenerator;
import com.asset.investment.engine.RentCalculateStrategyRouter;
import com.asset.investment.intention.dto.ApprovalCallbackDTO;
import com.asset.investment.intention.dto.IntentionSaveDTO;
import com.asset.investment.intention.entity.InvIntention;
import com.asset.investment.intention.mapper.InvIntentionMapper;
import com.asset.investment.intention.service.InvIntentionBillingService;
import com.asset.investment.intention.service.InvIntentionFeeService;
import com.asset.investment.intention.service.InvIntentionFeeStageService;
import com.asset.investment.intention.service.InvIntentionShopService;
import com.asset.investment.intention.service.impl.InvIntentionServiceImpl;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 意向协议状态机单元测试（任务9.2）
 * 覆盖：编辑/删除/提交/审批回调的状态前置校验逻辑
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("意向协议状态机测试")
class InvIntentionStateMachineTest {

    @Mock private InvIntentionMapper intentionMapper;
    @Mock private InvIntentionShopService intentionShopService;
    @Mock private InvIntentionFeeService intentionFeeService;
    @Mock private InvIntentionFeeStageService intentionFeeStageService;
    @Mock private InvIntentionBillingService intentionBillingService;
    @Mock private BillingGenerator billingGenerator;
    @Mock private RentCalculateStrategyRouter strategyRouter;

    private InvIntentionServiceImpl intentionService;

    @BeforeAll
    static void initMybatisPlusCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, InvIntention.class);
    }

    @BeforeEach
    void setUp() throws Exception {
        intentionService = new InvIntentionServiceImpl(
                intentionShopService, intentionFeeService, intentionFeeStageService,
                intentionBillingService, billingGenerator, strategyRouter);

        var baseMapperField = CrudRepository.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(intentionService, intentionMapper);
    }

    // ─── 编辑校验：checkEditable ────────────────────────────────

    @ParameterizedTest(name = "状态[{0}]不可编辑")
    @ValueSource(ints = {1, 2, 4})   // 审批中/通过/已转合同
    @DisplayName("updateIntention：非可编辑状态（审批中/通过/已转合同）应抛出 BizException")
    void updateIntention_nonEditableStatus_throws(int status) {
        when(intentionMapper.selectById(1L)).thenReturn(buildIntention(1L, status));

        BizException ex = assertThrows(BizException.class,
                () -> intentionService.updateIntention(1L, new IntentionSaveDTO()));
        assertTrue(ex.getMessage().contains("不允许修改"),
                "状态[" + status + "] 应返回不允许修改的提示");
    }

    @ParameterizedTest(name = "状态[{0}]可编辑")
    @ValueSource(ints = {0, 3})   // 草稿/驳回
    @DisplayName("updateIntention：草稿(0)和驳回(3)状态可正常编辑")
    void updateIntention_editableStatus_succeeds(int status) {
        when(intentionMapper.selectById(2L)).thenReturn(buildIntention(2L, status));
        when(intentionMapper.updateById(any(InvIntention.class))).thenReturn(1);

        assertDoesNotThrow(() -> intentionService.updateIntention(2L, new IntentionSaveDTO()),
                "状态[" + status + "] 应允许编辑");
    }

    // ─── 删除校验 ────────────────────────────────────────────────

    @Test
    @DisplayName("deleteIntention：审批中(1)不可删除")
    void deleteIntention_approvingStatus_throws() {
        when(intentionMapper.selectById(10L)).thenReturn(buildIntention(10L, 1));

        BizException ex = assertThrows(BizException.class,
                () -> intentionService.deleteIntention(10L));
        assertTrue(ex.getMessage().contains("审批中") && ex.getMessage().contains("不能删除"),
                "审批中应禁止删除");
    }

    @Test
    @DisplayName("deleteIntention：已转合同(4)不可删除")
    void deleteIntention_convertedStatus_throws() {
        when(intentionMapper.selectById(11L)).thenReturn(buildIntention(11L, 4));

        BizException ex = assertThrows(BizException.class,
                () -> intentionService.deleteIntention(11L));
        assertTrue(ex.getMessage().contains("已转合同") && ex.getMessage().contains("不能删除"),
                "已转合同应禁止删除");
    }

    // ─── 提交审批：submitApproval ────────────────────────────────

    @ParameterizedTest(name = "状态[{0}]不可提交审批")
    @ValueSource(ints = {1, 2, 4})   // 审批中/通过/已转合同
    @DisplayName("submitApproval：非草稿/驳回状态不可提交审批")
    void submitApproval_invalidStatus_throws(int status) {
        when(intentionMapper.selectById(20L)).thenReturn(buildIntention(20L, status));

        BizException ex = assertThrows(BizException.class,
                () -> intentionService.submitApproval(20L));
        assertTrue(ex.getMessage().contains("不允许发起审批"),
                "状态[" + status + "] 应返回不允许发起审批的提示");
    }

    @ParameterizedTest(name = "状态[{0}]可提交审批")
    @ValueSource(ints = {0, 3})   // 草稿/驳回
    @DisplayName("submitApproval：草稿(0)和驳回(3)可提交审批，状态变为审批中(1)")
    void submitApproval_validStatus_succeeds(int status) {
        when(intentionMapper.selectById(21L)).thenReturn(buildIntention(21L, status));
        lenient().when(intentionMapper.update(any(), any(Wrapper.class))).thenReturn(1);

        assertDoesNotThrow(() -> intentionService.submitApproval(21L),
                "状态[" + status + "] 应允许提交审批");
    }

    // ─── 审批回调：handleApprovalCallback ───────────────────────

    @Test
    @DisplayName("handleApprovalCallback：当前非审批中(1)时抛出 BizException")
    void handleApprovalCallback_notApproving_throws() {
        // 已通过的单据不可再回调
        when(intentionMapper.selectById(30L)).thenReturn(buildIntention(30L, 2));

        ApprovalCallbackDTO dto = new ApprovalCallbackDTO();
        dto.setApproved(true);

        BizException ex = assertThrows(BizException.class,
                () -> intentionService.handleApprovalCallback(30L, dto));
        assertTrue(ex.getMessage().contains("不在审批中"),
                "非审批中状态应拒绝审批回调");
    }

    @Test
    @DisplayName("handleApprovalCallback：审批通过时状态变为 APPROVED(2)")
    void handleApprovalCallback_approved_statusChanges() {
        when(intentionMapper.selectById(31L)).thenReturn(buildIntention(31L, 1));
        when(intentionMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        // updateShopStatusToIntention 内部调用 intentionShopService.list()
        lenient().when(intentionShopService.list(any(Wrapper.class))).thenReturn(java.util.List.of());

        ApprovalCallbackDTO dto = new ApprovalCallbackDTO();
        dto.setApproved(true);
        dto.setApprovalId("REAL-APPROVAL-001");

        assertDoesNotThrow(() -> intentionService.handleApprovalCallback(31L, dto));
        verify(intentionMapper, times(1)).update(any(), any(Wrapper.class));
    }

    @Test
    @DisplayName("handleApprovalCallback：审批驳回时状态变为 REJECTED(3)")
    void handleApprovalCallback_rejected_statusChanges() {
        when(intentionMapper.selectById(32L)).thenReturn(buildIntention(32L, 1));
        when(intentionMapper.update(any(), any(Wrapper.class))).thenReturn(1);

        ApprovalCallbackDTO dto = new ApprovalCallbackDTO();
        dto.setApproved(false);

        assertDoesNotThrow(() -> intentionService.handleApprovalCallback(32L, dto));
        // 驳回不触发 updateShopStatusToIntention
        verify(intentionShopService, never()).list(any(Wrapper.class));
    }

    // ─── 记录不存在校验 ──────────────────────────────────────────

    @Test
    @DisplayName("所有操作：意向协议不存在时应抛出 BizException")
    void allOperations_notFound_throws() {
        when(intentionMapper.selectById(anyLong())).thenReturn(null);

        assertAll(
                () -> assertThrows(BizException.class,
                        () -> intentionService.updateIntention(99L, new IntentionSaveDTO()),
                        "updateIntention: 不存在应抛出异常"),
                () -> assertThrows(BizException.class,
                        () -> intentionService.deleteIntention(99L),
                        "deleteIntention: 不存在应抛出异常"),
                () -> assertThrows(BizException.class,
                        () -> intentionService.submitApproval(99L),
                        "submitApproval: 不存在应抛出异常")
        );
    }

    // ─── 辅助方法 ─────────────────────────────────────────────────

    private InvIntention buildIntention(Long id, int status) {
        InvIntention intention = new InvIntention();
        intention.setId(id);
        intention.setStatus(status);
        intention.setIntentionName("测试意向-" + id);
        intention.setProjectId(1L);
        return intention;
    }
}
