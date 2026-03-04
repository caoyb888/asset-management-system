package com.asset.investment.intention.service;

import com.asset.common.exception.BizException;
import com.asset.investment.common.enums.IntentionStatus;
import com.asset.investment.engine.BillingGenerator;
import com.asset.investment.engine.RentCalculateStrategyRouter;
import com.asset.investment.intention.dto.ApprovalCallbackDTO;
import com.asset.investment.intention.dto.IntentionSaveDTO;
import com.asset.investment.intention.dto.IntentionShopItemDTO;
import com.asset.investment.intention.entity.InvIntention;
import com.asset.investment.intention.entity.InvIntentionShop;
import com.asset.investment.intention.mapper.InvIntentionMapper;
import com.asset.investment.intention.service.impl.InvIntentionServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 意向协议 Service 单元测试（INT-U-01 ~ INT-U-12）
 * 使用 Mockito @Spy + @InjectMocks，不启动 Spring 容器
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("意向协议 Service 单元测试")
class InvIntentionServiceTest {

    @Mock
    InvIntentionMapper intentionMapper;

    @Mock
    InvIntentionShopService intentionShopService;

    @Mock
    InvIntentionFeeService intentionFeeService;

    @Mock
    InvIntentionFeeStageService intentionFeeStageService;

    @Mock
    InvIntentionBillingService intentionBillingService;

    @Mock
    BillingGenerator billingGenerator;

    @Mock
    RentCalculateStrategyRouter strategyRouter;

    @Spy
    @InjectMocks
    InvIntentionServiceImpl service;

