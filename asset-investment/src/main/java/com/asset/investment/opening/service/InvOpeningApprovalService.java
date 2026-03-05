package com.asset.investment.opening.service;

import com.asset.investment.opening.entity.InvOpeningApproval;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface InvOpeningApprovalService extends IService<InvOpeningApproval> {

    IPage<InvOpeningApproval> pageQueryWithCondition(
            Page<InvOpeningApproval> page, Long projectId, Integer status, Long contractId);
}
