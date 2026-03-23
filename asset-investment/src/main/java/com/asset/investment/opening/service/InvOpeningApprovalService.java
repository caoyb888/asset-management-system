package com.asset.investment.opening.service;

import com.asset.investment.opening.entity.InvOpeningApproval;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface InvOpeningApprovalService extends IService<InvOpeningApproval> {

    IPage<InvOpeningApproval> pageQueryWithCondition(
            Page<InvOpeningApproval> page, Long projectId, Integer status, Long contractId);

    /** 提交审批：草稿(0)/驳回(3) → 审批中(1) */
    void submitApproval(Long id);

    /** 审批回调：审批中(1) → 通过(2)/驳回(3) */
    void handleApprovalCallback(Long id, int result, String comment);
}
