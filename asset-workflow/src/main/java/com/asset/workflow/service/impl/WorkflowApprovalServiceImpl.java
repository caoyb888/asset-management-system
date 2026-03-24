package com.asset.workflow.service.impl;

import com.asset.api.workflow.dto.*;
import com.asset.api.workflow.enums.ApprovalAction;
import com.asset.api.workflow.enums.ProcessStatus;
import com.asset.common.exception.BizException;
import com.asset.workflow.callback.ApprovalCallbackDispatcher;
import com.asset.workflow.entity.WfApprovalRecord;
import com.asset.workflow.entity.WfProcessDefinition;
import com.asset.workflow.entity.WfProcessInstance;
import com.asset.workflow.mapper.WfProcessInstanceMapper;
import com.asset.workflow.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一审批服务核心实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowApprovalServiceImpl implements WorkflowApprovalService {

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final WfProcessDefinitionService definitionService;
    private final WfProcessInstanceService instanceService;
    private final WfProcessInstanceMapper instanceMapper;
    private final WfApprovalRecordService recordService;
    private final ApprovalCallbackDispatcher callbackDispatcher;
    private final WfProcessInstanceServiceImpl instanceServiceImpl;
    private final ObjectMapper objectMapper;
    private final ApproverResolveService approverResolveService;

    // ==================== 发起审批 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String submit(ApprovalSubmitDTO dto) {
        // 1. 查找流程定义
        WfProcessDefinition def = definitionService.getByBusinessType(dto.getBusinessType());

        // 2. 检查是否已有活跃流程
        WfProcessInstance existing = instanceService.getByBusiness(dto.getBusinessType(), dto.getBusinessId());
        if (existing != null && (existing.getStatus() == ProcessStatus.PENDING.getCode()
                || existing.getStatus() == ProcessStatus.IN_PROGRESS.getCode())) {
            throw new BizException("该业务单据已有进行中的审批流程");
        }

        // 3. 启动 Flowable 流程
        Map<String, Object> variables = new HashMap<>();
        variables.put("initiatorId", dto.getInitiatorId());
        variables.put("businessType", dto.getBusinessType());
        variables.put("businessId", dto.getBusinessId());
        if (dto.getVariables() != null) {
            variables.putAll(dto.getVariables());
        }
        // 自动解析审批人（DEPT_LEADER / INITIATOR_LEADER 策略）
        approverResolveService.resolveAndFill(dto.getInitiatorId(), variables);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                def.getProcessKey(), dto.getBusinessType() + ":" + dto.getBusinessId(), variables);

        // 4. 写入 wf_process_instance
        WfProcessInstance wfInstance = new WfProcessInstance();
        wfInstance.setProcessKey(def.getProcessKey());
        wfInstance.setFlowableInstanceId(processInstance.getId());
        wfInstance.setBusinessType(dto.getBusinessType());
        wfInstance.setBusinessId(dto.getBusinessId());
        wfInstance.setTitle(dto.getTitle());
        wfInstance.setInitiatorId(dto.getInitiatorId());
        wfInstance.setInitiatorName(dto.getInitiatorName());
        wfInstance.setProjectId(dto.getProjectId());
        wfInstance.setStatus(ProcessStatus.IN_PROGRESS.getCode());
        wfInstance.setPriority(dto.getPriority() != null ? dto.getPriority() : 0);
        wfInstance.setStartedAt(LocalDateTime.now());
        try {
            if (dto.getVariables() != null) {
                wfInstance.setVariablesJson(objectMapper.writeValueAsString(dto.getVariables()));
            }
        } catch (Exception e) {
            log.warn("序列化 variables 失败", e);
        }

        // 查找当前任务的审批人
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId()).list();
        if (!tasks.isEmpty()) {
            Task currentTask = tasks.get(0);
            wfInstance.setCurrentNodeName(currentTask.getName());
            if (currentTask.getAssignee() != null) {
                try {
                    wfInstance.setCurrentAssigneeId(Long.parseLong(currentTask.getAssignee()));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // 如果有旧的已完成记录，先逻辑删除
        if (existing != null) {
            existing.setIsDeleted(1);
            instanceService.updateById(existing);
        }
        instanceService.save(wfInstance);

        log.info("[审批发起] 流程={}，业务={}:{}，flowableId={}",
                def.getProcessKey(), dto.getBusinessType(), dto.getBusinessId(), processInstance.getId());
        return processInstance.getId();
    }

    // ==================== 通过审批 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long instanceId, String comment, Long approverId, String approverName) {
        WfProcessInstance wfInstance = getAndCheck(instanceId, ProcessStatus.IN_PROGRESS);

        // 查找 Flowable 当前任务
        Task task = getCurrentTask(wfInstance.getFlowableInstanceId());

        // 记录审批
        saveRecord(wfInstance, task, ApprovalAction.APPROVE, comment, approverId, approverName);

        // 完成 Flowable 任务
        if (StringUtils.hasText(comment)) {
            taskService.addComment(task.getId(), wfInstance.getFlowableInstanceId(), comment);
        }
        taskService.complete(task.getId());

        // 检查流程是否已结束
        ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                .processInstanceId(wfInstance.getFlowableInstanceId()).singleResult();
        if (pi == null) {
            // 流程已结束 → 通过
            finishInstance(wfInstance, ProcessStatus.APPROVED, comment);
            // 触发回调
            dispatchCallback(wfInstance, ProcessStatus.APPROVED, comment, approverId, approverName);
        } else {
            // 流转到下一个节点
            updateCurrentNode(wfInstance);
        }

        log.info("[审批通过] instanceId={}, approver={}", instanceId, approverName);
    }

    // ==================== 驳回审批 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long instanceId, String comment, Long approverId, String approverName) {
        WfProcessInstance wfInstance = getAndCheck(instanceId, ProcessStatus.IN_PROGRESS);
        Task task = getCurrentTask(wfInstance.getFlowableInstanceId());

        // 记录审批
        saveRecord(wfInstance, task, ApprovalAction.REJECT, comment, approverId, approverName);

        // 终止 Flowable 流程
        runtimeService.deleteProcessInstance(wfInstance.getFlowableInstanceId(), "驳回: " + comment);

        // 更新实例状态
        finishInstance(wfInstance, ProcessStatus.REJECTED, comment);

        // 触发回调
        dispatchCallback(wfInstance, ProcessStatus.REJECTED, comment, approverId, approverName);

        log.info("[审批驳回] instanceId={}, approver={}, comment={}", instanceId, approverName, comment);
    }

    // ==================== 撤回审批 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long instanceId) {
        WfProcessInstance wfInstance = getAndCheck(instanceId, ProcessStatus.IN_PROGRESS);

        // 检查是否还有审批记录（有的话说明已经有人审批过，不能撤回）
        long recordCount = recordService.count(new LambdaQueryWrapper<WfApprovalRecord>()
                .eq(WfApprovalRecord::getInstanceId, instanceId));
        if (recordCount > 0) {
            throw new BizException("审批流程已有审批操作，无法撤回");
        }

        // 终止 Flowable 流程
        runtimeService.deleteProcessInstance(wfInstance.getFlowableInstanceId(), "发起人撤回");

        // 更新实例状态
        finishInstance(wfInstance, ProcessStatus.REVOKED, "发起人撤回");

        // 回调业务模块（驳回处理，业务侧将状态回退到草稿）
        dispatchCallback(wfInstance, ProcessStatus.REJECTED, "发起人撤回",
                wfInstance.getInitiatorId(), wfInstance.getInitiatorName());

        log.info("[审批撤回] instanceId={}", instanceId);
    }

    // ==================== 转办 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reassign(Long instanceId, Long targetUserId, String comment,
                         Long operatorId, String operatorName) {
        WfProcessInstance wfInstance = getAndCheck(instanceId, ProcessStatus.IN_PROGRESS);
        Task task = getCurrentTask(wfInstance.getFlowableInstanceId());

        // 记录转办操作
        saveRecord(wfInstance, task, ApprovalAction.REASSIGN, comment, operatorId, operatorName);

        // Flowable 转办
        taskService.setAssignee(task.getId(), String.valueOf(targetUserId));

        // 更新实例当前审批人
        wfInstance.setCurrentAssigneeId(targetUserId);
        instanceService.updateById(wfInstance);

        log.info("[审批转办] instanceId={}, from={} to={}", instanceId, operatorName, targetUserId);
    }

    // ==================== 催办 ====================

    @Override
    public void urge(Long instanceId) {
        WfProcessInstance wfInstance = instanceService.getById(instanceId);
        if (wfInstance == null) throw new BizException("流程实例不存在");
        if (wfInstance.getStatus() != ProcessStatus.IN_PROGRESS.getCode()) {
            throw new BizException("仅审批中的流程可以催办");
        }
        // 记录催办操作到审批记录
        WfApprovalRecord record = new WfApprovalRecord();
        record.setInstanceId(wfInstance.getId());
        record.setApproverId(wfInstance.getInitiatorId());
        record.setApproverName(wfInstance.getInitiatorName());
        record.setAction(6); // 6=催办
        record.setComment("发起人催办");
        record.setNodeName(wfInstance.getCurrentNodeName());
        recordService.save(record);
        // TODO: 对接 asset-message 服务发送催办通知给 currentAssigneeId
        log.info("[催办] instanceId={}, assignee={}, title={}", instanceId,
                wfInstance.getCurrentAssigneeId(), wfInstance.getTitle());
    }

    // ==================== 查询 ====================

    @Override
    public ProcessInstanceVO getDetail(Long instanceId) {
        WfProcessInstance inst = instanceService.getById(instanceId);
        if (inst == null) throw new BizException("流程实例不存在");
        return instanceServiceImpl.toVO(inst);
    }

    @Override
    public List<ApprovalRecordVO> getRecords(Long instanceId) {
        return recordService.listByInstanceId(instanceId);
    }

    @Override
    public ProcessInstanceVO getByBusiness(String businessType, Long businessId) {
        WfProcessInstance inst = instanceService.getByBusiness(businessType, businessId);
        if (inst == null) return null;
        return instanceServiceImpl.toVO(inst);
    }

    @Override
    public IPage<ProcessInstanceVO> todoPage(TaskPageQuery query, Long userId) {
        IPage<WfProcessInstance> page = instanceService.page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<WfProcessInstance>()
                        .eq(WfProcessInstance::getCurrentAssigneeId, userId)
                        .eq(WfProcessInstance::getStatus, ProcessStatus.IN_PROGRESS.getCode())
                        .eq(StringUtils.hasText(query.getBusinessType()),
                                WfProcessInstance::getBusinessType, query.getBusinessType())
                        .like(StringUtils.hasText(query.getTitle()),
                                WfProcessInstance::getTitle, query.getTitle())
                        .orderByDesc(WfProcessInstance::getCreatedAt));
        return page.convert(instanceServiceImpl::toVO);
    }

    @Override
    public IPage<ProcessInstanceVO> donePage(TaskPageQuery query, Long userId) {
        // 查找该用户已审批过的流程实例 ID
        List<WfApprovalRecord> records = recordService.list(
                new LambdaQueryWrapper<WfApprovalRecord>()
                        .eq(WfApprovalRecord::getApproverId, userId)
                        .select(WfApprovalRecord::getInstanceId));
        List<Long> instanceIds = records.stream()
                .map(WfApprovalRecord::getInstanceId).distinct().toList();
        if (instanceIds.isEmpty()) {
            return new Page<>(query.getPageNum(), query.getPageSize());
        }
        IPage<WfProcessInstance> page = instanceService.page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<WfProcessInstance>()
                        .in(WfProcessInstance::getId, instanceIds)
                        .eq(StringUtils.hasText(query.getBusinessType()),
                                WfProcessInstance::getBusinessType, query.getBusinessType())
                        .like(StringUtils.hasText(query.getTitle()),
                                WfProcessInstance::getTitle, query.getTitle())
                        .orderByDesc(WfProcessInstance::getCreatedAt));
        return page.convert(instanceServiceImpl::toVO);
    }

    @Override
    public IPage<ProcessInstanceVO> initiatedPage(TaskPageQuery query, Long userId) {
        IPage<WfProcessInstance> page = instanceService.page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<WfProcessInstance>()
                        .eq(WfProcessInstance::getInitiatorId, userId)
                        .eq(StringUtils.hasText(query.getBusinessType()),
                                WfProcessInstance::getBusinessType, query.getBusinessType())
                        .like(StringUtils.hasText(query.getTitle()),
                                WfProcessInstance::getTitle, query.getTitle())
                        .eq(query.getStatus() != null,
                                WfProcessInstance::getStatus, query.getStatus())
                        .orderByDesc(WfProcessInstance::getCreatedAt));
        return page.convert(instanceServiceImpl::toVO);
    }

    @Override
    public int todoCount(Long userId) {
        return (int) instanceService.count(new LambdaQueryWrapper<WfProcessInstance>()
                .eq(WfProcessInstance::getCurrentAssigneeId, userId)
                .eq(WfProcessInstance::getStatus, ProcessStatus.IN_PROGRESS.getCode()));
    }

    // ==================== 私有方法 ====================

    private WfProcessInstance getAndCheck(Long instanceId, ProcessStatus expected) {
        WfProcessInstance inst = instanceService.getById(instanceId);
        if (inst == null) throw new BizException("流程实例不存在");
        if (inst.getStatus() != expected.getCode()) {
            throw new BizException("流程状态不正确，当前状态: "
                    + ProcessStatus.fromCode(inst.getStatus()).getLabel());
        }
        return inst;
    }

    private Task getCurrentTask(String flowableInstanceId) {
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(flowableInstanceId)
                .orderByTaskCreateTime().asc()
                .list();
        if (tasks.isEmpty()) {
            throw new BizException("未找到待办任务");
        }
        return tasks.get(0);
    }

    private void saveRecord(WfProcessInstance wfInstance, Task task,
                            ApprovalAction action, String comment,
                            Long approverId, String approverName) {
        // 计算节点序号
        long count = recordService.count(new LambdaQueryWrapper<WfApprovalRecord>()
                .eq(WfApprovalRecord::getInstanceId, wfInstance.getId()));

        WfApprovalRecord record = new WfApprovalRecord();
        record.setInstanceId(wfInstance.getId());
        record.setFlowableTaskId(task.getId());
        record.setNodeName(task.getName() != null ? task.getName() : "审批节点");
        record.setNodeOrder((int) count + 1);
        record.setApproverId(approverId);
        record.setApproverName(approverName);
        record.setAction(action.getCode());
        record.setComment(comment);
        record.setCreatedAt(LocalDateTime.now());

        // 计算节点耗时
        if (task.getCreateTime() != null) {
            long ms = System.currentTimeMillis() - task.getCreateTime().getTime();
            record.setDurationMs(ms);
        }
        recordService.save(record);
    }

    private void finishInstance(WfProcessInstance wfInstance, ProcessStatus status, String comment) {
        wfInstance.setStatus(status.getCode());
        wfInstance.setResultComment(comment);
        wfInstance.setFinishedAt(LocalDateTime.now());
        wfInstance.setCurrentAssigneeId(null);
        wfInstance.setCurrentNodeName(null);
        if (wfInstance.getStartedAt() != null) {
            wfInstance.setDurationMs(Duration.between(wfInstance.getStartedAt(), wfInstance.getFinishedAt()).toMillis());
        }
        instanceService.updateById(wfInstance);
    }

    private void updateCurrentNode(WfProcessInstance wfInstance) {
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(wfInstance.getFlowableInstanceId()).list();
        if (!tasks.isEmpty()) {
            Task nextTask = tasks.get(0);
            wfInstance.setCurrentNodeName(nextTask.getName());
            if (nextTask.getAssignee() != null) {
                try {
                    wfInstance.setCurrentAssigneeId(Long.parseLong(nextTask.getAssignee()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        instanceService.updateById(wfInstance);
    }

    private void dispatchCallback(WfProcessInstance wfInstance, ProcessStatus status,
                                   String comment, Long approverId, String approverName) {
        ApprovalCallbackDTO callback = new ApprovalCallbackDTO();
        callback.setProcessInstanceId(wfInstance.getFlowableInstanceId());
        callback.setBusinessType(wfInstance.getBusinessType());
        callback.setBusinessId(wfInstance.getBusinessId());
        callback.setResult(status.getCode());
        callback.setComment(comment);
        callback.setApproverId(approverId);
        callback.setApproverName(approverName);
        callbackDispatcher.dispatch(callback);
    }
}
