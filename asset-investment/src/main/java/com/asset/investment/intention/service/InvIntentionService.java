package com.asset.investment.intention.service;

import com.asset.investment.intention.dto.ApprovalCallbackDTO;
import com.asset.investment.intention.dto.IntentionQueryDTO;
import com.asset.investment.intention.dto.IntentionSaveDTO;
import com.asset.investment.intention.entity.InvIntention;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 意向协议 Service
 * 涵盖任务 4.1（CRUD + 状态机）和任务 4.3（审批回调）
 */
public interface InvIntentionService extends IService<InvIntention> {

    /**
     * 多条件分页查询
     * 支持项目/状态/商家/品牌/楼栋/楼层/商铺/业态/关键词等筛选
     */
    IPage<InvIntention> pageQuery(IntentionQueryDTO query);

    /**
     * 新增意向协议（草稿状态，自动生成编号）
     *
     * @return 新记录ID
     */
    Long createIntention(IntentionSaveDTO dto);

    /**
     * 编辑意向协议
     * 仅草稿(0)或驳回(3)状态可修改
     */
    void updateIntention(Long id, IntentionSaveDTO dto);

    /**
     * 逻辑删除意向协议
     * 审批中(1)和已转合同(4)状态不可删除
     */
    void deleteIntention(Long id);

    /**
     * 暂存（显式保存草稿，等同于 update，语义上与提交前暂存对应）
     * 仅草稿(0)或驳回(3)状态可暂存
     */
    void saveDraft(Long id, IntentionSaveDTO dto);

    /**
     * 发起审批
     * 草稿(0) → 审批中(1)
     * 驳回(3) → 审批中(1)（重新发起）
     */
    void submitApproval(Long id);

    /**
     * 审批回调（任务 4.3）
     * 审批中(1) → 审批通过(2) 或 驳回(3)
     * 通过时联动更新关联商铺状态为"意向中"
     */
    void handleApprovalCallback(Long id, ApprovalCallbackDTO dto);
}
