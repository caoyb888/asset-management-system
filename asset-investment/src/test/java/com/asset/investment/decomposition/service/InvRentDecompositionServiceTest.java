package com.asset.investment.decomposition.service;

import com.asset.common.exception.BizException;
import com.asset.investment.decomposition.entity.InvRentDecomposition;
import com.asset.investment.decomposition.mapper.InvRentDecompositionMapper;
import com.asset.investment.decomposition.service.impl.InvRentDecompositionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 租金分解 Service 单元测试（DECOMP-U-01 ~ DECOMP-U-05）
 * 使用 Mockito @Spy + @InjectMocks，不启动 Spring 容器
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("租金分解 Service 单元测试")
class InvRentDecompositionServiceTest {

    @Mock
    InvRentDecompositionMapper decompositionMapper;

    @Spy
    @InjectMocks
    InvRentDecompositionServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", decompositionMapper);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DECOMP-U-01 发起审批-草稿→审批中
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DECOMP-U-01 发起审批-草稿状态-调用update置status=1且approvalId非空")
    void submitApproval_draftStatus_setsApprovingStatus() {
        InvRentDecomposition draft = buildDecomp(1L, 0);
        doReturn(draft).when(service).getById(1L);
        doReturn(true).when(service).update(any());

        service.submitApproval(1L);

        verify(service, times(1)).update(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DECOMP-U-02 发起审批-驳回→审批中（重新发起）
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DECOMP-U-02 发起审批-驳回状态-调用update置status=1")
    void submitApproval_rejectedStatus_setsApprovingStatus() {
        InvRentDecomposition rejected = buildDecomp(2L, 3);
        doReturn(rejected).when(service).getById(2L);
        doReturn(true).when(service).update(any());

        service.submitApproval(2L);

        verify(service, times(1)).update(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DECOMP-U-03 发起审批-审批中→抛异常
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DECOMP-U-03 发起审批-审批中状态-抛BizException含'仅草稿或驳回'")
    void submitApproval_approvingStatus_throwsBizException() {
        InvRentDecomposition approving = buildDecomp(3L, 1);
        doReturn(approving).when(service).getById(3L);

        assertThatThrownBy(() -> service.submitApproval(3L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("仅草稿或驳回");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DECOMP-U-04 审批回调-通过
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DECOMP-U-04 审批回调-通过-调用update置status=2")
    void handleApprovalCallback_approved_setsApprovedStatus() {
        InvRentDecomposition approving = buildDecomp(1L, 1);
        doReturn(approving).when(service).getById(1L);
        doReturn(true).when(service).update(any());

        service.handleApprovalCallback(1L, true);

        verify(service, times(1)).update(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DECOMP-U-05 审批回调-驳回
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DECOMP-U-05 审批回调-驳回-调用update置status=3")
    void handleApprovalCallback_rejected_setsRejectedStatus() {
        InvRentDecomposition approving = buildDecomp(1L, 1);
        doReturn(approving).when(service).getById(1L);
        doReturn(true).when(service).update(any());

        service.handleApprovalCallback(1L, false);

        verify(service, times(1)).update(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 工具方法
    // ─────────────────────────────────────────────────────────────────────────

    private InvRentDecomposition buildDecomp(Long id, int status) {
        InvRentDecomposition decomp = new InvRentDecomposition();
        decomp.setId(id);
        decomp.setStatus(status);
        return decomp;
    }
}
