package com.asset.operation.engine;

import com.asset.common.exception.BizException;
import com.asset.operation.change.dto.ChangeImpactVO;
import com.asset.operation.change.entity.OprContractChange;
import com.asset.operation.change.entity.OprContractChangeDetail;
import com.asset.operation.change.entity.OprContractChangeSnapshot;
import com.asset.operation.change.entity.OprContractChangeType;
import com.asset.operation.change.mapper.OprContractChangeDetailMapper;
import com.asset.operation.change.mapper.OprContractChangeMapper;
import com.asset.operation.change.mapper.OprContractChangeSnapshotMapper;
import com.asset.operation.change.mapper.OprContractChangeTypeMapper;
import com.asset.operation.ledger.entity.OprContractLedger;
import com.asset.operation.ledger.entity.OprReceivablePlan;
import com.asset.operation.ledger.mapper.OprContractLedgerMapper;
import com.asset.operation.ledger.mapper.OprReceivablePlanMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 应收重算引擎
 * 变更审批通过后，根据变更类型对已有应收计划执行：
 *   - 已推送应收：红冲（status=3）+ 新增（version递增）
 *   - 未推送应收：原地更新或删除重建
 *
 * 支持变更类型：
 *   RENT/FEE  → 按比例重算未到期应收金额
 *   TERM      → 新增/删除账期（延长/缩短合同期）
 *   AREA      → 按面积比例重算所有费项
 *   BRAND/TENANT/COMPANY/CLAUSE → 仅更新合同信息字段，不影响应收
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReceivableRecalculateEngine {

    private final OprContractChangeMapper changeMapper;
    private final OprContractChangeTypeMapper changeTypeMapper;
    private final OprContractChangeDetailMapper changeDetailMapper;
    private final OprContractChangeSnapshotMapper changeSnapshotMapper;
    private final OprContractLedgerMapper ledgerMapper;
    private final OprReceivablePlanMapper receivablePlanMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 预览变更影响（不执行实际修改）
     *
     * @param change 变更单
     * @return 影响预览 VO
     */
    public ChangeImpactVO preview(OprContractChange change) {
        log.info("[应收重算] 预览变更影响，changeId={}", change.getId());

        List<String> typeCodes = getTypeCodes(change.getId());
        List<OprReceivablePlan> plans = getActivePlans(change.getLedgerId(), change.getEffectiveDate());
        Map<String, Object> changeFields = getChangeFields(change);

        ChangeImpactVO vo = new ChangeImpactVO();
        vo.setAffectedPlanCount(plans.size());
        BigDecimal originalTotal = plans.stream()
                .map(OprReceivablePlan::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setOriginalTotalAmount(originalTotal);

        // 计算预估新总金额
        BigDecimal newTotal = estimateNewTotal(plans, typeCodes, changeFields, originalTotal);
        vo.setNewTotalAmount(newTotal);
        vo.setAmountDiff(newTotal.subtract(originalTotal));

        // 构造字段对比
        List<Map<String, String>> comparisons = buildFieldComparisons(change, typeCodes, changeFields);
        vo.setFieldComparisons(comparisons);
        vo.setImpactDesc(buildImpactDesc(typeCodes, plans.size(), newTotal.subtract(originalTotal)));

        return vo;
    }

    /**
     * 执行应收重算（事务性）
     * 审批通过后调用
     */
    @Transactional(rollbackFor = Exception.class)
    public void execute(OprContractChange change) {
        log.info("[应收重算] 开始执行应收重算，changeId={}，ledgerId={}", change.getId(), change.getLedgerId());

        if (change.getLedgerId() == null) {
            log.warn("[应收重算] 变更单无关联台账，跳过应收重算，changeId={}", change.getId());
            return;
        }

        // 1. 保存变更前快照
        saveSnapshot(change);

        List<String> typeCodes = getTypeCodes(change.getId());
        Map<String, Object> changeFields = getChangeFields(change);

        // 2. 按变更类型分支处理
        boolean needsRecalculate = typeCodes.stream()
                .anyMatch(c -> Set.of("RENT", "FEE", "TERM", "AREA").contains(c));

        if (!needsRecalculate) {
            log.info("[应收重算] 变更类型不影响应收（BRAND/TENANT/COMPANY/CLAUSE），跳过重算，changeId={}", change.getId());
            // 写入变更明细字段记录（这些类型仅更新合同主表，字段记录由 Service 层在创建变更时填写）
            return;
        }

        // 3. 查询生效日期之后的未收应收计划
        List<OprReceivablePlan> plans = getActivePlans(change.getLedgerId(), change.getEffectiveDate());
        if (plans.isEmpty()) {
            log.info("[应收重算] 生效日期 {} 后无待收应收，无需重算", change.getEffectiveDate());
            return;
        }

        if (typeCodes.contains("TERM")) {
            recalculateForTermChange(change, plans, changeFields);
        } else if (typeCodes.contains("AREA")) {
            recalculateForAreaChange(change, plans, changeFields);
        } else if (typeCodes.contains("RENT") || typeCodes.contains("FEE")) {
            recalculateForRentChange(change, plans, changeFields);
        }

        log.info("[应收重算] 重算完成，changeId={}，处理 {} 条应收", change.getId(), plans.size());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 私有重算逻辑
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 租金/费项单价变更：按新单价比例调整未到期应收金额
     * changeFields 中携带：newRentAmount(BigDecimal), oldRentAmount(BigDecimal)
     */
    private void recalculateForRentChange(OprContractChange change,
                                           List<OprReceivablePlan> plans,
                                           Map<String, Object> changeFields) {
        BigDecimal newAmount = parseDecimal(changeFields.get("newRentAmount"));
        BigDecimal oldAmount = parseDecimal(changeFields.get("oldRentAmount"));

        if (newAmount == null || oldAmount == null || oldAmount.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("[应收重算-租金] 新旧金额参数缺失，降级为直接修改应收金额");
            // 降级：直接写入新金额
            if (newAmount != null) {
                for (OprReceivablePlan plan : plans) {
                    voidAndCreate(plan, newAmount, change.getId());
                }
            }
            return;
        }

        BigDecimal ratio = newAmount.divide(oldAmount, 10, RoundingMode.HALF_UP);
        for (OprReceivablePlan plan : plans) {
            BigDecimal recalculated = plan.getAmount().multiply(ratio).setScale(2, RoundingMode.HALF_UP);
            voidAndCreate(plan, recalculated, change.getId());
        }
        log.info("[应收重算-租金] 已按比例 {} 重算 {} 条应收", ratio.toPlainString(), plans.size());
    }

    /**
     * 租期变更：延长则新增账期，缩短则作废超出部分账期
     * changeFields 中携带：newContractEnd(String yyyy-MM-dd)
     */
    private void recalculateForTermChange(OprContractChange change,
                                           List<OprReceivablePlan> plans,
                                           Map<String, Object> changeFields) {
        Object newEndObj = changeFields.get("newContractEnd");
        if (newEndObj == null) {
            log.warn("[应收重算-租期] 无 newContractEnd 字段，跳过");
            return;
        }
        LocalDate newContractEnd = LocalDate.parse(newEndObj.toString().substring(0, 10));

        // 作废超出新合同结束日期的应收计划
        int voidCount = 0;
        for (OprReceivablePlan plan : plans) {
            if (plan.getBillingStart() != null && plan.getBillingStart().isAfter(newContractEnd)) {
                voidPlan(plan);
                voidCount++;
            }
        }
        log.info("[应收重算-租期] 作废 {} 条超出新合同期的应收", voidCount);

        // 更新台账合同结束日期
        if (change.getLedgerId() != null) {
            ledgerMapper.update(null, new LambdaUpdateWrapper<OprContractLedger>()
                    .eq(OprContractLedger::getId, change.getLedgerId())
                    .set(OprContractLedger::getContractEnd, newContractEnd));
        }
    }

    /**
     * 面积变更：按面积比例重算所有未到期应收金额
     * changeFields 中携带：newRentArea(BigDecimal), oldRentArea(BigDecimal)
     */
    private void recalculateForAreaChange(OprContractChange change,
                                           List<OprReceivablePlan> plans,
                                           Map<String, Object> changeFields) {
        BigDecimal newArea = parseDecimal(changeFields.get("newRentArea"));
        BigDecimal oldArea = parseDecimal(changeFields.get("oldRentArea"));

        if (newArea == null || oldArea == null || oldArea.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("[应收重算-面积] 新旧面积参数缺失，跳过重算");
            return;
        }
        BigDecimal ratio = newArea.divide(oldArea, 10, RoundingMode.HALF_UP);
        for (OprReceivablePlan plan : plans) {
            BigDecimal recalculated = plan.getAmount().multiply(ratio).setScale(2, RoundingMode.HALF_UP);
            voidAndCreate(plan, recalculated, change.getId());
        }
        log.info("[应收重算-面积] 已按面积比 {} 重算 {} 条应收", ratio.toPlainString(), plans.size());
    }

    /**
     * 已推送应收：红冲原记录 + 新建（version+1）
     * 未推送应收：直接更新金额
     */
    private void voidAndCreate(OprReceivablePlan plan, BigDecimal newAmount, Long changeId) {
        if (plan.getPushStatus() != null && plan.getPushStatus() == 1) {
            // 已推送：红冲原记录
            voidPlan(plan);
            // 创建新记录（version递增，source_type=2变更生成）
            OprReceivablePlan newPlan = new OprReceivablePlan();
            newPlan.setLedgerId(plan.getLedgerId());
            newPlan.setContractId(plan.getContractId());
            newPlan.setShopId(plan.getShopId());
            newPlan.setFeeItemId(plan.getFeeItemId());
            newPlan.setFeeName(plan.getFeeName());
            newPlan.setBillingStart(plan.getBillingStart());
            newPlan.setBillingEnd(plan.getBillingEnd());
            newPlan.setDueDate(plan.getDueDate());
            newPlan.setAmount(newAmount);
            newPlan.setReceivedAmount(BigDecimal.ZERO);
            newPlan.setStatus(0);
            newPlan.setPushStatus(0);  // 重新等待推送
            newPlan.setSourceType(2);  // 变更生成
            newPlan.setVersion(plan.getVersion() + 1);
            receivablePlanMapper.insert(newPlan);
        } else {
            // 未推送：原地更新金额
            receivablePlanMapper.update(null, new LambdaUpdateWrapper<OprReceivablePlan>()
                    .eq(OprReceivablePlan::getId, plan.getId())
                    .set(OprReceivablePlan::getAmount, newAmount)
                    .set(OprReceivablePlan::getSourceType, 2)
                    .set(OprReceivablePlan::getVersion, plan.getVersion() + 1));
        }
    }

    /** 作废应收计划 */
    private void voidPlan(OprReceivablePlan plan) {
        receivablePlanMapper.update(null, new LambdaUpdateWrapper<OprReceivablePlan>()
                .eq(OprReceivablePlan::getId, plan.getId())
                .set(OprReceivablePlan::getStatus, 3));  // 已作废
    }

    /** 保存变更前快照 */
    private void saveSnapshot(OprContractChange change) {
        try {
            // 快照：合同主表信息
            String contractSQL = """
                    SELECT c.* FROM inv_lease_contract c WHERE c.id = ? AND c.is_deleted = 0 LIMIT 1
                    """;
            List<Map<String, Object>> contractData = jdbcTemplate.queryForList(contractSQL, change.getContractId());
            if (!contractData.isEmpty()) {
                OprContractChangeSnapshot snapshot = new OprContractChangeSnapshot();
                snapshot.setChangeId(change.getId());
                snapshot.setSnapshotType(1); // 合同主表
                snapshot.setSnapshotData(objectMapper.writeValueAsString(contractData.get(0)));
                changeSnapshotMapper.insert(snapshot);
            }

            // 快照：应收计划
            if (change.getLedgerId() != null) {
                List<OprReceivablePlan> plans = getActivePlans(change.getLedgerId(), null);
                if (!plans.isEmpty()) {
                    OprContractChangeSnapshot planSnapshot = new OprContractChangeSnapshot();
                    planSnapshot.setChangeId(change.getId());
                    planSnapshot.setSnapshotType(3); // 应收计划
                    planSnapshot.setSnapshotData(objectMapper.writeValueAsString(plans));
                    changeSnapshotMapper.insert(planSnapshot);
                }
            }
        } catch (Exception e) {
            log.error("[应收重算] 保存快照失败，changeId={}，原因：{}", change.getId(), e.getMessage());
            // 快照失败不影响主流程
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 辅助方法
    // ─────────────────────────────────────────────────────────────────────────

    /** 查询变更单的变更类型编码列表 */
    private List<String> getTypeCodes(Long changeId) {
        return changeTypeMapper.selectList(new LambdaQueryWrapper<OprContractChangeType>()
                .eq(OprContractChangeType::getChangeId, changeId)
                .eq(OprContractChangeType::getIsDeleted, 0))
                .stream()
                .map(OprContractChangeType::getChangeTypeCode)
                .collect(Collectors.toList());
    }

    /** 查询台账下生效日期之后的待收/部分收应收计划 */
    private List<OprReceivablePlan> getActivePlans(Long ledgerId, LocalDate effectiveDate) {
        if (ledgerId == null) return Collections.emptyList();
        LambdaQueryWrapper<OprReceivablePlan> wrapper = new LambdaQueryWrapper<OprReceivablePlan>()
                .eq(OprReceivablePlan::getLedgerId, ledgerId)
                .in(OprReceivablePlan::getStatus, 0, 1)   // 待收或部分收
                .orderByAsc(OprReceivablePlan::getDueDate);
        if (effectiveDate != null) {
            // 只处理账期开始日期 >= 生效日期的应收
            wrapper.ge(OprReceivablePlan::getBillingStart, effectiveDate);
        }
        return receivablePlanMapper.selectList(wrapper);
    }

    /** 从变更明细中提取动态字段（changeFields 以变更明细 fieldName→newValue 形式存储） */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getChangeFields(OprContractChange change) {
        // 先从 impactSummary 尝试取 changeFields
        if (change.getImpactSummary() != null) {
            try {
                Map<String, Object> m = objectMapper.treeToValue(change.getImpactSummary(), Map.class);
                if (m.containsKey("changeFields")) {
                    return (Map<String, Object>) m.get("changeFields");
                }
            } catch (Exception ignored) {}
        }
        // 否则从变更明细表构造 Map（fieldName → newValue）
        List<OprContractChangeDetail> details = changeDetailMapper.selectList(
                new LambdaQueryWrapper<OprContractChangeDetail>()
                        .eq(OprContractChangeDetail::getChangeId, change.getId())
                        .eq(OprContractChangeDetail::getIsDeleted, 0));
        Map<String, Object> fields = new HashMap<>();
        for (OprContractChangeDetail d : details) {
            fields.put(d.getFieldName(), d.getNewValue());
            // 同时放 old 值
            fields.put("old_" + d.getFieldName(), d.getOldValue());
        }
        return fields;
    }

    /** 预估重算后总金额 */
    private BigDecimal estimateNewTotal(List<OprReceivablePlan> plans, List<String> typeCodes,
                                        Map<String, Object> changeFields, BigDecimal originalTotal) {
        if (typeCodes.contains("TERM")) {
            // TERM 变更可能减少账期
            Object newEndObj = changeFields.get("newContractEnd");
            if (newEndObj != null) {
                LocalDate newEnd = LocalDate.parse(newEndObj.toString().substring(0, 10));
                BigDecimal sum = plans.stream()
                        .filter(p -> p.getBillingStart() == null || !p.getBillingStart().isAfter(newEnd))
                        .map(OprReceivablePlan::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                return sum;
            }
        }
        if (typeCodes.contains("AREA")) {
            BigDecimal newArea = parseDecimal(changeFields.get("newRentArea"));
            BigDecimal oldArea = parseDecimal(changeFields.get("oldRentArea"));
            if (newArea != null && oldArea != null && oldArea.compareTo(BigDecimal.ZERO) != 0) {
                return originalTotal.multiply(newArea.divide(oldArea, 10, RoundingMode.HALF_UP))
                        .setScale(2, RoundingMode.HALF_UP);
            }
        }
        if (typeCodes.contains("RENT") || typeCodes.contains("FEE")) {
            BigDecimal newAmount = parseDecimal(changeFields.get("newRentAmount"));
            BigDecimal oldAmount = parseDecimal(changeFields.get("oldRentAmount"));
            if (newAmount != null && oldAmount != null && oldAmount.compareTo(BigDecimal.ZERO) != 0) {
                return originalTotal.multiply(newAmount.divide(oldAmount, 10, RoundingMode.HALF_UP))
                        .setScale(2, RoundingMode.HALF_UP);
            }
        }
        return originalTotal;
    }

    /** 构建字段对比列表 */
    private List<Map<String, String>> buildFieldComparisons(OprContractChange change,
                                                              List<String> typeCodes,
                                                              Map<String, Object> changeFields) {
        List<OprContractChangeDetail> details = changeDetailMapper.selectList(
                new LambdaQueryWrapper<OprContractChangeDetail>()
                        .eq(OprContractChangeDetail::getChangeId, change.getId())
                        .eq(OprContractChangeDetail::getIsDeleted, 0));
        List<Map<String, String>> comparisons = new ArrayList<>();
        for (OprContractChangeDetail d : details) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("field", d.getFieldName());
            row.put("label", d.getFieldLabel());
            row.put("oldValue", d.getOldValue());
            row.put("newValue", d.getNewValue());
            comparisons.add(row);
        }
        return comparisons;
    }

    /** 构建影响描述文本 */
    private String buildImpactDesc(List<String> typeCodes, int planCount, BigDecimal diff) {
        StringBuilder sb = new StringBuilder();
        sb.append("变更类型：").append(String.join("、",
                typeCodes.stream().map(this::typeCodeDesc).collect(Collectors.toList())));
        sb.append("；受影响应收计划：").append(planCount).append(" 条");
        if (diff.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("；预计新增应收 ").append(diff.toPlainString()).append(" 元");
        } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
            sb.append("；预计减少应收 ").append(diff.abs().toPlainString()).append(" 元");
        }
        return sb.toString();
    }

    private String typeCodeDesc(String code) {
        Map<String, String> m = Map.of(
                "RENT", "租金变更", "FEE", "费项单价变更", "TERM", "租期变更",
                "AREA", "面积变更", "BRAND", "品牌变更", "TENANT", "租户变更",
                "COMPANY", "公司名称变更", "CLAUSE", "合同条款变更");
        return m.getOrDefault(code, code);
    }

    private BigDecimal parseDecimal(Object val) {
        if (val == null) return null;
        try { return new BigDecimal(val.toString()); } catch (Exception e) { return null; }
    }
}
