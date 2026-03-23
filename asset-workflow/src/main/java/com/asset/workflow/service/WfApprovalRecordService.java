package com.asset.workflow.service;

import com.asset.api.workflow.dto.ApprovalRecordVO;
import com.asset.workflow.entity.WfApprovalRecord;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 审批记录服务
 */
public interface WfApprovalRecordService extends IService<WfApprovalRecord> {

    /** 查询流程实例的审批记录（按节点序号排序） */
    List<ApprovalRecordVO> listByInstanceId(Long instanceId);
}
