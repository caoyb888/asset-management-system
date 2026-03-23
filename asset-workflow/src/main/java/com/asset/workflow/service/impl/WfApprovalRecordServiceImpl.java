package com.asset.workflow.service.impl;

import com.asset.api.workflow.dto.ApprovalRecordVO;
import com.asset.api.workflow.enums.ApprovalAction;
import com.asset.workflow.entity.WfApprovalRecord;
import com.asset.workflow.mapper.WfApprovalRecordMapper;
import com.asset.workflow.service.WfApprovalRecordService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WfApprovalRecordServiceImpl
        extends ServiceImpl<WfApprovalRecordMapper, WfApprovalRecord>
        implements WfApprovalRecordService {

    @Override
    public List<ApprovalRecordVO> listByInstanceId(Long instanceId) {
        List<WfApprovalRecord> records = list(new LambdaQueryWrapper<WfApprovalRecord>()
                .eq(WfApprovalRecord::getInstanceId, instanceId)
                .orderByAsc(WfApprovalRecord::getNodeOrder)
                .orderByAsc(WfApprovalRecord::getCreatedAt));
        return records.stream().map(this::toVO).collect(Collectors.toList());
    }

    private ApprovalRecordVO toVO(WfApprovalRecord record) {
        ApprovalRecordVO vo = new ApprovalRecordVO();
        vo.setId(record.getId());
        vo.setInstanceId(record.getInstanceId());
        vo.setFlowableTaskId(record.getFlowableTaskId());
        vo.setNodeName(record.getNodeName());
        vo.setNodeOrder(record.getNodeOrder());
        vo.setApproverId(record.getApproverId());
        vo.setApproverName(record.getApproverName());
        vo.setAction(record.getAction());
        try {
            vo.setActionName(ApprovalAction.fromCode(record.getAction()).getLabel());
        } catch (Exception ignored) {
            vo.setActionName("未知");
        }
        vo.setComment(record.getComment());
        vo.setAttachmentUrls(record.getAttachmentUrls());
        vo.setDurationMs(record.getDurationMs());
        vo.setCreatedAt(record.getCreatedAt());
        return vo;
    }
}
