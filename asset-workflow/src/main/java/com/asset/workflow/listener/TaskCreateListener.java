package com.asset.workflow.listener;

import com.asset.workflow.entity.WfProcessInstance;
import com.asset.workflow.service.WfProcessInstanceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;

/**
 * Flowable 任务创建监听器
 * <p>
 * 当新任务创建时，更新 wf_process_instance 的当前节点和审批人信息
 */
@Slf4j
@Component("taskCreateListener")
@RequiredArgsConstructor
public class TaskCreateListener implements TaskListener {

    private final WfProcessInstanceService instanceService;

    @Override
    public void notify(DelegateTask delegateTask) {
        String processInstanceId = delegateTask.getProcessInstanceId();
        String taskName = delegateTask.getName();
        String assignee = delegateTask.getAssignee();

        log.info("[任务创建] flowableInstanceId={}, taskName={}, assignee={}",
                processInstanceId, taskName, assignee);

        WfProcessInstance instance = instanceService.getOne(
                new LambdaQueryWrapper<WfProcessInstance>()
                        .eq(WfProcessInstance::getFlowableInstanceId, processInstanceId));
        if (instance == null) {
            log.warn("[任务创建] 未找到对应的 wf_process_instance, flowableId={}", processInstanceId);
            return;
        }

        LambdaUpdateWrapper<WfProcessInstance> update = new LambdaUpdateWrapper<WfProcessInstance>()
                .eq(WfProcessInstance::getId, instance.getId())
                .set(WfProcessInstance::getCurrentNodeName, taskName);
        if (assignee != null) {
            try {
                update.set(WfProcessInstance::getCurrentAssigneeId, Long.parseLong(assignee));
            } catch (NumberFormatException ignored) {
            }
        }
        instanceService.update(update);
    }
}
