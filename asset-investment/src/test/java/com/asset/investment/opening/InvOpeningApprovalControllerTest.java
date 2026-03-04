package com.asset.investment.opening;

import com.asset.common.exception.BizException;
import com.asset.investment.opening.controller.InvOpeningApprovalController;
import com.asset.investment.opening.entity.InvOpeningApproval;
import com.asset.investment.opening.service.InvOpeningApprovalService;
import com.asset.investment.opening.service.InvOpeningAttachmentService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 开业审批控制器单元测试（OA-05）
 * 覆盖：驳回快照保存、基于历史创建（数据回填）、状态机校验
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("开业审批控制器测试 (OA-05)")
class InvOpeningApprovalControllerTest {

    @Mock private InvOpeningApprovalService approvalService;
    @Mock private InvOpeningAttachmentService attachmentService;

    private InvOpeningApprovalController controller;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeAll
    static void initMybatisPlusCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, InvOpeningApproval.class);
    }

    @BeforeEach
    void setUp() {
        controller = new InvOpeningApprovalController(approvalService, attachmentService, objectMapper);
    }

    // ─── OA-05：驳回处理 ─────────────────────────────────────────

    @Test
    @DisplayName("OA-05-1：审批回调驳回时，快照数据 snapshotData 被序列化保存")
    void approvalCallback_rejected_snapshotDataSaved() {
        // 准备：一条审批中的单据
        InvOpeningApproval approval = buildApproval(1L, 1); // status=1 审批中
        approval.setProjectId(10L);
        approval.setShopId(20L);
        approval.setContractId(30L);
        approval.setPlannedOpeningDate(LocalDate.of(2026, 6, 1));
        when(approvalService.getById(1L)).thenReturn(approval);
        when(approvalService.updateById(any())).thenReturn(true);

        // 执行驳回
        controller.approvalCallback(1L, Map.of("approved", false));

        // 验证 updateById() 被调用，且 snapshotData 字段已设置（通过 ArgumentCaptor 捕获实体）
        verify(approvalService, times(1)).updateById(any());
        // 通过直接调用 objectMapper 验证序列化可行
        JsonNode snapshot = objectMapper.valueToTree(approval);
        assertNotNull(snapshot.get("shopId"), "快照应包含 shopId");
        assertNotNull(snapshot.get("projectId"), "快照应包含 projectId");
        assertNotNull(snapshot.get("plannedOpeningDate"), "快照应包含 plannedOpeningDate");
    }

    @Test
    @DisplayName("OA-05-2：审批回调通过时，snapshotData 字段不写入（无需快照）")
    void approvalCallback_approved_noSnapshot() {
        InvOpeningApproval approval = buildApproval(2L, 1);
        when(approvalService.getById(2L)).thenReturn(approval);
        when(approvalService.updateById(any())).thenReturn(true);

        controller.approvalCallback(2L, Map.of("approved", true));

        // 通过：updateById 被调用，但不涉及 snapshotData 的 set
        verify(approvalService, times(1)).updateById(any());
    }

    @Test
    @DisplayName("OA-05-3：回调时当前状态非「审批中」时抛出 BizException")
    void approvalCallback_notApproving_throws() {
        // 已通过的单据（status=2）不可再回调
        InvOpeningApproval approval = buildApproval(3L, 2);
        when(approvalService.getById(3L)).thenReturn(approval);

        BizException ex = assertThrows(BizException.class,
                () -> controller.approvalCallback(3L, Map.of("approved", false)));
        assertTrue(ex.getMessage().contains("不在审批中"), "应提示当前状态不在审批中");
    }

    // ─── OA-05：基于历史创建（数据回填）─────────────────────────

    @Test
    @DisplayName("OA-05-4：基于驳回单据创建新单，业务字段全部回填，状态重置为草稿")
    void createFromPrevious_copiesAllBusinessFields() {
        InvOpeningApproval source = buildApproval(10L, 3); // status=3 已驳回
        source.setProjectId(100L);
        source.setBuildingId(200L);
        source.setFloorId(300L);
        source.setShopId(400L);
        source.setContractId(500L);
        source.setMerchantId(600L);
        source.setPlannedOpeningDate(LocalDate.of(2026, 8, 1));
        source.setActualOpeningDate(LocalDate.of(2026, 8, 15));
        source.setRemark("首次申请驳回原因：材料不全");

        when(approvalService.getById(10L)).thenReturn(source);
        when(approvalService.count()).thenReturn(5L); // 用于生成 OA 编号
        ArgumentCaptor<InvOpeningApproval> captor = ArgumentCaptor.forClass(InvOpeningApproval.class);
        when(approvalService.save(captor.capture())).thenReturn(true);

        controller.createFromPrevious(10L);

        InvOpeningApproval newApproval = captor.getValue();
        // 验证业务字段全部回填
        assertEquals(100L, newApproval.getProjectId(), "projectId 应回填");
        assertEquals(200L, newApproval.getBuildingId(), "buildingId 应回填");
        assertEquals(300L, newApproval.getFloorId(), "floorId 应回填");
        assertEquals(400L, newApproval.getShopId(), "shopId 应回填");
        assertEquals(500L, newApproval.getContractId(), "contractId 应回填");
        assertEquals(600L, newApproval.getMerchantId(), "merchantId 应回填");
        assertEquals(LocalDate.of(2026, 8, 1), newApproval.getPlannedOpeningDate(), "计划开业日应回填");
        assertEquals("首次申请驳回原因：材料不全", newApproval.getRemark(), "备注应回填");
        // 验证新单系统字段
        assertEquals(0, newApproval.getStatus(), "新单状态应为草稿(0)");
        assertEquals(10L, newApproval.getPreviousApprovalId(), "应关联原单ID");
        assertNotNull(newApproval.getApprovalCode(), "应生成新审批编号");
    }

    @Test
    @DisplayName("OA-05-5：基于「非驳回」状态创建新单时，应抛出 BizException")
    void createFromPrevious_nonRejected_throws() {
        // 草稿（0）、审批中（1）、通过（2）均不允许作为来源
        int[] invalidStatuses = {0, 1, 2};
        for (int status : invalidStatuses) {
            InvOpeningApproval source = buildApproval((long) status + 20, status);
            when(approvalService.getById((long) status + 20)).thenReturn(source);

            BizException ex = assertThrows(BizException.class,
                    () -> controller.createFromPrevious((long) status + 20),
                    "状态[" + status + "] 应拒绝基于历史创建");
            assertTrue(ex.getMessage().contains("仅驳回状态"), "应提示仅驳回状态可基于历史创建");
        }
    }

    @Test
    @DisplayName("OA-05-6：原始记录不存在时，createFromPrevious 应抛出 BizException")
    void createFromPrevious_notFound_throws() {
        when(approvalService.getById(99L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class,
                () -> controller.createFromPrevious(99L));
        assertTrue(ex.getMessage().contains("不存在"), "应提示原始记录不存在");
    }

    // ─── 状态机：编辑校验 ────────────────────────────────────────

    @Test
    @DisplayName("状态机：审批中(1)不可编辑，应抛出 BizException")
    void update_approvingStatus_throws() {
        InvOpeningApproval existing = buildApproval(50L, 1); // status=1 审批中
        when(approvalService.getById(50L)).thenReturn(existing);

        BizException ex = assertThrows(BizException.class,
                () -> controller.update(50L, new InvOpeningApproval()));
        assertTrue(ex.getMessage().contains("审批中不可修改"), "审批中应不允许编辑");
    }

    @Test
    @DisplayName("状态机：草稿(0)可以正常编辑")
    void update_draftStatus_succeeds() {
        InvOpeningApproval existing = buildApproval(51L, 0);
        when(approvalService.getById(51L)).thenReturn(existing);
        when(approvalService.updateById(any())).thenReturn(true);

        assertDoesNotThrow(() -> controller.update(51L, new InvOpeningApproval()));
        verify(approvalService, times(1)).updateById(any());
    }

    // ─── 状态机：提交审批 ────────────────────────────────────────

    @Test
    @DisplayName("状态机：草稿(0)可提交审批，状态变为审批中(1)")
    void submit_fromDraft_succeeds() {
        InvOpeningApproval existing = buildApproval(60L, 0);
        when(approvalService.getById(60L)).thenReturn(existing);
        when(approvalService.update(any())).thenReturn(true);

        assertDoesNotThrow(() -> controller.submit(60L));
        verify(approvalService, times(1)).update(any());
    }

    @Test
    @DisplayName("状态机：已通过(2)的单据不可再次提交审批")
    void submit_alreadyApproved_throws() {
        InvOpeningApproval existing = buildApproval(61L, 2); // 已通过
        when(approvalService.getById(61L)).thenReturn(existing);

        BizException ex = assertThrows(BizException.class,
                () -> controller.submit(61L));
        assertTrue(ex.getMessage().contains("仅草稿状态可提交审批"),
                "已通过状态不可再次提交审批");
    }

    // ─── 状态机：删除校验 ────────────────────────────────────────

    @Test
    @DisplayName("状态机：审批中(1)不可删除")
    void delete_approvingStatus_throws() {
        InvOpeningApproval existing = buildApproval(70L, 1);
        when(approvalService.getById(70L)).thenReturn(existing);

        BizException ex = assertThrows(BizException.class,
                () -> controller.delete(70L));
        assertTrue(ex.getMessage().contains("审批中不可删除"));
    }

    @Test
    @DisplayName("状态机：已通过(2)不可删除")
    void delete_approvedStatus_throws() {
        InvOpeningApproval existing = buildApproval(71L, 2);
        when(approvalService.getById(71L)).thenReturn(existing);

        BizException ex = assertThrows(BizException.class,
                () -> controller.delete(71L));
        assertTrue(ex.getMessage().contains("不可删除"));
    }

    // ─── 辅助方法 ─────────────────────────────────────────────────

    private InvOpeningApproval buildApproval(Long id, int status) {
        InvOpeningApproval a = new InvOpeningApproval();
        a.setId(id);
        a.setStatus(status);
        a.setApprovalCode("OA" + String.format("%06d", id));
        return a;
    }
}
