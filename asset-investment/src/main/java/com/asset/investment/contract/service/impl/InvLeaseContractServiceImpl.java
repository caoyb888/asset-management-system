package com.asset.investment.contract.service.impl;

import com.asset.common.exception.BizException;
import com.asset.investment.common.enums.BillingMode;
import com.asset.investment.common.enums.ChargeType;
import com.asset.investment.common.enums.ContractStatus;
import com.asset.investment.common.enums.IntentionStatus;
import com.asset.investment.common.enums.PaymentCycle;
import com.asset.investment.contract.dto.*;
import com.asset.investment.contract.entity.*;
import com.asset.investment.contract.mapper.InvLeaseContractMapper;
import com.asset.investment.contract.service.*;
import com.asset.investment.engine.BillingGenerator;
import com.asset.investment.engine.BillingPeriod;
import com.asset.investment.engine.FixedRentStrategy;
import com.asset.investment.engine.RentCalculateContext;
import com.asset.investment.engine.RentCalculateStrategyRouter;
import com.asset.investment.intention.entity.InvIntention;
import com.asset.investment.intention.entity.InvIntentionFee;
import com.asset.investment.intention.entity.InvIntentionFeeStage;
import com.asset.investment.intention.entity.InvIntentionShop;
import com.asset.investment.intention.service.InvIntentionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 招商合同 Service 实现
 * 实现完整状态机校验、意向转合同（Redisson 分布式锁）和 CRUD 操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvLeaseContractServiceImpl extends ServiceImpl<InvLeaseContractMapper, InvLeaseContract>
        implements InvLeaseContractService {

    private final InvLeaseContractShopService contractShopService;
    private final InvLeaseContractFeeService contractFeeService;
    private final InvLeaseContractFeeStageService contractFeeStageService;
    private final InvLeaseContractBillingService contractBillingService;
    private final InvLeaseContractVersionService contractVersionService;
    private final InvIntentionService intentionService;
    private final BillingGenerator billingGenerator;
    private final RentCalculateStrategyRouter strategyRouter;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    // ====================================================
    // 查询
    // ====================================================

    @Override
    public IPage<InvLeaseContract> pageQuery(ContractQueryDTO query) {
        LambdaQueryWrapper<InvLeaseContract> wrapper = new LambdaQueryWrapper<InvLeaseContract>()
                .eq(query.getProjectId() != null, InvLeaseContract::getProjectId, query.getProjectId())
                .eq(query.getStatus() != null, InvLeaseContract::getStatus, query.getStatus())
                .eq(query.getMerchantId() != null, InvLeaseContract::getMerchantId, query.getMerchantId())
                .and(query.getKeyword() != null && !query.getKeyword().isBlank(), w -> w
                        .like(InvLeaseContract::getContractName, query.getKeyword())
                        .or().like(InvLeaseContract::getContractCode, query.getKeyword()))
                .eq(InvLeaseContract::getIsCurrent, 1)
                .orderByDesc(InvLeaseContract::getCreatedAt);
        return page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    // ====================================================
    // 任务 5.1 — 意向转合同（分布式锁）
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long convertFromIntention(Long intentionId, ContractSaveDTO dto) {
        // 1. 查询并校验意向状态
        InvIntention intention = intentionService.getById(intentionId);
        if (intention == null) {
            throw new BizException("意向协议不存在，ID=" + intentionId);
        }
        if (intention.getStatus() != IntentionStatus.APPROVED.getCode()) {
            throw new BizException("仅审批通过的意向协议可转合同，当前状态=" + intention.getStatus());
        }

        // 2. 获取关联商铺
        List<InvIntentionShop> intentionShops = intentionService.listShops(intentionId);
        List<Long> shopIds = intentionShops.stream()
                .map(InvIntentionShop::getShopId).collect(Collectors.toList());

        // 3. 对每个商铺尝试获取分布式锁
        List<RLock> locks = new ArrayList<>();
        try {
            for (Long shopId : shopIds) {
                RLock lock = redissonClient.getLock("shop:contract:" + shopId);
                boolean locked = lock.tryLock(3, 30, TimeUnit.SECONDS);
                if (!locked) {
                    throw new BizException("商铺 " + shopId + " 正在被其他操作占用，请稍后重试");
                }
                locks.add(lock);
            }

            // 4. 双重校验：商铺无审批中或生效的合同
            if (!shopIds.isEmpty()) {
                List<InvLeaseContractShop> existingShops = contractShopService.list(
                        new LambdaQueryWrapper<InvLeaseContractShop>()
                                .in(InvLeaseContractShop::getShopId, shopIds));
                if (!existingShops.isEmpty()) {
                    Set<Long> existingContractIds = existingShops.stream()
                            .map(InvLeaseContractShop::getContractId).collect(Collectors.toSet());
                    long conflictCount = count(new LambdaQueryWrapper<InvLeaseContract>()
                            .in(InvLeaseContract::getId, existingContractIds)
                            .in(InvLeaseContract::getStatus,
                                    ContractStatus.APPROVING.getCode(),
                                    ContractStatus.EFFECTIVE.getCode()));
                    if (conflictCount > 0) {
                        throw new BizException("部分商铺已有审批中或生效的合同，无法转合同");
                    }
                }
            }

            // 5. 创建合同主记录（继承意向字段）
            InvLeaseContract contract = new InvLeaseContract();
            contract.setContractCode(generateCode());
            contract.setContractName(dto.getContractName());
            contract.setContractType(dto.getContractType());
            contract.setProjectId(intention.getProjectId());
            contract.setMerchantId(intention.getMerchantId());
            contract.setBrandId(intention.getBrandId());
            contract.setIntentionId(intentionId);
            contract.setSigningEntity(intention.getSigningEntity());
            contract.setRentSchemeId(intention.getRentSchemeId());
            contract.setDeliveryDate(intention.getDeliveryDate());
            contract.setDecorationStart(intention.getDecorationStart());
            contract.setDecorationEnd(intention.getDecorationEnd());
            contract.setOpeningDate(intention.getOpeningDate());
            contract.setContractStart(intention.getContractStart());
            contract.setContractEnd(intention.getContractEnd());
            contract.setPaymentCycle(intention.getPaymentCycle());
            contract.setBillingMode(intention.getBillingMode());
            contract.setTotalAmount(intention.getTotalAmount());
            contract.setStatus(ContractStatus.DRAFT.getCode());
            contract.setVersion(1);
            contract.setIsCurrent(1);
            save(contract);
            Long contractId = contract.getId();

            // 6. 复制商铺关联
            if (!intentionShops.isEmpty()) {
                List<InvLeaseContractShop> contractShops = intentionShops.stream().map(is -> {
                    InvLeaseContractShop cs = new InvLeaseContractShop();
                    cs.setContractId(contractId);
                    cs.setShopId(is.getShopId());
                    cs.setBuildingId(is.getBuildingId());
                    cs.setFloorId(is.getFloorId());
                    cs.setFormatType(is.getFormatType());
                    cs.setArea(is.getArea());
                    return cs;
                }).collect(Collectors.toList());
                contractShopService.saveBatch(contractShops);
            }

            // 7. 复制费项（记录 intentionFeeId → contractFeeId 映射）
            List<InvIntentionFee> intentionFees = intentionService.listFees(intentionId);
            Map<Long, Long> feeIdMapping = new HashMap<>();
            for (InvIntentionFee iFee : intentionFees) {
                InvLeaseContractFee cFee = new InvLeaseContractFee();
                cFee.setContractId(contractId);
                cFee.setFeeItemId(iFee.getFeeItemId());
                cFee.setFeeName(iFee.getFeeName());
                cFee.setChargeType(iFee.getChargeType());
                cFee.setUnitPrice(iFee.getUnitPrice());
                cFee.setArea(iFee.getArea());
                cFee.setAmount(iFee.getAmount());
                cFee.setStartDate(iFee.getStartDate());
                cFee.setEndDate(iFee.getEndDate());
                cFee.setPeriodIndex(iFee.getPeriodIndex());
                cFee.setFormulaParams(iFee.getFormulaParams());
                contractFeeService.save(cFee);
                feeIdMapping.put(iFee.getId(), cFee.getId());
            }

            // 8. 复制费项阶段（使用 feeIdMapping 转换费项ID）
            List<InvIntentionFeeStage> intentionStages = intentionService.listFeeStages(intentionId);
            if (!intentionStages.isEmpty()) {
                List<InvLeaseContractFeeStage> contractStages = new ArrayList<>();
                for (InvIntentionFeeStage iStage : intentionStages) {
                    Long contractFeeId = feeIdMapping.get(iStage.getIntentionFeeId());
                    if (contractFeeId == null) continue;
                    InvLeaseContractFeeStage cStage = new InvLeaseContractFeeStage();
                    cStage.setContractFeeId(contractFeeId);
                    cStage.setShopId(iStage.getShopId());
                    cStage.setStageStart(iStage.getStageStart());
                    cStage.setStageEnd(iStage.getStageEnd());
                    cStage.setUnitPrice(iStage.getUnitPrice());
                    cStage.setCommissionRate(iStage.getCommissionRate());
                    cStage.setMinCommissionAmount(iStage.getMinCommissionAmount());
                    cStage.setAmount(iStage.getAmount());
                    contractStages.add(cStage);
                }
                if (!contractStages.isEmpty()) {
                    contractFeeStageService.saveBatch(contractStages);
                }
            }

            // 9. 更新商铺状态为"在租"(1)——商铺已签约
            if (!shopIds.isEmpty()) {
                String placeholders = shopIds.stream().map(id -> "?").collect(Collectors.joining(","));
                jdbcTemplate.update(
                        "UPDATE biz_shop SET shop_status = 1 WHERE id IN (" + placeholders + ")",
                        shopIds.toArray());
                log.info("[意向转合同] 已更新商铺状态为在租，shopIds={}", shopIds);
            }

            // 10. 更新意向状态为"已转合同"(4)
            intentionService.update(new LambdaUpdateWrapper<InvIntention>()
                    .eq(InvIntention::getId, intentionId)
                    .set(InvIntention::getStatus, IntentionStatus.CONVERTED.getCode()));

            // 11. 写版本快照
            createSnapshot(contractId, "意向转合同");

            log.info("[意向转合同] intentionId={}, contractId={}, contractCode={}",
                    intentionId, contractId, contract.getContractCode());
            return contractId;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException("获取锁时发生中断，请重试");
        } finally {
            // 11. 释放所有已持有的锁
            for (RLock lock : locks) {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }

    // ====================================================
    // 新增合同
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createContract(ContractSaveDTO dto) {
        InvLeaseContract contract = new InvLeaseContract();
        copyFromDto(dto, contract);
        contract.setContractCode(generateCode());
        contract.setStatus(ContractStatus.DRAFT.getCode());
        contract.setVersion(1);
        contract.setIsCurrent(1);
        save(contract);
        return contract.getId();
    }

    // ====================================================
    // 编辑合同
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateContract(Long id, ContractSaveDTO dto) {
        InvLeaseContract existing = getAndCheck(id);
        checkEditable(existing.getStatus());
        copyFromDto(dto, existing);
        updateById(existing);
    }

    // ====================================================
    // 删除合同
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteContract(Long id) {
        InvLeaseContract existing = getAndCheck(id);
        int status = existing.getStatus();
        if (status == ContractStatus.APPROVING.getCode()) {
            throw new BizException("审批中的合同不能删除，请先撤回审批");
        }
        if (status == ContractStatus.EFFECTIVE.getCode()) {
            throw new BizException("已生效的合同不能删除");
        }
        removeById(id);
    }

    // ====================================================
    // 发起审批：草稿(0) → 审批中(1)
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitApproval(Long id) {
        InvLeaseContract existing = getAndCheck(id);
        if (existing.getStatus() != ContractStatus.DRAFT.getCode()) {
            String desc = ContractStatus.of(existing.getStatus()) != null
                    ? ContractStatus.of(existing.getStatus()).getDesc()
                    : String.valueOf(existing.getStatus());
            throw new BizException(String.format(
                    "当前状态[%s]不允许发起审批，仅草稿状态可提交", desc));
        }
        // TODO: 调用审批引擎（任务 8.1）
        String mockApprovalId = "MOCK-CONTRACT-" + id + "-" + System.currentTimeMillis();
        update(new LambdaUpdateWrapper<InvLeaseContract>()
                .eq(InvLeaseContract::getId, id)
                .set(InvLeaseContract::getStatus, ContractStatus.APPROVING.getCode())
                .set(InvLeaseContract::getApprovalId, mockApprovalId));
    }

    // ====================================================
    // 审批回调：审批中(1) → 生效(2) / 草稿(0)
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleApprovalCallback(Long id, ContractApprovalCallbackDTO dto) {
        InvLeaseContract existing = getAndCheck(id);
        if (existing.getStatus() != ContractStatus.APPROVING.getCode()) {
            throw new BizException("当前状态不在审批中，无法处理审批回调");
        }
        // 通过 → 生效(2)；驳回 → 回到草稿(0)重新编辑
        boolean approved = Boolean.TRUE.equals(dto.getApproved());
        int newStatus = approved ? ContractStatus.EFFECTIVE.getCode() : ContractStatus.DRAFT.getCode();
        LambdaUpdateWrapper<InvLeaseContract> uw = new LambdaUpdateWrapper<InvLeaseContract>()
                .eq(InvLeaseContract::getId, id)
                .set(InvLeaseContract::getStatus, newStatus);
        if (dto.getApprovalId() != null) {
            uw.set(InvLeaseContract::getApprovalId, dto.getApprovalId());
        }
        update(uw);

        // 审批通过时写版本快照（任务5.3：状态≠草稿时记录变更历史）
        if (approved) {
            createSnapshot(id, "审批通过，合同生效");
        }
    }

    // ====================================================
    // 更新合同状态（到期/终止等系统操作）
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        getAndCheck(id);
        update(new LambdaUpdateWrapper<InvLeaseContract>()
                .eq(InvLeaseContract::getId, id)
                .set(InvLeaseContract::getStatus, status));
    }

    // ====================================================
    // 商铺关联
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveShops(Long id, List<ContractShopItemDTO> shops) {
        InvLeaseContract contract = getAndCheck(id);
        checkEditable(contract.getStatus());
        contractShopService.remove(new LambdaQueryWrapper<InvLeaseContractShop>()
                .eq(InvLeaseContractShop::getContractId, id));
        if (shops == null || shops.isEmpty()) return;
        List<InvLeaseContractShop> entities = shops.stream().map(dto -> {
            InvLeaseContractShop s = new InvLeaseContractShop();
            s.setContractId(id);
            s.setShopId(dto.getShopId());
            s.setBuildingId(dto.getBuildingId());
            s.setFloorId(dto.getFloorId());
            s.setFormatType(dto.getFormatType());
            s.setArea(dto.getArea());
            s.setRentUnitPrice(dto.getRentUnitPrice());
            s.setPropertyUnitPrice(dto.getPropertyUnitPrice());
            return s;
        }).collect(Collectors.toList());
        contractShopService.saveBatch(entities);
    }

    @Override
    public List<InvLeaseContractShop> listShops(Long id) {
        return contractShopService.list(new LambdaQueryWrapper<InvLeaseContractShop>()
                .eq(InvLeaseContractShop::getContractId, id)
                .orderByAsc(InvLeaseContractShop::getId));
    }

    // ====================================================
    // 费项配置
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveFees(Long id, List<ContractFeeItemDTO> fees) {
        InvLeaseContract contract = getAndCheck(id);
        checkEditable(contract.getStatus());
        // 全量替换：先删除旧费项及其阶段
        List<InvLeaseContractFee> oldFees = contractFeeService.list(
                new LambdaQueryWrapper<InvLeaseContractFee>()
                        .eq(InvLeaseContractFee::getContractId, id));
        if (!oldFees.isEmpty()) {
            List<Long> oldFeeIds = oldFees.stream()
                    .map(InvLeaseContractFee::getId).collect(Collectors.toList());
            contractFeeStageService.remove(new LambdaQueryWrapper<InvLeaseContractFeeStage>()
                    .in(InvLeaseContractFeeStage::getContractFeeId, oldFeeIds));
            contractFeeService.remove(new LambdaQueryWrapper<InvLeaseContractFee>()
                    .eq(InvLeaseContractFee::getContractId, id));
        }
        if (fees == null || fees.isEmpty()) return;
        List<InvLeaseContractFee> entities = fees.stream().map(dto -> {
            InvLeaseContractFee f = new InvLeaseContractFee();
            f.setContractId(id);
            f.setFeeItemId(dto.getFeeItemId());
            f.setFeeName(dto.getFeeName());
            f.setChargeType(dto.getChargeType());
            f.setUnitPrice(dto.getUnitPrice());
            f.setArea(dto.getArea());
            f.setStartDate(dto.getStartDate() != null ? dto.getStartDate() : contract.getContractStart());
            f.setEndDate(dto.getEndDate() != null ? dto.getEndDate() : contract.getContractEnd());
            f.setPeriodIndex(dto.getPeriodIndex());
            f.setFormulaParams(dto.getFormulaParams());
            return f;
        }).collect(Collectors.toList());
        contractFeeService.saveBatch(entities);
    }

    @Override
    public List<InvLeaseContractFee> listFees(Long id) {
        return contractFeeService.list(new LambdaQueryWrapper<InvLeaseContractFee>()
                .eq(InvLeaseContractFee::getContractId, id)
                .orderByAsc(InvLeaseContractFee::getId));
    }

    // ====================================================
    // 分铺计租阶段
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveFeeStages(Long id, List<ContractFeeStageItemDTO> stages) {
        InvLeaseContract contract = getAndCheck(id);
        checkEditable(contract.getStatus());
        if (stages == null || stages.isEmpty()) {
            // 清空该合同所有费项的阶段
            List<InvLeaseContractFee> fees = contractFeeService.list(
                    new LambdaQueryWrapper<InvLeaseContractFee>()
                            .eq(InvLeaseContractFee::getContractId, id));
            if (!fees.isEmpty()) {
                List<Long> feeIds = fees.stream()
                        .map(InvLeaseContractFee::getId).collect(Collectors.toList());
                contractFeeStageService.remove(new LambdaQueryWrapper<InvLeaseContractFeeStage>()
                        .in(InvLeaseContractFeeStage::getContractFeeId, feeIds));
            }
            return;
        }
        // 按 contractFeeId 分组，逐费项替换阶段
        Map<Long, List<ContractFeeStageItemDTO>> grouped = stages.stream()
                .collect(Collectors.groupingBy(ContractFeeStageItemDTO::getContractFeeId));
        for (Map.Entry<Long, List<ContractFeeStageItemDTO>> entry : grouped.entrySet()) {
            Long feeId = entry.getKey();
            InvLeaseContractFee fee = contractFeeService.getById(feeId);
            if (fee == null || !id.equals(fee.getContractId())) {
                throw new BizException("费项ID=" + feeId + " 不属于该合同");
            }
            contractFeeStageService.remove(new LambdaQueryWrapper<InvLeaseContractFeeStage>()
                    .eq(InvLeaseContractFeeStage::getContractFeeId, feeId));
            List<InvLeaseContractFeeStage> stageEntities = entry.getValue().stream().map(dto -> {
                InvLeaseContractFeeStage s = new InvLeaseContractFeeStage();
                s.setContractFeeId(feeId);
                s.setShopId(dto.getShopId());
                s.setStageStart(dto.getStageStart());
                s.setStageEnd(dto.getStageEnd());
                s.setUnitPrice(dto.getUnitPrice());
                s.setCommissionRate(dto.getCommissionRate());
                s.setMinCommissionAmount(dto.getMinCommissionAmount());
                return s;
            }).collect(Collectors.toList());
            contractFeeStageService.saveBatch(stageEntities);
        }
    }

    @Override
    public List<InvLeaseContractFeeStage> listFeeStages(Long id) {
        List<InvLeaseContractFee> fees = contractFeeService.list(
                new LambdaQueryWrapper<InvLeaseContractFee>()
                        .eq(InvLeaseContractFee::getContractId, id));
        if (fees.isEmpty()) return List.of();
        List<Long> feeIds = fees.stream().map(InvLeaseContractFee::getId).collect(Collectors.toList());
        return contractFeeStageService.list(new LambdaQueryWrapper<InvLeaseContractFeeStage>()
                .in(InvLeaseContractFeeStage::getContractFeeId, feeIds)
                .orderByAsc(InvLeaseContractFeeStage::getContractFeeId)
                .orderByAsc(InvLeaseContractFeeStage::getStageStart));
    }

    // ====================================================
    // 费用生成（调用计算引擎）
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generateCost(Long id) {
        InvLeaseContract contract = getAndCheck(id);
        List<InvLeaseContractFee> fees = contractFeeService.list(
                new LambdaQueryWrapper<InvLeaseContractFee>()
                        .eq(InvLeaseContractFee::getContractId, id));
        if (fees.isEmpty()) {
            throw new BizException("请先配置费项后再生成费用");
        }
        List<Long> feeIds = fees.stream().map(InvLeaseContractFee::getId).collect(Collectors.toList());
        Map<Long, List<InvLeaseContractFeeStage>> stagesMap = contractFeeStageService.list(
                new LambdaQueryWrapper<InvLeaseContractFeeStage>()
                        .in(InvLeaseContractFeeStage::getContractFeeId, feeIds)
                        .orderByAsc(InvLeaseContractFeeStage::getStageStart))
                .stream().collect(Collectors.groupingBy(InvLeaseContractFeeStage::getContractFeeId));

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (InvLeaseContractFee fee : fees) {
            BigDecimal feeAmount = calcFeeAmount(fee, contract, stagesMap.get(fee.getId()));
            fee.setAmount(feeAmount);
            contractFeeService.updateById(fee);
            totalAmount = totalAmount.add(feeAmount);
        }
        update(new LambdaUpdateWrapper<InvLeaseContract>()
                .eq(InvLeaseContract::getId, id)
                .set(InvLeaseContract::getTotalAmount, totalAmount));
        log.info("[生成费用] contractId={}, totalAmount={}", id, totalAmount);

        Map<String, Object> result = new HashMap<>();
        result.put("totalAmount", totalAmount);
        result.put("fees", fees);
        return result;
    }

    // ====================================================
    // 账期生成
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<InvLeaseContractBilling> generateBilling(Long id) {
        InvLeaseContract contract = getAndCheck(id);
        if (contract.getContractStart() == null || contract.getContractEnd() == null) {
            throw new BizException("请先设置合同起止日期");
        }
        if (contract.getPaymentCycle() == null || contract.getBillingMode() == null) {
            throw new BizException("请先设置支付周期和账期模式");
        }
        PaymentCycle cycle = PaymentCycle.of(contract.getPaymentCycle());
        BillingMode mode = BillingMode.of(contract.getBillingMode());
        if (cycle == null || mode == null) {
            throw new BizException("支付周期或账期模式配置无效");
        }
        List<InvLeaseContractFee> fees = contractFeeService.list(
                new LambdaQueryWrapper<InvLeaseContractFee>()
                        .eq(InvLeaseContractFee::getContractId, id));
        if (fees.isEmpty()) {
            throw new BizException("请先配置费项并生成费用后再生成账期");
        }
        // 全量替换旧账期
        contractBillingService.remove(new LambdaQueryWrapper<InvLeaseContractBilling>()
                .eq(InvLeaseContractBilling::getContractId, id));

        List<InvLeaseContractBilling> allBillings = new ArrayList<>();
        for (InvLeaseContractFee fee : fees) {
            int chargeType = fee.getChargeType() != null ? fee.getChargeType() : ChargeType.FIXED.getCode();
            LocalDate feeStart = fee.getStartDate() != null ? fee.getStartDate() : contract.getContractStart();
            LocalDate feeEnd = fee.getEndDate() != null ? fee.getEndDate() : contract.getContractEnd();

            if (chargeType == ChargeType.ONE_TIME.getCode()) {
                // 一次性：单条账期
                InvLeaseContractBilling billing = new InvLeaseContractBilling();
                billing.setContractId(id);
                billing.setFeeItemId(fee.getFeeItemId());
                billing.setBillingStart(feeStart);
                billing.setBillingEnd(feeEnd);
                billing.setDueDate(feeStart);
                billing.setAmount(safeDecimal(fee.getAmount()));
                billing.setBillingType(1);
                billing.setStatus(0);
                allBillings.add(billing);
            } else {
                // 周期性：按支付周期拆分
                List<BillingPeriod> periods = billingGenerator.generate(feeStart, feeEnd, cycle, mode);
                for (BillingPeriod period : periods) {
                    BigDecimal periodAmount;
                    if (chargeType == ChargeType.FIXED.getCode()) {
                        RentCalculateContext ctx = RentCalculateContext.builder()
                                .unitPrice(safeDecimal(fee.getUnitPrice()))
                                .area(safeDecimal(fee.getArea()))
                                .stageStart(period.getBillingStart())
                                .stageEnd(period.getBillingEnd())
                                .build();
                        periodAmount = strategyRouter.route("fixedRentStrategy").calculate(ctx);
                    } else {
                        // 提成类：按总金额均摊
                        int size = periods.size();
                        periodAmount = size > 0
                                ? safeDecimal(fee.getAmount()).divide(BigDecimal.valueOf(size), 2, RoundingMode.HALF_UP)
                                : safeDecimal(fee.getAmount());
                    }
                    InvLeaseContractBilling billing = new InvLeaseContractBilling();
                    billing.setContractId(id);
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
            contractBillingService.saveBatch(allBillings);
        }
        log.info("[生成账期] contractId={}, 共{}条账期记录", id, allBillings.size());
        return allBillings;
    }

    @Override
    public List<InvLeaseContractBilling> listBilling(Long id) {
        return contractBillingService.list(new LambdaQueryWrapper<InvLeaseContractBilling>()
                .eq(InvLeaseContractBilling::getContractId, id)
                .orderByAsc(InvLeaseContractBilling::getBillingStart)
                .orderByAsc(InvLeaseContractBilling::getFeeItemId));
    }

    // ====================================================
    // 版本快照
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createSnapshot(Long contractId, String changeReason) {
        InvLeaseContract contract = getById(contractId);
        if (contract == null) return;
        InvLeaseContractVersion version = new InvLeaseContractVersion();
        version.setContractId(contractId);
        version.setVersion(contract.getVersion());
        version.setSnapshotData(objectMapper.valueToTree(contract));
        version.setChangeReason(changeReason);
        version.setCreatedAt(LocalDateTime.now());
        contractVersionService.save(version);
    }

    @Override
    public List<InvLeaseContractVersion> listVersions(Long contractId) {
        return contractVersionService.list(
                new LambdaQueryWrapper<InvLeaseContractVersion>()
                        .eq(InvLeaseContractVersion::getContractId, contractId)
                        .orderByDesc(InvLeaseContractVersion::getVersion)
        );
    }

    // ====================================================
    // 私有工具方法
    // ====================================================

    /** 获取合同并校验存在 */
    private InvLeaseContract getAndCheck(Long id) {
        InvLeaseContract contract = getById(id);
        if (contract == null) {
            throw new BizException("合同不存在，ID=" + id);
        }
        return contract;
    }

    /** 校验当前状态是否允许编辑（仅草稿状态可修改） */
    private void checkEditable(int status) {
        if (status != ContractStatus.DRAFT.getCode()) {
            String desc = ContractStatus.of(status) != null
                    ? ContractStatus.of(status).getDesc() : String.valueOf(status);
            throw new BizException(String.format("当前状态[%s]不允许修改，仅草稿状态可编辑", desc));
        }
    }

    /** 从 DTO 复制字段到实体（不覆盖系统管控字段） */
    private void copyFromDto(ContractSaveDTO dto, InvLeaseContract target) {
        target.setContractName(dto.getContractName());
        target.setContractType(dto.getContractType());
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
        target.setContractText(dto.getContractText());
    }

    /**
     * 生成合同编号：LC + yyyyMMdd + 6位流水号
     * 例：LC202602240001
     */
    private String generateCode() {
        String dayPrefix = "LC" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = count(new LambdaQueryWrapper<InvLeaseContract>()
                .likeRight(InvLeaseContract::getContractCode, dayPrefix));
        return dayPrefix + String.format("%06d", count + 1);
    }

    /**
     * 计算单个费项的总金额（覆盖5种收费方式）
     */
    private BigDecimal calcFeeAmount(InvLeaseContractFee fee, InvLeaseContract contract,
                                     List<InvLeaseContractFeeStage> stages) {
        int chargeType = fee.getChargeType() != null ? fee.getChargeType() : ChargeType.FIXED.getCode();
        LocalDate start = fee.getStartDate() != null ? fee.getStartDate() : contract.getContractStart();
        LocalDate end = fee.getEndDate() != null ? fee.getEndDate() : contract.getContractEnd();

        if (start == null || end == null) {
            log.warn("[生成费用] 合同费项ID={} 缺少起止日期，金额置为0", fee.getId());
            return BigDecimal.ZERO;
        }

        // 阶梯提成(3) / 两者取高(4)：优先使用 fee_stage 计算
        if ((chargeType == ChargeType.STEP_COMMISSION.getCode()
                || chargeType == ChargeType.HIGHER_OF.getCode())
                && stages != null && !stages.isEmpty()) {
            BigDecimal stageTotal = BigDecimal.ZERO;
            for (InvLeaseContractFeeStage stage : stages) {
                RentCalculateContext ctx = buildStageContext(fee, stage);
                BigDecimal stageAmount = strategyRouter.route(chargeTypeToBeanName(chargeType)).calculate(ctx);
                stage.setAmount(stageAmount);
                contractFeeStageService.updateById(stage);
                stageTotal = stageTotal.add(stageAmount);
            }
            return stageTotal.setScale(2, RoundingMode.HALF_UP);
        }

        return switch (chargeType) {
            case 1 -> { // 固定租金：单价 × 面积 × 月数
                RentCalculateContext ctx = RentCalculateContext.builder()
                        .unitPrice(safeDecimal(fee.getUnitPrice()))
                        .area(safeDecimal(fee.getArea()))
                        .stageStart(start)
                        .stageEnd(end)
                        .build();
                yield strategyRouter.route("fixedRentStrategy").calculate(ctx);
            }
            case 5 -> { // 一次性：formula_params.amount 或 unitPrice × area
                RentCalculateContext ctx = RentCalculateContext.builder()
                        .unitPrice(safeDecimal(fee.getUnitPrice()))
                        .area(safeDecimal(fee.getArea()))
                        .formulaParams(fee.getFormulaParams())
                        .build();
                yield strategyRouter.route("oneTimeStrategy").calculate(ctx);
            }
            case 2, 3 -> { // 提成类（无阶段时）：最低金额 × 月数（保底估算）
                BigDecimal minAmt = extractDecimalFromJson(fee.getFormulaParams(), "min_commission_amount");
                BigDecimal months = FixedRentStrategy.calcMonths(start, end);
                yield minAmt.multiply(months).setScale(2, RoundingMode.HALF_UP);
            }
            case 4 -> { // 两者取高（无阶段时）：固定 vs 保底，取大值
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
    private RentCalculateContext buildStageContext(InvLeaseContractFee fee, InvLeaseContractFeeStage stage) {
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

    /** 安全获取 BigDecimal（null 转 ZERO） */
    private BigDecimal safeDecimal(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    /** 从 JSON 节点安全提取 BigDecimal */
    private BigDecimal extractDecimalFromJson(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName)) return BigDecimal.ZERO;
        try {
            return new BigDecimal(node.get(fieldName).asText());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
