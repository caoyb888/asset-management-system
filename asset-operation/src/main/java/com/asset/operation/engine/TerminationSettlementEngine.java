package com.asset.operation.engine;

import com.asset.common.exception.BizException;
import com.asset.operation.common.enums.TerminationType;
import com.asset.operation.ledger.entity.OprReceivablePlan;
import com.asset.operation.ledger.mapper.OprContractLedgerMapper;
import com.asset.operation.ledger.mapper.OprReceivablePlanMapper;
import com.asset.operation.termination.entity.OprContractTermination;
import com.asset.operation.termination.entity.OprTerminationSettlement;
import com.asset.operation.termination.mapper.OprContractTerminationMapper;
import com.asset.operation.termination.mapper.OprTerminationSettlementMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 解约清算引擎
 * 支持三种解约类型：
 *   1-到期自然终止：汇总所有未收应收，生成"未收租费"清算明细
 *   2-提前解约：计算违约金（剩余天数*日租金*penaltyRate）+ 未收租费
 *   3-重签解约：关联新合同ID，作废原合同剩余应收，清算金额=0
 *
 * execute()：事务性执行多表联动
 *   - opr_contract_termination status=2（已生效）
 *   - opr_contract_ledger status=2（已解约）
 *   - inv_lease_contract status=5（已解约）
 *   - biz_shop shop_status=0（空置）
 *   - opr_receivable_plan 待收→已作废
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TerminationSettlementEngine {

    private final OprContractTerminationMapper terminationMapper;
    private final OprTerminationSettlementMapper settlementMapper;
    private final OprReceivablePlanMapper receivablePlanMapper;
    private final OprContractLedgerMapper ledgerMapper;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 计算解约清算金额，结果写入 opr_termination_settlement 明细表，更新主表汇总字段
     *
     * @param terminationId 解约单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void calculateSettlement(Long terminationId) {
        log.info("[解约清算引擎] 开始计算，terminationId={}", terminationId);

        OprContractTermination termination = terminationMapper.selectById(terminationId);
        if (termination == null) {
            throw new BizException("解约单不存在，id=" + terminationId);
        }
        if (termination.getStatus() != 0 && termination.getStatus() != 3) {
            throw new BizException("仅草稿或驳回状态可重新计算清算");
        }

        // 删除旧的清算明细
        settlementMapper.delete(new LambdaQueryWrapper<OprTerminationSettlement>()
                .eq(OprTerminationSettlement::getTerminationId, terminationId));

        // 查询关联台账ID（若未记录则查找）
        Long ledgerId = termination.getLedgerId();
        if (ledgerId == null) {
            try {
                ledgerId = jdbcTemplate.queryForObject(
                        "SELECT id FROM opr_contract_ledger WHERE contract_id=? AND is_deleted=0 LIMIT 1",
                        Long.class, termination.getContractId());
                // 回填台账ID
                terminationMapper.update(null, new LambdaUpdateWrapper<OprContractTermination>()
                        .eq(OprContractTermination::getId, terminationId)
                        .set(OprContractTermination::getLedgerId, ledgerId));
                termination.setLedgerId(ledgerId);
            } catch (Exception ignored) {}
        }

        TerminationType type = TerminationType.of(termination.getTerminationType());
        if (type == null) throw new BizException("解约类型无效：" + termination.getTerminationType());

        BigDecimal totalSettlement;
        switch (type) {
            case NATURAL -> totalSettlement = calcNatural(terminationId, ledgerId);
            case EARLY -> totalSettlement = calcEarly(termination, ledgerId);
            case RENEWAL -> totalSettlement = calcRenewal(terminationId, ledgerId, termination.getNewContractId());
            default -> throw new BizException("未支持的解约类型：" + type);
        }

        // 更新主表汇总字段
        terminationMapper.update(null, new LambdaUpdateWrapper<OprContractTermination>()
                .eq(OprContractTermination::getId, terminationId)
                .set(OprContractTermination::getSettlementAmount, totalSettlement)
                .set(OprContractTermination::getLedgerId, termination.getLedgerId()));

        log.info("[解约清算引擎] 计算完成，terminationId={}，清算总额={}", terminationId, totalSettlement);
    }

    /**
     * 执行解约（事务性多表联动），审批通过后调用
     *
     * @param terminationId 解约单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void execute(Long terminationId) {
        log.info("[解约清算引擎] 执行解约，terminationId={}", terminationId);

        OprContractTermination termination = terminationMapper.selectById(terminationId);
        if (termination == null) throw new BizException("解约单不存在，id=" + terminationId);
        if (termination.getStatus() != 1) throw new BizException("当前状态不是审批中，无法执行解约");

        Long ledgerId = termination.getLedgerId();

        // 1. 作废台账下所有待收/部分收应收计划
        if (ledgerId != null) {
            receivablePlanMapper.update(null, new LambdaUpdateWrapper<OprReceivablePlan>()
                    .eq(OprReceivablePlan::getLedgerId, ledgerId)
                    .in(OprReceivablePlan::getStatus, 0, 1)   // 待收/部分收
                    .set(OprReceivablePlan::getStatus, 3));    // 已作废

            // 2. 更新台账状态为已解约
            jdbcTemplate.update(
                    "UPDATE opr_contract_ledger SET status=2 WHERE id=? AND is_deleted=0",
                    ledgerId);
        }

        // 3. 更新招商合同状态为已解约（status=5）
        if (termination.getContractId() != null) {
            jdbcTemplate.update(
                    "UPDATE inv_lease_contract SET status=5 WHERE id=? AND is_deleted=0",
                    termination.getContractId());
        }

        // 4. 更新商铺状态为空置可租（shop_status=0）
        if (termination.getShopId() != null) {
            jdbcTemplate.update(
                    "UPDATE biz_shop SET shop_status=0 WHERE id=? AND is_deleted=0",
                    termination.getShopId());
        } else if (termination.getContractId() != null) {
            // 从合同查商铺
            try {
                List<Long> shopIds = jdbcTemplate.queryForList(
                        "SELECT shop_id FROM inv_lease_contract_shop WHERE contract_id=? AND is_deleted=0",
                        Long.class, termination.getContractId());
                for (Long shopId : shopIds) {
                    jdbcTemplate.update(
                            "UPDATE biz_shop SET shop_status=0 WHERE id=? AND is_deleted=0", shopId);
                }
            } catch (Exception e) {
                log.warn("[解约执行] 更新商铺状态失败：{}", e.getMessage());
            }
        }

        // 5. 更新解约单状态为已生效
        terminationMapper.update(null, new LambdaUpdateWrapper<OprContractTermination>()
                .eq(OprContractTermination::getId, terminationId)
                .set(OprContractTermination::getStatus, 2));

        log.info("[解约清算引擎] 解约执行完成，terminationId={}", terminationId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 三种清算策略
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 到期自然终止：汇总台账下所有待收应收金额，生成"未收租费"明细（应收）
     */
    private BigDecimal calcNatural(Long terminationId, Long ledgerId) {
        BigDecimal unsettled = sumUnpaidReceivable(ledgerId);

        if (unsettled.compareTo(BigDecimal.ZERO) > 0) {
            insertSettlementItem(terminationId, 1, "未收租费", unsettled, "到期解约待收汇总");
        }

        // 更新主表 unsettledAmount
        terminationMapper.update(null, new LambdaUpdateWrapper<OprContractTermination>()
                .eq(OprContractTermination::getId, terminationId)
                .set(OprContractTermination::getUnsettledAmount, unsettled));

        log.info("[解约清算-到期] terminationId={}，未收租费={}", terminationId, unsettled);
        return unsettled;
    }

    /**
     * 提前解约：违约金（日租金*剩余天数*penaltyRate）+ 未收租费
     * 违约金通过已存储在主表的 penaltyRate 或从合同租金推算
     */
    private BigDecimal calcEarly(OprContractTermination termination, Long ledgerId) {
        Long terminationId = termination.getId();

        // 1. 未收租费（正数，应收）
        BigDecimal unsettled = sumUnpaidReceivable(ledgerId);
        if (unsettled.compareTo(BigDecimal.ZERO) > 0) {
            insertSettlementItem(terminationId, 1, "未收租费", unsettled, "提前解约待收汇总");
        }

        // 2. 违约金计算：剩余天数 * 日均租金 * penaltyRate
        BigDecimal penaltyAmount = calcPenalty(termination, ledgerId);
        if (penaltyAmount.compareTo(BigDecimal.ZERO) > 0) {
            insertSettlementItem(terminationId, 2, "提前解约违约金", penaltyAmount, "按剩余天数折算");
        }

        // 更新主表
        BigDecimal total = unsettled.add(penaltyAmount);
        terminationMapper.update(null, new LambdaUpdateWrapper<OprContractTermination>()
                .eq(OprContractTermination::getId, terminationId)
                .set(OprContractTermination::getUnsettledAmount, unsettled)
                .set(OprContractTermination::getPenaltyAmount, penaltyAmount));

        log.info("[解约清算-提前] terminationId={}，未收租费={}，违约金={}，合计={}",
                terminationId, unsettled, penaltyAmount, total);
        return total;
    }

    /**
     * 重签解约：关联新合同，原合同剩余应收清零（清算金额=0），生成记录备档
     */
    private BigDecimal calcRenewal(Long terminationId, Long ledgerId, Long newContractId) {
        BigDecimal unsettled = sumUnpaidReceivable(ledgerId);

        // 重签解约：原合同剩余应收将在 execute 时作废，清算金额记录为0
        insertSettlementItem(terminationId, 4,
                "重签解约-应收清零",
                unsettled.negate(),  // 负数表示冲减（退让）
                newContractId != null ? "关联新合同ID:" + newContractId : "重签解约应收清零");

        terminationMapper.update(null, new LambdaUpdateWrapper<OprContractTermination>()
                .eq(OprContractTermination::getId, terminationId)
                .set(OprContractTermination::getUnsettledAmount, unsettled));

        log.info("[解约清算-重签] terminationId={}，冲减应收={}", terminationId, unsettled);
        return BigDecimal.ZERO;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 辅助方法
    // ─────────────────────────────────────────────────────────────────────────

    /** 汇总台账下待收/部分收的应收金额（实际金额 - 已收金额） */
    private BigDecimal sumUnpaidReceivable(Long ledgerId) {
        if (ledgerId == null) return BigDecimal.ZERO;
        List<OprReceivablePlan> plans = receivablePlanMapper.selectList(
                new LambdaQueryWrapper<OprReceivablePlan>()
                        .eq(OprReceivablePlan::getLedgerId, ledgerId)
                        .in(OprReceivablePlan::getStatus, 0, 1)  // 待收/部分收
                        .eq(OprReceivablePlan::getIsDeleted, 0));
        return plans.stream()
                .map(p -> p.getAmount().subtract(
                        p.getReceivedAmount() != null ? p.getReceivedAmount() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算违约金
     * 公式：日均租金 × 剩余天数 × penaltyRate
     * 日均租金 = 月租金 / 30
     * 剩余天数 = 合同到期日 - 解约日期
     */
    private BigDecimal calcPenalty(OprContractTermination termination, Long ledgerId) {
        try {
            // 获取合同结束日期
            LocalDate contractEnd = null;
            if (ledgerId != null) {
                contractEnd = jdbcTemplate.queryForObject(
                        "SELECT contract_end FROM opr_contract_ledger WHERE id=? AND is_deleted=0 LIMIT 1",
                        LocalDate.class, ledgerId);
            }
            if (contractEnd == null && termination.getContractId() != null) {
                contractEnd = jdbcTemplate.queryForObject(
                        "SELECT contract_end FROM inv_lease_contract WHERE id=? AND is_deleted=0 LIMIT 1",
                        LocalDate.class, termination.getContractId());
            }

            if (contractEnd == null) return BigDecimal.ZERO;

            long remainDays = ChronoUnit.DAYS.between(termination.getTerminationDate(), contractEnd);
            if (remainDays <= 0) return BigDecimal.ZERO;

            // 获取月租金（从合同主表）
            BigDecimal monthlyRent = null;
            if (termination.getContractId() != null) {
                try {
                    monthlyRent = jdbcTemplate.queryForObject(
                            "SELECT rent_amount FROM inv_lease_contract WHERE id=? AND is_deleted=0 LIMIT 1",
                            BigDecimal.class, termination.getContractId());
                } catch (Exception ignored) {}
            }
            // 降级：从应收计划推算月均租金
            if (monthlyRent == null && ledgerId != null) {
                try {
                    BigDecimal avg = jdbcTemplate.queryForObject(
                            "SELECT AVG(amount) FROM opr_receivable_plan WHERE ledger_id=? AND status IN(0,1) AND is_deleted=0",
                            BigDecimal.class, ledgerId);
                    monthlyRent = avg != null ? avg : BigDecimal.ZERO;
                } catch (Exception ignored) {
                    monthlyRent = BigDecimal.ZERO;
                }
            }
            if (monthlyRent == null || monthlyRent.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

            // 使用存储的 penaltyAmount（若已手动填写则直接使用）
            if (termination.getPenaltyAmount() != null
                    && termination.getPenaltyAmount().compareTo(BigDecimal.ZERO) > 0) {
                return termination.getPenaltyAmount();
            }

            // 默认 penaltyRate=0.3（30%）
            BigDecimal penaltyRate = new BigDecimal("0.30");

            BigDecimal dailyRent = monthlyRent.divide(BigDecimal.valueOf(30), 10, RoundingMode.HALF_UP);
            return dailyRent.multiply(BigDecimal.valueOf(remainDays))
                    .multiply(penaltyRate)
                    .setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.warn("[解约清算] 违约金计算失败：{}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /** 插入清算明细记录 */
    private void insertSettlementItem(Long terminationId, int itemType, String itemName,
                                      BigDecimal amount, String remark) {
        OprTerminationSettlement item = new OprTerminationSettlement();
        item.setTerminationId(terminationId);
        item.setItemType(itemType);
        item.setItemName(itemName);
        item.setAmount(amount);
        item.setRemark(remark);
        settlementMapper.insert(item);
    }
}
