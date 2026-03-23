package com.asset.investment.opening.service.impl;

import com.asset.api.workflow.ApprovalService;
import com.asset.api.workflow.dto.ApprovalSubmitDTO;
import com.asset.common.exception.BizException;
import com.asset.investment.opening.entity.InvOpeningApproval;
import com.asset.investment.opening.mapper.InvOpeningApprovalMapper;
import com.asset.investment.opening.service.InvOpeningApprovalService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvOpeningApprovalServiceImpl extends ServiceImpl<InvOpeningApprovalMapper, InvOpeningApproval> implements InvOpeningApprovalService {

    private final ApprovalService approvalService;

    @Override
    public IPage<InvOpeningApproval> pageQueryWithCondition(
            Page<InvOpeningApproval> page, Long projectId, Integer status, Long contractId) {
        return baseMapper.pageQueryWithCondition(page, projectId, status, contractId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitApproval(Long id) {
        InvOpeningApproval oa = getById(id);
        if (oa == null) throw new BizException("开业审批记录不存在");
        if (oa.getStatus() != 0 && oa.getStatus() != 3)
            throw new BizException("仅草稿或驳回状态可提交审批");

        ApprovalSubmitDTO submitDTO = new ApprovalSubmitDTO();
        submitDTO.setBusinessType("INV_OPENING");
        submitDTO.setBusinessId(id);
        submitDTO.setTitle("开业审批-" + id);
        submitDTO.setProjectId(oa.getProjectId());
        String approvalId = approvalService.submit(submitDTO);

        update(new LambdaUpdateWrapper<InvOpeningApproval>()
                .eq(InvOpeningApproval::getId, id)
                .set(InvOpeningApproval::getStatus, 1)
                .set(InvOpeningApproval::getApprovalId, approvalId));
        log.info("[开业审批] 已提交审批, id={}, approvalId={}", id, approvalId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleApprovalCallback(Long id, int result, String comment) {
        InvOpeningApproval oa = getById(id);
        if (oa == null) throw new BizException("开业审批记录不存在");
        if (oa.getStatus() != 1) throw new BizException("当前状态不在审批中");
        // result: 2=通过, 3=驳回
        update(new LambdaUpdateWrapper<InvOpeningApproval>()
                .eq(InvOpeningApproval::getId, id)
                .set(InvOpeningApproval::getStatus, result == 2 ? 2 : 3));
        log.info("[开业审批] 审批回调, id={}, result={}", id, result);
    }
}
