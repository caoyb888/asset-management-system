package com.asset.api.workflow;

import com.asset.api.workflow.dto.ApprovalSubmitDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Mock 审批服务实现（默认）
 * <p>
 * 保留现有行为：直接生成 Mock approvalId，不调用外部服务。
 * 配置 approval.engine=mock 或未配置时启用。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "approval.engine", havingValue = "mock", matchIfMissing = true)
public class MockApprovalService implements ApprovalService {

    @Override
    public String submit(ApprovalSubmitDTO dto) {
        String approvalId = "MOCK-" + dto.getBusinessType() + "-" + dto.getBusinessId()
                + "-" + System.currentTimeMillis();
        log.info("[Mock审批] 业务类型={}, 业务ID={}, 标题={}, approvalId={}",
                dto.getBusinessType(), dto.getBusinessId(), dto.getTitle(), approvalId);
        return approvalId;
    }
}
