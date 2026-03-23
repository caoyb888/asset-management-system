package com.asset.investment.decomposition.service.impl;

import com.asset.api.workflow.ApprovalService;
import com.asset.api.workflow.dto.ApprovalSubmitDTO;
import com.asset.common.exception.BizException;
import com.asset.investment.decomposition.entity.InvRentDecomposition;
import com.asset.investment.decomposition.mapper.InvRentDecompositionMapper;
import com.asset.investment.decomposition.service.InvRentDecompositionService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvRentDecompositionServiceImpl
        extends ServiceImpl<InvRentDecompositionMapper, InvRentDecomposition>
        implements InvRentDecompositionService {

    private final ApprovalService approvalService;

    @Override
    public void submitApproval(Long id) {
        InvRentDecomposition existing = getById(id);
        if (existing == null) throw new BizException("记录不存在");
        if (existing.getStatus() != 0 && existing.getStatus() != 3)
            throw new BizException("仅草稿或驳回状态可提交审批");

        ApprovalSubmitDTO submitDTO = new ApprovalSubmitDTO();
        submitDTO.setBusinessType("INV_RENT_DECOMP");
        submitDTO.setBusinessId(id);
        submitDTO.setTitle("租金分解审批-" + id);
        submitDTO.setProjectId(existing.getProjectId());
        String approvalId = approvalService.submit(submitDTO);

        update(new LambdaUpdateWrapper<InvRentDecomposition>()
                .eq(InvRentDecomposition::getId, id)
                .set(InvRentDecomposition::getStatus, 1)
                .set(InvRentDecomposition::getApprovalId, approvalId));
    }

    @Override
    public void handleApprovalCallback(Long id, int result, String comment) {
        InvRentDecomposition existing = getById(id);
        if (existing == null) throw new BizException("记录不存在");
        if (existing.getStatus() != 1) throw new BizException("当前状态不在审批中");
        // result: 2=通过, 3=驳回
        update(new LambdaUpdateWrapper<InvRentDecomposition>()
                .eq(InvRentDecomposition::getId, id)
                .set(InvRentDecomposition::getStatus, result == 2 ? 2 : 3));
    }
}
