package com.asset.workflow.service.impl;

import com.asset.api.workflow.dto.ProcessInstanceVO;
import com.asset.api.workflow.dto.ProcessPageQuery;
import com.asset.api.workflow.enums.ApprovalBusinessType;
import com.asset.api.workflow.enums.ProcessStatus;
import com.asset.workflow.entity.WfProcessInstance;
import com.asset.workflow.mapper.WfProcessInstanceMapper;
import com.asset.workflow.service.WfProcessInstanceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WfProcessInstanceServiceImpl
        extends ServiceImpl<WfProcessInstanceMapper, WfProcessInstance>
        implements WfProcessInstanceService {

    @Override
    public WfProcessInstance getByBusiness(String businessType, Long businessId) {
        return getOne(new LambdaQueryWrapper<WfProcessInstance>()
                .eq(WfProcessInstance::getBusinessType, businessType)
                .eq(WfProcessInstance::getBusinessId, businessId)
                .last("LIMIT 1"));
    }

    @Override
    public IPage<ProcessInstanceVO> pageQuery(ProcessPageQuery query) {
        IPage<WfProcessInstance> page = page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<WfProcessInstance>()
                        .eq(StringUtils.hasText(query.getBusinessType()),
                                WfProcessInstance::getBusinessType, query.getBusinessType())
                        .like(StringUtils.hasText(query.getTitle()),
                                WfProcessInstance::getTitle, query.getTitle())
                        .eq(query.getStatus() != null,
                                WfProcessInstance::getStatus, query.getStatus())
                        .eq(query.getInitiatorId() != null,
                                WfProcessInstance::getInitiatorId, query.getInitiatorId())
                        .eq(query.getProjectId() != null,
                                WfProcessInstance::getProjectId, query.getProjectId())
                        .orderByDesc(WfProcessInstance::getCreatedAt));
        return page.convert(this::toVO);
    }

    @Override
    public Map<String, Object> statistics() {
        Map<String, Object> stats = new HashMap<>();
        // 总流程数
        long total = count();
        stats.put("total", total);
        // 各状态数量
        for (ProcessStatus s : ProcessStatus.values()) {
            long cnt = count(new LambdaQueryWrapper<WfProcessInstance>()
                    .eq(WfProcessInstance::getStatus, s.getCode()));
            stats.put(s.name().toLowerCase() + "Count", cnt);
        }
        // 平均耗时（仅已完成的）
        List<WfProcessInstance> finished = list(new LambdaQueryWrapper<WfProcessInstance>()
                .in(WfProcessInstance::getStatus, 2, 3)
                .isNotNull(WfProcessInstance::getDurationMs));
        double avgMs = finished.stream()
                .mapToLong(WfProcessInstance::getDurationMs)
                .average().orElse(0);
        stats.put("avgDurationMs", (long) avgMs);
        // 通过率
        long approved = count(new LambdaQueryWrapper<WfProcessInstance>()
                .eq(WfProcessInstance::getStatus, 2));
        long decided = count(new LambdaQueryWrapper<WfProcessInstance>()
                .in(WfProcessInstance::getStatus, 2, 3));
        stats.put("approvalRate", decided > 0 ? Math.round(approved * 100.0 / decided) : 0);
        return stats;
    }

    public ProcessInstanceVO toVO(WfProcessInstance inst) {
        ProcessInstanceVO vo = new ProcessInstanceVO();
        vo.setId(inst.getId());
        vo.setProcessKey(inst.getProcessKey());
        vo.setFlowableInstanceId(inst.getFlowableInstanceId());
        vo.setBusinessType(inst.getBusinessType());
        try {
            vo.setBusinessTypeName(ApprovalBusinessType.valueOf(inst.getBusinessType()).getLabel());
        } catch (Exception ignored) {
            vo.setBusinessTypeName(inst.getBusinessType());
        }
        vo.setBusinessId(inst.getBusinessId());
        vo.setTitle(inst.getTitle());
        vo.setInitiatorId(inst.getInitiatorId());
        vo.setInitiatorName(inst.getInitiatorName());
        vo.setProjectId(inst.getProjectId());
        vo.setCurrentAssigneeId(inst.getCurrentAssigneeId());
        vo.setCurrentNodeName(inst.getCurrentNodeName());
        vo.setStatus(inst.getStatus());
        try {
            vo.setStatusName(ProcessStatus.fromCode(inst.getStatus()).getLabel());
        } catch (Exception ignored) {
            vo.setStatusName("未知");
        }
        vo.setResultComment(inst.getResultComment());
        vo.setPriority(inst.getPriority());
        vo.setStartedAt(inst.getStartedAt());
        vo.setFinishedAt(inst.getFinishedAt());
        vo.setDurationMs(inst.getDurationMs());
        vo.setCreatedAt(inst.getCreatedAt());
        return vo;
    }
}
