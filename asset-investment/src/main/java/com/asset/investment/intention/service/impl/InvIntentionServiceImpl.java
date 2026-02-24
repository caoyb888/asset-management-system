package com.asset.investment.intention.service.impl;

import com.asset.common.exception.BizException;
import com.asset.investment.intention.dto.ApprovalCallbackDTO;
import com.asset.investment.intention.dto.IntentionQueryDTO;
import com.asset.investment.intention.dto.IntentionSaveDTO;
import com.asset.investment.intention.entity.InvIntention;
import com.asset.investment.intention.entity.InvIntentionShop;
import com.asset.investment.intention.mapper.InvIntentionMapper;
import com.asset.investment.intention.service.InvIntentionService;
import com.asset.investment.intention.service.InvIntentionShopService;
import com.asset.investment.common.enums.IntentionStatus;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 意向协议 Service 实现
 * 实现完整状态机校验和 CRUD 操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvIntentionServiceImpl extends ServiceImpl<InvIntentionMapper, InvIntention>
        implements InvIntentionService {

    private final InvIntentionShopService intentionShopService;

    // ====================================================
    // 查询
    // ====================================================

    @Override
    public IPage<InvIntention> pageQuery(IntentionQueryDTO query) {
        Page<InvIntention> page = new Page<>(query.getPageNum(), query.getPageSize());
        return baseMapper.pageQueryWithCondition(page, query);
    }

    // ====================================================
    // 新增
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createIntention(IntentionSaveDTO dto) {
        InvIntention intention = new InvIntention();
        copyFromDto(dto, intention);
        intention.setIntentionCode(generateCode());
        intention.setStatus(IntentionStatus.DRAFT.getCode());
        intention.setVersion(1);
        intention.setIsCurrent(1);
        save(intention);
        return intention.getId();
    }

    // ====================================================
    // 编辑
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateIntention(Long id, IntentionSaveDTO dto) {
        InvIntention existing = getAndCheck(id);
        checkEditable(existing.getStatus());
        copyFromDto(dto, existing);
        updateById(existing);
    }

    // ====================================================
    // 删除
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteIntention(Long id) {
        InvIntention existing = getAndCheck(id);
        int status = existing.getStatus();
        if (status == IntentionStatus.APPROVING.getCode()) {
            throw new BizException("审批中的意向协议不能删除，请先撤回审批");
        }
        if (status == IntentionStatus.CONVERTED.getCode()) {
            throw new BizException("已转合同的意向协议不能删除");
        }
        removeById(id);
    }

    // ====================================================
    // 暂存
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDraft(Long id, IntentionSaveDTO dto) {
        // 语义与 update 相同，保持接口独立便于后续扩展（如自动触发快照）
        updateIntention(id, dto);
    }

    // ====================================================
    // 发起审批：草稿(0)/驳回(3) → 审批中(1)
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitApproval(Long id) {
        InvIntention existing = getAndCheck(id);
        int status = existing.getStatus();
        if (status != IntentionStatus.DRAFT.getCode()
                && status != IntentionStatus.REJECTED.getCode()) {
            throw new BizException(String.format(
                    "当前状态[%s]不允许发起审批，仅草稿或驳回状态可提交",
                    IntentionStatus.of(status) != null
                            ? IntentionStatus.of(status).getDesc() : status));
        }
        // TODO: 调用审批引擎（任务 8.1），获取真实 approvalId
        String mockApprovalId = "MOCK-INV-" + id + "-" + System.currentTimeMillis();

        update(new LambdaUpdateWrapper<InvIntention>()
                .eq(InvIntention::getId, id)
                .set(InvIntention::getStatus, IntentionStatus.APPROVING.getCode())
                .set(InvIntention::getApprovalId, mockApprovalId));
    }

    // ====================================================
    // 审批回调：审批中(1) → 审批通过(2) 或 驳回(3)
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleApprovalCallback(Long id, ApprovalCallbackDTO dto) {
        InvIntention existing = getAndCheck(id);
        if (existing.getStatus() != IntentionStatus.APPROVING.getCode()) {
            throw new BizException("当前状态不在审批中，无法处理审批回调");
        }

        int newStatus = Boolean.TRUE.equals(dto.getApproved())
                ? IntentionStatus.APPROVED.getCode()
                : IntentionStatus.REJECTED.getCode();

        LambdaUpdateWrapper<InvIntention> uw = new LambdaUpdateWrapper<InvIntention>()
                .eq(InvIntention::getId, id)
                .set(InvIntention::getStatus, newStatus);
        if (dto.getApprovalId() != null) {
            uw.set(InvIntention::getApprovalId, dto.getApprovalId());
        }
        update(uw);

        // 审批通过：将关联商铺状态置为"意向中"（shop_status = 1）
        if (newStatus == IntentionStatus.APPROVED.getCode()) {
            updateShopStatusToIntention(id);
        }
    }

    // ====================================================
    // 私有方法
    // ====================================================

    /** 获取并校验意向协议存在 */
    private InvIntention getAndCheck(Long id) {
        InvIntention intention = getById(id);
        if (intention == null) {
            throw new BizException("意向协议不存在，ID=" + id);
        }
        return intention;
    }

    /** 校验当前状态是否允许编辑 */
    private void checkEditable(int status) {
        if (status != IntentionStatus.DRAFT.getCode()
                && status != IntentionStatus.REJECTED.getCode()) {
            String desc = IntentionStatus.of(status) != null
                    ? IntentionStatus.of(status).getDesc() : String.valueOf(status);
            throw new BizException(String.format("当前状态[%s]不允许修改，仅草稿或驳回状态可编辑", desc));
        }
    }

    /**
     * 从 DTO 复制字段到实体（不覆盖系统管控字段）
     * 明确列出字段，避免 BeanUtils 误覆盖 null 导致数据丢失
     */
    private void copyFromDto(IntentionSaveDTO dto, InvIntention target) {
        target.setIntentionName(dto.getIntentionName());
        target.setProjectId(dto.getProjectId());
        target.setMerchantId(dto.getMerchantId());
        target.setBrandId(dto.getBrandId());
        target.setSigningEntity(dto.getSigningEntity());
        target.setRentSchemeId(dto.getRentSchemeId());
        target.setDeliveryDate(dto.getDeliveryDate());
        target.setDecorationStart(dto.getDecorationStart());
        target.setDecorationEnd(dto.getDecorationEnd());
        target.setOpeningDate(dto.getOpeningDate());
        target.setContractStart(dto.getContractStart());
        target.setContractEnd(dto.getContractEnd());
        target.setPaymentCycle(dto.getPaymentCycle());
        target.setBillingMode(dto.getBillingMode());
        target.setAgreementText(dto.getAgreementText());
    }

    /**
     * 生成意向协议编号：INV + yyyyMM + 4位序号
     * 例：INV2026020001
     */
    private String generateCode() {
        String monthPrefix = "INV" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        // 查询当月已有编号数量（含已删除，防止重号）
        long count = count(new LambdaQueryWrapper<InvIntention>()
                .likeRight(InvIntention::getIntentionCode, monthPrefix));
        return monthPrefix + String.format("%04d", count + 1);
    }

    /**
     * 审批通过后，将关联商铺状态更新为"意向中"(shop_status=1)
     * 通过修改 inv_intention_shop 表中的 format_type 无法影响 biz_shop，
     * 此处直接调用 InvIntentionShopService 获取 shopId 列表，
     * 再发起跨模块调用（当前阶段用日志占位，任务 4.3 完整实现时接入基础数据服务）
     */
    private void updateShopStatusToIntention(Long intentionId) {
        List<InvIntentionShop> shops = intentionShopService.list(
                new LambdaQueryWrapper<InvIntentionShop>()
                        .eq(InvIntentionShop::getIntentionId, intentionId));
        if (shops.isEmpty()) {
            return;
        }
        // TODO：调用基础数据模块接口将商铺状态更新为"意向中"(shop_status=1)
        // 当前阶段记录日志，待任务 8.1 审批引擎集成时完善跨模块调用
        log.info("[意向协议审批通过] intentionId={}, 涉及商铺IDs={}",
                intentionId,
                shops.stream().map(InvIntentionShop::getShopId).toList());
    }
}
