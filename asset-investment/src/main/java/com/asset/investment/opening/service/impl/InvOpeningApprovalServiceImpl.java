package com.asset.investment.opening.service.impl;

import com.asset.investment.opening.entity.InvOpeningApproval;
import com.asset.investment.opening.mapper.InvOpeningApprovalMapper;
import com.asset.investment.opening.service.InvOpeningApprovalService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class InvOpeningApprovalServiceImpl extends ServiceImpl<InvOpeningApprovalMapper, InvOpeningApproval> implements InvOpeningApprovalService {

    @Override
    public IPage<InvOpeningApproval> pageQueryWithCondition(
            Page<InvOpeningApproval> page, Long projectId, Integer status, Long contractId) {
        return baseMapper.pageQueryWithCondition(page, projectId, status, contractId);
    }
}
