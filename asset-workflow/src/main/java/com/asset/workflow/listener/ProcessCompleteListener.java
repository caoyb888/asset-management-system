package com.asset.workflow.listener;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

/**
 * Flowable 流程完成监听器
 * <p>
 * 流程正常结束时记录日志。
 * 实际的回调分发在 WorkflowApprovalServiceImpl.approve() 中检测流程结束后触发，
 * 确保事务内一致性。
 */
@Slf4j
@Component("processCompleteListener")
public class ProcessCompleteListener implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) {
        log.info("[流程完成] flowableInstanceId={}, processDefinitionId={}",
                execution.getProcessInstanceId(), execution.getProcessDefinitionId());
    }
}
