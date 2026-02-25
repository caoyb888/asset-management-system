package com.asset.investment.decomposition.service.impl;

import com.asset.common.exception.BizException;
import com.asset.investment.decomposition.entity.InvRentDecomposition;
import com.asset.investment.decomposition.mapper.InvRentDecompositionMapper;
import com.asset.investment.decomposition.service.InvRentDecompositionService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class InvRentDecompositionServiceImpl
        extends ServiceImpl<InvRentDecompositionMapper, InvRentDecomposition>
        implements InvRentDecompositionService {

    @Override
    public void submitApproval(Long id) {
        InvRentDecomposition existing = getById(id);
        if (existing == null) throw new BizException("记录不存在");
        if (existing.getStatus() != 0 && existing.getStatus() != 3)
            throw new BizException("仅草稿或驳回状态可提交审批");
        // 生成 Mock 审批ID（后续对接真实审批引擎时替换）
        String mockApprovalId = "MOCK-DECOMP-" + id + "-" + System.currentTimeMillis();
        update(new LambdaUpdateWrapper<InvRentDecomposition>()
                .eq(InvRentDecomposition::getId, id)
                .set(InvRentDecomposition::getStatus, 1)
                .set(InvRentDecomposition::getApprovalId, mockApprovalId));
    }

    @Override
    public void handleApprovalCallback(Long id, boolean approved) {
        InvRentDecomposition existing = getById(id);
        if (existing == null) throw new BizException("记录不存在");
        if (existing.getStatus() != 1) throw new BizException("当前状态不在审批中");
        update(new LambdaUpdateWrapper<InvRentDecomposition>()
                .eq(InvRentDecomposition::getId, id)
                .set(InvRentDecomposition::getStatus, approved ? 2 : 3));
    }
}