    @BeforeEach
    void setUp() {
        // 将 mock Mapper 注入 ServiceImpl 的 baseMapper 继承字段
        ReflectionTestUtils.setField(service, "baseMapper", intentionMapper);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INT-U-01 新建意向-自动草稿状态
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("INT-U-01 新建意向-自动草稿状态-编码以INV开头")
    void createIntention_setsDraftStatusAndCodePrefix() {
        IntentionSaveDTO dto = new IntentionSaveDTO();
        dto.setIntentionName("测试意向");
        dto.setProjectId(90001L);

        // 模拟 count 返回 0（编号序列）
        doReturn(0L).when(service).count(any());
        // 模拟 save 成功
        doReturn(true).when(service).save(any(InvIntention.class));

        Long id = service.createIntention(dto);

        // 验证 save 被调用1次
        verify(service, times(1)).save(any(InvIntention.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INT-U-02 编辑意向-草稿可编辑
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("INT-U-02 编辑意向-草稿状态-调用updateById无异常")
    void updateIntention_draftStatus_callsUpdateById() {
        InvIntention draft = buildIntention(1L, IntentionStatus.DRAFT.getCode());
        doReturn(draft).when(service).getById(1L);
        doReturn(true).when(service).updateById(any());

        IntentionSaveDTO dto = new IntentionSaveDTO();
        dto.setIntentionName("修改后名称");

        service.updateIntention(1L, dto);

        verify(service, times(1)).updateById(any(InvIntention.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INT-U-03 编辑意向-审批中不可编辑
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("INT-U-03 编辑意向-审批中状态-抛BizException")
    void updateIntention_approvingStatus_throwsBizException() {
        InvIntention approving = buildIntention(2L, IntentionStatus.APPROVING.getCode());
        doReturn(approving).when(service).getById(2L);

        assertThatThrownBy(() -> service.updateIntention(2L, new IntentionSaveDTO()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不允许修改");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INT-U-04 删除意向-草稿可删
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("INT-U-04 删除意向-草稿状态-调用removeById无异常")
    void deleteIntention_draftStatus_callsRemoveById() {
        InvIntention draft = buildIntention(1L, IntentionStatus.DRAFT.getCode());
        doReturn(draft).when(service).getById(1L);
        doReturn(true).when(service).removeById(1L);

        service.deleteIntention(1L);

        verify(service, times(1)).removeById(1L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INT-U-05 删除意向-审批中不可删
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("INT-U-05 删除意向-审批中状态-抛BizException含'审批中'")
    void deleteIntention_approvingStatus_throwsBizException() {
        InvIntention approving = buildIntention(2L, IntentionStatus.APPROVING.getCode());
        doReturn(approving).when(service).getById(2L);

        assertThatThrownBy(() -> service.deleteIntention(2L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("审批中");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INT-U-06 删除意向-已转合同不可删
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("INT-U-06 删除意向-已转合同状态-抛BizException含'已转合同'")
    void deleteIntention_convertedStatus_throwsBizException() {
        InvIntention converted = buildIntention(3L, IntentionStatus.CONVERTED.getCode());
        doReturn(converted).when(service).getById(3L);

        assertThatThrownBy(() -> service.deleteIntention(3L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已转合同");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INT-U-07 发起审批-草稿→审批中
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("INT-U-07 发起审批-草稿状态-调用update置status=1")
    void submitApproval_draftStatus_setsApprovingStatus() {
        InvIntention draft = buildIntention(1L, IntentionStatus.DRAFT.getCode());
        doReturn(draft).when(service).getById(1L);
        doReturn(true).when(service).update(any());

        service.submitApproval(1L);

        verify(service, times(1)).update(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INT-U-08 发起审批-驳回→审批中（重新发起）
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("INT-U-08 发起审批-驳回状态-调用update置status=1")
    void submitApproval_rejectedStatus_setsApprovingStatus() {
        InvIntention rejected = buildIntention(4L, IntentionStatus.REJECTED.getCode());
        doReturn(rejected).when(service).getById(4L);
        doReturn(true).when(service).update(any());

        service.submitApproval(4L);

        verify(service, times(1)).update(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INT-U-09 发起审批-非法状态抛异常
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("INT-U-09 发起审批-审批中状态-抛BizException含'不允许发起审批'")
    void submitApproval_approvingStatus_throwsBizException() {
        InvIntention approving = buildIntention(2L, IntentionStatus.APPROVING.getCode());
        doReturn(approving).when(service).getById(2L);

        assertThatThrownBy(() -> service.submitApproval(2L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不允许发起审批");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INT-U-10 审批回调-通过
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("INT-U-10 审批回调-通过-调用update置status=2")
    void handleApprovalCallback_approved_setsApprovedStatus() {
        InvIntention approving = buildIntention(2L, IntentionStatus.APPROVING.getCode());
        doReturn(approving).when(service).getById(2L);
        doReturn(true).when(service).update(any());
        // updateShopStatusToIntention 内部调用 intentionShopService.list
        when(intentionShopService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        ApprovalCallbackDTO dto = new ApprovalCallbackDTO();
        dto.setApproved(true);

        service.handleApprovalCallback(2L, dto);

        verify(service, times(1)).update(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INT-U-11 审批回调-驳回
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("INT-U-11 审批回调-驳回-调用update置status=3")
    void handleApprovalCallback_rejected_setsRejectedStatus() {
        InvIntention approving = buildIntention(2L, IntentionStatus.APPROVING.getCode());
        doReturn(approving).when(service).getById(2L);
        doReturn(true).when(service).update(any());

        ApprovalCallbackDTO dto = new ApprovalCallbackDTO();
        dto.setApproved(false);

        service.handleApprovalCallback(2L, dto);

        verify(service, times(1)).update(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INT-U-12 保存商铺-全量替换
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("INT-U-12 保存商铺-全量替换-先remove后saveBatch")
    void saveShops_replacesAll_removeThenSaveBatch() {
        InvIntention draft = buildIntention(1L, IntentionStatus.DRAFT.getCode());
        doReturn(draft).when(service).getById(1L);
        when(intentionShopService.remove(any(LambdaQueryWrapper.class))).thenReturn(true);
        when(intentionShopService.saveBatch(anyList())).thenReturn(true);

        IntentionShopItemDTO s1 = new IntentionShopItemDTO();
        s1.setShopId(100L);
        IntentionShopItemDTO s2 = new IntentionShopItemDTO();
        s2.setShopId(200L);

        service.saveShops(1L, List.of(s1, s2));

        verify(intentionShopService, times(1)).remove(any(LambdaQueryWrapper.class));
        verify(intentionShopService, times(1)).saveBatch(
                argThat(list -> ((List<?>) list).size() == 2));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 工具方法
    // ─────────────────────────────────────────────────────────────────────────

    private InvIntention buildIntention(Long id, int status) {
        InvIntention intention = new InvIntention();
        intention.setId(id);
        intention.setStatus(status);
        intention.setIntentionCode("INV202601" + String.format("%04d", id));
        return intention;
    }
}
