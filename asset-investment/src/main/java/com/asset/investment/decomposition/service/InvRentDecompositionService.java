package com.asset.investment.decomposition.service;

import com.asset.investment.decomposition.entity.InvRentDecomposition;
import com.baomidou.mybatisplus.extension.service.IService;

public interface InvRentDecompositionService extends IService<InvRentDecomposition> {

    /** 提交审批：草稿(0)/驳回(3) → 审批中(1) */
    void submitApproval(Long id);

    /** 审批回调：审批中(1) → 通过(2)/驳回(3) */
    void handleApprovalCallback(Long id, boolean approved);
}
