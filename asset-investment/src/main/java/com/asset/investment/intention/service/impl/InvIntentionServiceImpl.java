package com.asset.investment.intention.service.impl;

import com.asset.common.exception.BizException;
import com.asset.investment.common.enums.ChargeType;
import com.asset.investment.common.enums.BillingMode;
import com.asset.investment.common.enums.IntentionStatus;
import com.asset.investment.common.enums.PaymentCycle;
import com.asset.investment.engine.BillingGenerator;
import com.asset.investment.engine.BillingPeriod;
import com.asset.investment.engine.FixedRentStrategy;
import com.asset.investment.engine.RentCalculateContext;
import com.asset.investment.engine.RentCalculateStrategyRouter;
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
import com.asset.investment.intention.mapper.InvIntentionMapper;
import com.asset.investment.intention.service.InvIntentionBillingService;
import com.asset.investment.intention.service.InvIntentionFeeService;
import com.asset.investment.intention.service.InvIntentionFeeStageService;
import com.asset.investment.intention.service.InvIntentionService;
import com.asset.investment.intention.service.InvIntentionShopService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final InvIntentionFeeService intentionFeeService;
    private final InvIntentionFeeStageService intentionFeeStageService;
    private final InvIntentionBillingService intentionBillingService;
    private final BillingGenerator billingGenerator;
    private final RentCalculateStrategyRouter strategyRouter;

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

    // ====================================================
    // 任务 4.2 — 商铺关联
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveShops(Long intentionId, List<IntentionShopItemDTO> shops) {
        InvIntention intention = getAndCheck(intentionId);
        checkEditable(intention.getStatus());
        // 全量替换：先逻辑删除已有商铺，再批量插入新商铺
        intentionShopService.remove(new LambdaQueryWrapper<InvIntentionShop>()
                .eq(InvIntentionShop::getIntentionId, intentionId));
        if (shops == null || shops.isEmpty()) {
            return;
        }
        List<InvIntentionShop> entities = shops.stream().map(dto -> {
            InvIntentionShop s = new InvIntentionShop();
            s.setIntentionId(intentionId);
            s.setShopId(dto.getShopId());
            s.setBuildingId(dto.getBuildingId());
            s.setFloorId(dto.getFloorId());
            s.setFormatType(dto.getFormatType());
            s.setArea(dto.getArea());
            return s;
        }).collect(Collectors.toList());
        intentionShopService.saveBatch(entities);
    }

    @Override
    public List<InvIntentionShop> listShops(Long intentionId) {
        return intentionShopService.list(new LambdaQueryWrapper<InvIntentionShop>()
                .eq(InvIntentionShop::getIntentionId, intentionId)
                .orderByAsc(InvIntentionShop::getId));
    }

    // ====================================================
    // 任务 4.2 — 费项配置
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveFees(Long intentionId, List<IntentionFeeItemDTO> fees) {
        InvIntention intention = getAndCheck(intentionId);
        checkEditable(intention.getStatus());
        // 全量替换：先删除所有费项（级联删除费项阶段）
        List<InvIntentionFee> oldFees = intentionFeeService.list(
                new LambdaQueryWrapper<InvIntentionFee>()
                        .eq(InvIntentionFee::getIntentionId, intentionId));
        if (!oldFees.isEmpty()) {
            List<Long> oldFeeIds = oldFees.stream().map(InvIntentionFee::getId).collect(Collectors.toList());
            // 先删除阶段数据
            intentionFeeStageService.remove(new LambdaQueryWrapper<InvIntentionFeeStage>()
                    .in(InvIntentionFeeStage::getIntentionFeeId, oldFeeIds));
            intentionFeeService.remove(new LambdaQueryWrapper<InvIntentionFee>()
                    .eq(InvIntentionFee::getIntentionId, intentionId));
        }
        if (fees == null || fees.isEmpty()) {
            return;
        }
        List<InvIntentionFee> entities = fees.stream().map(dto -> {
            InvIntentionFee f = new InvIntentionFee();
            f.setIntentionId(intentionId);
            f.setFeeItemId(dto.getFeeItemId());
            f.setFeeName(dto.getFeeName());
            f.setChargeType(dto.getChargeType());
            f.setUnitPrice(dto.getUnitPrice());
            f.setArea(dto.getArea());
            f.setStartDate(dto.getStartDate() != null ? dto.getStartDate() : intention.getContractStart());
            f.setEndDate(dto.getEndDate() != null ? dto.getEndDate() : intention.getContractEnd());
            f.setPeriodIndex(dto.getPeriodIndex());
            f.setFormulaParams(dto.getFormulaParams());
            return f;
        }).collect(Collectors.toList());
        intentionFeeService.saveBatch(entities);
    }

    @Override
    public List<InvIntentionFee> listFees(Long intentionId) {
        return intentionFeeService.list(new LambdaQueryWrapper<InvIntentionFee>()
                .eq(InvIntentionFee::getIntentionId, intentionId)
                .orderByAsc(InvIntentionFee::getId));
    }

    // ====================================================
    // 任务 4.2 — 分铺计租阶段
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveFeeStages(Long intentionId, List<IntentionFeeStageItemDTO> stages) {
        InvIntention intention = getAndCheck(intentionId);
        checkEditable(intention.getStatus());
        if (stages == null || stages.isEmpty()) {
            // 清空该意向所有费项的阶段
            List<InvIntentionFee> fees = intentionFeeService.list(
                    new LambdaQueryWrapper<InvIntentionFee>()
                            .eq(InvIntentionFee::getIntentionId, intentionId));
            if (!fees.isEmpty()) {
                List<Long> feeIds = fees.stream().map(InvIntentionFee::getId).collect(Collectors.toList());
                intentionFeeStageService.remove(new LambdaQueryWrapper<InvIntentionFeeStage>()
                        .in(InvIntentionFeeStage::getIntentionFeeId, feeIds));
            }
            return;
        }
        // 按 feeId 分组：只替换本次提交涉及到的费项的阶段
        Map<Long, List<IntentionFeeStageItemDTO>> grouped = stages.stream()
                .collect(Collectors.groupingBy(IntentionFeeStageItemDTO::getIntentionFeeId));
        for (Map.Entry<Long, List<IntentionFeeStageItemDTO>> entry : grouped.entrySet()) {
            Long feeId = entry.getKey();
            // 校验该费项属于本意向
            InvIntentionFee fee = intentionFeeService.getById(feeId);
            if (fee == null || !intentionId.equals(fee.getIntentionId())) {
                throw new BizException("费项ID=" + feeId + " 不属于该意向协议");
            }
            // 删除该费项的旧阶段
            intentionFeeStageService.remove(new LambdaQueryWrapper<InvIntentionFeeStage>()
                    .eq(InvIntentionFeeStage::getIntentionFeeId, feeId));
            // 插入新阶段
            List<InvIntentionFeeStage> stageEntities = entry.getValue().stream().map(dto -> {
                InvIntentionFeeStage s = new InvIntentionFeeStage();
                s.setIntentionFeeId(feeId);
                s.setShopId(dto.getShopId());
                s.setStageStart(dto.getStageStart());
                s.setStageEnd(dto.getStageEnd());
                s.setUnitPrice(dto.getUnitPrice());
                s.setCommissionRate(dto.getCommissionRate());
                s.setMinCommissionAmount(dto.getMinCommissionAmount());
                return s;
            }).collect(Collectors.toList());
            intentionFeeStageService.saveBatch(stageEntities);
        }
    }

    @Override
    public List<InvIntentionFeeStage> listFeeStages(Long intentionId) {
        // 查询该意向的所有费项ID，再查阶段
        List<InvIntentionFee> fees = intentionFeeService.list(
                new LambdaQueryWrapper<InvIntentionFee>()
                        .eq(InvIntentionFee::getIntentionId, intentionId));
        if (fees.isEmpty()) {
            return List.of();
        }
        List<Long> feeIds = fees.stream().map(InvIntentionFee::getId).collect(Collectors.toList());
        return intentionFeeStageService.list(new LambdaQueryWrapper<InvIntentionFeeStage>()
                .in(InvIntentionFeeStage::getIntentionFeeId, feeIds)
                .orderByAsc(InvIntentionFeeStage::getIntentionFeeId)
                .orderByAsc(InvIntentionFeeStage::getStageStart));
    }

    // ====================================================
    // 任务 4.2 — 生成费用（调用计算引擎）
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generateCost(Long intentionId) {
        InvIntention intention = getAndCheck(intentionId);
        List<InvIntentionFee> fees = intentionFeeService.list(
                new LambdaQueryWrapper<InvIntentionFee>()
                        .eq(InvIntentionFee::getIntentionId, intentionId));
        if (fees.isEmpty()) {
            throw new BizException("请先配置费项后再生成费用");
        }
        // 加载所有阶段数据（按费项ID分组，减少查询次数）
        List<Long> feeIds = fees.stream().map(InvIntentionFee::getId).collect(Collectors.toList());
        Map<Long, List<InvIntentionFeeStage>> stagesMap = intentionFeeStageService.list(
                new LambdaQueryWrapper<InvIntentionFeeStage>()
                        .in(InvIntentionFeeStage::getIntentionFeeId, feeIds)
                        .orderByAsc(InvIntentionFeeStage::getStageStart))
                .stream().collect(Collectors.groupingBy(InvIntentionFeeStage::getIntentionFeeId));

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (InvIntentionFee fee : fees) {
            BigDecimal feeAmount = calcFeeAmount(fee, intention, stagesMap.get(fee.getId()));
            fee.setAmount(feeAmount);
            intentionFeeService.updateById(fee);
            totalAmount = totalAmount.add(feeAmount);
        }
        // 更新意向总金额
        update(new LambdaUpdateWrapper<InvIntention>()
                .eq(InvIntention::getId, intentionId)
                .set(InvIntention::getTotalAmount, totalAmount));
        log.info("[生成费用] intentionId={}, totalAmount={}", intentionId, totalAmount);

        Map<String, Object> result = new HashMap<>();
        result.put("totalAmount", totalAmount);
        result.put("fees", fees);
        return result;
    }

    /**
     * 计算单个费项的总金额（覆盖5种收费方式）
     */
    private BigDecimal calcFeeAmount(InvIntentionFee fee, InvIntention intention,
                                     List<InvIntentionFeeStage> stages) {
        int chargeType = fee.getChargeType() != null ? fee.getChargeType() : ChargeType.FIXED.getCode();
        LocalDate start = fee.getStartDate() != null ? fee.getStartDate() : intention.getContractStart();
        LocalDate end = fee.getEndDate() != null ? fee.getEndDate() : intention.getContractEnd();

        if (start == null || end == null) {
            log.warn("[生成费用] 费项ID={} 缺少起止日期，金额置为0", fee.getId());
            return BigDecimal.ZERO;
        }

        // 阶梯提成(3) / 两者取高(4)：优先使用 fee_stage 计算
        if ((chargeType == ChargeType.STEP_COMMISSION.getCode()
                || chargeType == ChargeType.HIGHER_OF.getCode())
                && stages != null && !stages.isEmpty()) {
            BigDecimal stageTotal = BigDecimal.ZERO;
            for (InvIntentionFeeStage stage : stages) {
                RentCalculateContext ctx = buildStageContext(fee, stage, chargeType);
                BigDecimal stageAmount = strategyRouter.route(chargeTypeToBeanName(chargeType)).calculate(ctx);
                stage.setAmount(stageAmount);
                intentionFeeStageService.updateById(stage);
                stageTotal = stageTotal.add(stageAmount);
            }
            return stageTotal.setScale(2, RoundingMode.HALF_UP);
        }

        // 其他收费方式：直接用费项字段计算
        return switch (chargeType) {
            case 1 -> { // FIXED 固定租金：单价 × 面积 × 月数
                RentCalculateContext ctx = RentCalculateContext.builder()
                        .unitPrice(safeDecimal(fee.getUnitPrice()))
                        .area(safeDecimal(fee.getArea()))
                        .stageStart(start)
                        .stageEnd(end)
                        .build();
                yield strategyRouter.route("fixedRentStrategy").calculate(ctx);
            }
            case 5 -> { // ONE_TIME 一次性：formula_params.amount 或 unitPrice × area
                RentCalculateContext ctx = RentCalculateContext.builder()
                        .unitPrice(safeDecimal(fee.getUnitPrice()))
                        .area(safeDecimal(fee.getArea()))
                        .formulaParams(fee.getFormulaParams())
                        .build();
                yield strategyRouter.route("oneTimeStrategy").calculate(ctx);
            }
            case 2, 3 -> { // 固定提成/阶梯提成（无阶段配置时）：最低金额 × 月数（保底估算）
                BigDecimal minAmt = extractDecimalFromJson(fee.getFormulaParams(), "min_commission_amount");
                BigDecimal months = FixedRentStrategy.calcMonths(start, end);
                yield minAmt.multiply(months).setScale(2, RoundingMode.HALF_UP);
            }
            case 4 -> { // 两者取高（无阶段配置时）：固定部分 vs 保底，取大值
                BigDecimal months = FixedRentStrategy.calcMonths(start, end);
                BigDecimal fixedPart = safeDecimal(fee.getUnitPrice())
                        .multiply(safeDecimal(fee.getArea()))
                        .multiply(months);
                BigDecimal minAmt = extractDecimalFromJson(fee.getFormulaParams(), "min_commission_amount");
                BigDecimal commissionFloor = minAmt.multiply(months);
                yield fixedPart.max(commissionFloor).setScale(2, RoundingMode.HALF_UP);
            }
            default -> BigDecimal.ZERO;
        };
    }

    /** 按阶段构建计算上下文 */
    private RentCalculateContext buildStageContext(InvIntentionFee fee, InvIntentionFeeStage stage, int chargeType) {
        return RentCalculateContext.builder()
                .unitPrice(stage.getUnitPrice() != null ? stage.getUnitPrice() : safeDecimal(fee.getUnitPrice()))
                .area(safeDecimal(fee.getArea()))
                .commissionRate(stage.getCommissionRate())
                .minCommissionAmount(stage.getMinCommissionAmount())
                .stageStart(stage.getStageStart())
                .stageEnd(stage.getStageEnd())
                .formulaParams(fee.getFormulaParams())
                .build();
    }

    /** 收费方式 → 策略 Bean 名称 */
    private String chargeTypeToBeanName(int chargeType) {
        return switch (chargeType) {
            case 1 -> "fixedRentStrategy";
            case 2 -> "fixedCommissionStrategy";
            case 3 -> "stepCommissionStrategy";
            case 4 -> "higherOfStrategy";
            case 5 -> "oneTimeStrategy";
            default -> "fixedRentStrategy";
        };
    }

    // ====================================================
    // 任务 4.2 — 生成账期（调用账期生成器）
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<InvIntentionBilling> generateBilling(Long intentionId) {
        InvIntention intention = getAndCheck(intentionId);
        if (intention.getContractStart() == null || intention.getContractEnd() == null) {
            throw new BizException("请先设置合同起止日期");
        }
        if (intention.getPaymentCycle() == null || intention.getBillingMode() == null) {
            throw new BizException("请先设置支付周期和账期模式");
        }
        PaymentCycle cycle = PaymentCycle.of(intention.getPaymentCycle());
        BillingMode mode = BillingMode.of(intention.getBillingMode());
        if (cycle == null || mode == null) {
            throw new BizException("支付周期或账期模式配置无效");
        }

        List<InvIntentionFee> fees = intentionFeeService.list(
                new LambdaQueryWrapper<InvIntentionFee>()
                        .eq(InvIntentionFee::getIntentionId, intentionId));
        if (fees.isEmpty()) {
            throw new BizException("请先配置费项并生成费用后再生成账期");
        }

        // 全量替换：删除已有账期
        intentionBillingService.remove(new LambdaQueryWrapper<InvIntentionBilling>()
                .eq(InvIntentionBilling::getIntentionId, intentionId));

        List<InvIntentionBilling> allBillings = new ArrayList<>();
        for (InvIntentionFee fee : fees) {
            int chargeType = fee.getChargeType() != null ? fee.getChargeType() : ChargeType.FIXED.getCode();
            LocalDate feeStart = fee.getStartDate() != null ? fee.getStartDate() : intention.getContractStart();
            LocalDate feeEnd = fee.getEndDate() != null ? fee.getEndDate() : intention.getContractEnd();

            if (chargeType == ChargeType.ONE_TIME.getCode()) {
                // 一次性费用：单条账期记录
                InvIntentionBilling billing = new InvIntentionBilling();
                billing.setIntentionId(intentionId);
                billing.setFeeItemId(fee.getFeeItemId());
                billing.setBillingStart(feeStart);
                billing.setBillingEnd(feeEnd);
                billing.setDueDate(feeStart); // 合同开始时一次性缴清
                billing.setAmount(safeDecimal(fee.getAmount()));
                billing.setBillingType(1); // 首（且唯一）账期
                billing.setStatus(0);
                allBillings.add(billing);
            } else {
                // 周期性费用：按支付周期拆分账期
                List<BillingPeriod> periods = billingGenerator.generate(feeStart, feeEnd, cycle, mode);
                for (BillingPeriod period : periods) {
                    BigDecimal periodAmount;
                    if (chargeType == ChargeType.FIXED.getCode()) {
                        // 固定租金：按实际账期天数精确计算
                        RentCalculateContext ctx = RentCalculateContext.builder()
                                .unitPrice(safeDecimal(fee.getUnitPrice()))
                                .area(safeDecimal(fee.getArea()))
                                .stageStart(period.getBillingStart())
                                .stageEnd(period.getBillingEnd())
                                .build();
                        periodAmount = strategyRouter.route("fixedRentStrategy").calculate(ctx);
                    } else {
                        // 提成类：按总金额均摊（意向阶段无实际营业额，均摊为合理估算）
                        int size = periods.size();
                        periodAmount = size > 0
                                ? safeDecimal(fee.getAmount()).divide(BigDecimal.valueOf(size), 2, RoundingMode.HALF_UP)
                                : safeDecimal(fee.getAmount());
                    }
                    InvIntentionBilling billing = new InvIntentionBilling();
                    billing.setIntentionId(intentionId);
                    billing.setFeeItemId(fee.getFeeItemId());
                    billing.setBillingStart(period.getBillingStart());
                    billing.setBillingEnd(period.getBillingEnd());
                    billing.setDueDate(period.getDueDate());
                    billing.setAmount(periodAmount);
                    billing.setBillingType(period.getBillingType());
                    billing.setStatus(0);
                    allBillings.add(billing);
                }
            }
        }

        if (!allBillings.isEmpty()) {
            intentionBillingService.saveBatch(allBillings);
        }
        log.info("[生成账期] intentionId={}, 共{}条账期记录", intentionId, allBillings.size());
        return allBillings;
    }

    @Override
    public List<InvIntentionBilling> listBilling(Long intentionId) {
        return intentionBillingService.list(new LambdaQueryWrapper<InvIntentionBilling>()
                .eq(InvIntentionBilling::getIntentionId, intentionId)
                .orderByAsc(InvIntentionBilling::getBillingStart)
                .orderByAsc(InvIntentionBilling::getFeeItemId));
    }

    // ====================================================
    // 工具方法
    // ====================================================

    /** 安全获取 BigDecimal（null 转 ZERO） */
    private BigDecimal safeDecimal(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    /** 从 JSON 节点安全提取 BigDecimal 字段 */
    private BigDecimal extractDecimalFromJson(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(node.get(fieldName).asText());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
