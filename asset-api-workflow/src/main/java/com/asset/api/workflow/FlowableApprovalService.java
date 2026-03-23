package com.asset.api.workflow;

import com.asset.api.workflow.client.WorkflowFeignClient;
import com.asset.api.workflow.dto.ApprovalSubmitDTO;
import com.asset.common.exception.BizException;
import com.asset.common.model.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Flowable 审批服务实现
 * <p>
 * 通过 Feign 调用 asset-workflow 服务发起审批流程。
 * 配置 approval.engine=flowable 时启用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "approval.engine", havingValue = "flowable")
public class FlowableApprovalService implements ApprovalService {

    private final WorkflowFeignClient workflowClient;

    @Override
    public String submit(ApprovalSubmitDTO dto) {
        log.info("[Flowable审批] 提交审批: 业务类型={}, 业务ID={}, 标题={}",
                dto.getBusinessType(), dto.getBusinessId(), dto.getTitle());
        R<String> result = workflowClient.submitApproval(dto);
        if (result.getCode() != 200) {
            throw new BizException("审批提交失败: " + result.getMsg());
        }
        return result.getData();
    }
}
