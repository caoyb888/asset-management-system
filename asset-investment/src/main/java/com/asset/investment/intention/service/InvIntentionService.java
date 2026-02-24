package com.asset.investment.intention.service;

import com.asset.investment.intention.dto.ApprovalCallbackDTO;
import com.asset.investment.intention.dto.IntentionFeeItemDTO;
import com.asset.investment.intention.dto.IntentionFeeStageItemDTO;
import com.asset.investment.intention.dto.IntentionQueryDTO;
import com.asset.investment.intention.dto.IntentionSaveDTO;
import com.asset.investment.intention.dto.IntentionShopItemDTO;
import com.asset.investment.intention.entity.InvIntention;
import com.asset.investment.intention.entity.InvIntentionBilling;
import com.asset.investment.intention.entity.InvIntentionFee;
import com.asset.investment.intention.entity.InvIntentionFeeStage;
import com.asset.investment.intention.entity.InvIntentionShop;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 意向协议 Service
 * 涵盖任务 4.1（CRUD + 状态机）、任务 4.2（商铺/费项/计租阶段/费用生成/账期）和任务 4.3（审批回调）
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

    // ================================================================
    // 任务 4.2 — 商铺关联、费项配置、分铺计租
    // ================================================================

    /**
     * 批量保存商铺关联（替换全量，IA-03 商铺选择）
     * 仅草稿(0)/驳回(3)状态可操作
     */
    void saveShops(Long intentionId, List<IntentionShopItemDTO> shops);

    /**
     * 查询关联商铺列表
     */
    List<InvIntentionShop> listShops(Long intentionId);

    /**
     * 批量保存费项配置（替换全量，含 formula_params）
     * 仅草稿(0)/驳回(3)状态可操作
     */
    void saveFees(Long intentionId, List<IntentionFeeItemDTO> fees);

    /**
     * 查询费项列表
     */
    List<InvIntentionFee> listFees(Long intentionId);

    /**
     * 批量保存分铺计租阶段（替换全量，含 min_commission_amount）
     * 仅草稿(0)/驳回(3)状态可操作
     * 每条记录须含 intentionFeeId，支持一次提交多个费项的多个商铺阶段
     */
    void saveFeeStages(Long intentionId, List<IntentionFeeStageItemDTO> stages);

    /**
     * 查询当前意向的全部分铺计租阶段
     */
    List<InvIntentionFeeStage> listFeeStages(Long intentionId);

    /**
     * 调用计算引擎，生成费用明细并更新 total_amount（任务 4.2 核心）
     * 计算逻辑：
     *   - 固定租金(1)：unitPrice × area × 月数
     *   - 固定提成(2)：按 formula_params.min_commission_amount × 月数（意向阶段无实际营业额）
     *   - 阶梯提成(3)：按 fee_stage 各阶段计算后求和
     *   - 两者取高(4)：max(固定部分, 提成保底部分)
     *   - 一次性(5)：formula_params.amount 或 unitPrice × area
     * 返回包含 totalAmount 和各费项明细的结果 Map
     */
    Map<String, Object> generateCost(Long intentionId);

    /**
     * 调用账期生成器，生成账期列表并持久化（替换全量）
     * 前提：费项已配置且已调用 generateCost
     * 账期按 paymentCycle + billingMode 拆分，首账期标记 billingType=1
     */
    List<InvIntentionBilling> generateBilling(Long intentionId);

    /**
     * 查询账期列表（按 billing_start 升序）
     */
    List<InvIntentionBilling> listBilling(Long intentionId);
}
