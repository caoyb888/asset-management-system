package com.asset.operation.engine;

import com.asset.common.exception.BizException;
import com.asset.operation.ledger.entity.OprContractLedger;
import com.asset.operation.ledger.entity.OprReceivablePlan;
import com.asset.operation.ledger.mapper.OprContractLedgerMapper;
import com.asset.operation.ledger.mapper.OprReceivablePlanMapper;
import com.asset.operation.revenue.entity.OprFloatingRent;
import com.asset.operation.revenue.entity.OprFloatingRentTier;
import com.asset.operation.revenue.mapper.OprFloatingRentMapper;
import com.asset.operation.revenue.mapper.OprFloatingRentTierMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 浮动租金计算引擎（策略模式）
 * <p>
 * 支持三种收费方式：
 * <ul>
 *   <li>charge_type=2 固定提成：月营业额 × 提成率</li>
 *   <li>charge_type=3 阶梯提成：按档位累进计算，每档明细写入 opr_floating_rent_tier</li>
 *   <li>charge_type=4 两者取高：固定租金 vs 提成金额，取较高值（差额计入浮动租金）</li>
 * </ul>
 * <p>
 * 前置校验：月度填报完整性检查（不完整则拒绝计算）
 * 计算结果：写入 opr_floating_rent + 生成 opr_receivable_plan（source_type=3）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FloatingRentCalculator {

    private final JdbcTemplate jdbcTemplate;
    private final OprFloatingRentMapper floatingRentMapper;
    private final OprFloatingRentTierMapper floatingRentTierMapper;
    private final OprReceivablePlanMapper receivablePlanMapper;
    private final OprContractLedgerMapper ledgerMapper;
    private final ObjectMapper objectMapper;

    // ── 内部结果封装 ────────────────────────────────────────────

    /** 策略计算结果 */
    private record CalcResult(
            BigDecimal fixedRent,
            BigDecimal commissionRate,
            BigDecimal commissionAmount,
            BigDecimal floatingRent,
            String calcFormula,
            List<OprFloatingRentTier> tiers
    ) {}

    /** 阶梯配置 */
    private record TierConfig(BigDecimal from, BigDecimal to, BigDecimal rate) {}

    // ── 主入口 ──────────────────────────────────────────────────

    /**
     * 计算指定合同指定月度的浮动租金
     *
     * @param contractId 合同ID
     * @param calcMonth  计算月份（YYYY-MM）
     * @return 生成的浮动租金记录ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long calculate(Long contractId, String calcMonth) {
        log.info("[浮动租金] 开始计算，contractId={}, calcMonth={}", contractId, calcMonth);

        // 1. 幂等检查：同一合同同月已计算则拒绝
        Integer existCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM opr_floating_rent WHERE contract_id=? AND calc_month=? AND is_deleted=0",
                Integer.class, contractId, calcMonth);
        if (existCount != null && existCount > 0) {
            throw new BizException("该合同本月浮动租金已计算，请勿重复操作（month=" + calcMonth + "）");
        }

        // 2. 月度营业额汇总 + 完整性校验
        YearMonth ym = YearMonth.parse(calcMonth);
        int totalDays = ym.lengthOfMonth();

        Map<String, Object> revenueRow = jdbcTemplate.queryForMap(
                "SELECT COUNT(*) AS filled_days, COALESCE(SUM(revenue_amount),0) AS total_revenue " +
                "FROM opr_revenue_report WHERE contract_id=? AND report_month=? AND is_deleted=0",
                contractId, calcMonth);
        int filledDays = ((Number) revenueRow.get("filled_days")).intValue();
        BigDecimal monthlyRevenue = new BigDecimal(revenueRow.get("total_revenue").toString());

        if (filledDays < totalDays) {
            throw new BizException(String.format(
                    "月度营业额数据不完整，%s月共 %d 天，已填报 %d 天，无法计算浮动租金",
                    calcMonth, totalDays, filledDays));
        }

        // 3. 查询合同提成类费项（charge_type IN 2,3,4）
        List<Map<String, Object>> fees = jdbcTemplate.queryForList(
                "SELECT f.id, f.charge_type, f.amount, f.formula_params " +
                "FROM inv_lease_contract_fee f " +
                "WHERE f.contract_id=? AND f.is_deleted=0 AND f.charge_type IN (2,3,4) " +
                "ORDER BY f.id LIMIT 1",
                contractId);
        if (fees.isEmpty()) {
            throw new BizException("未找到提成类费项（charge_type=2/3/4），contractId=" + contractId);
        }
        Map<String, Object> fee = fees.get(0);
        int chargeType = ((Number) fee.get("charge_type")).intValue();
        Long contractFeeId = ((Number) fee.get("id")).longValue();

        // 4. 查询当月适用的计租阶段（commission_rate / min_commission_amount）
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd   = ym.atEndOfMonth();
        List<Map<String, Object>> stages = jdbcTemplate.queryForList(
                "SELECT s.commission_rate, s.min_commission_amount, s.unit_price, s.amount " +
                "FROM inv_lease_contract_fee_stage s " +
                "WHERE s.contract_fee_id=? AND s.is_deleted=0 " +
                "  AND s.stage_start<=? AND s.stage_end>=? " +
                "ORDER BY s.stage_start LIMIT 1",
                contractFeeId, monthEnd, monthStart);

        // 5. 按策略计算
        CalcResult result = switch (chargeType) {
            case 2 -> calcFixed(monthlyRevenue, stages);
            case 3 -> calcTiered(monthlyRevenue, fee);
            case 4 -> calcHigherOf(monthlyRevenue, stages);
            default -> throw new BizException("不支持的收费方式：chargeType=" + chargeType);
        };

        // 6. 查询合同关联商铺
        Long shopId = queryShopId(contractId);

        // 7. 保存 opr_floating_rent
        OprFloatingRent fr = new OprFloatingRent();
        fr.setContractId(contractId);
        fr.setShopId(shopId);
        fr.setCalcMonth(calcMonth);
        fr.setMonthlyRevenue(monthlyRevenue);
        fr.setFixedRent(result.fixedRent());
        fr.setCommissionRate(result.commissionRate());
        fr.setCommissionAmount(result.commissionAmount());
        fr.setFloatingRent(result.floatingRent());
        fr.setCalcFormula(result.calcFormula());
        floatingRentMapper.insert(fr);

        // 8. 保存阶梯明细
        for (OprFloatingRentTier tier : result.tiers()) {
            tier.setFloatingRentId(fr.getId());
            floatingRentTierMapper.insert(tier);
        }

        // 9. 生成应收计划（source_type=3）
        if (fr.getFloatingRent() != null && fr.getFloatingRent().compareTo(BigDecimal.ZERO) > 0) {
            generateReceivablePlan(contractId, shopId, fr);
        }

        log.info("[浮动租金] 计算完成，floatingRentId={}, floatingRent={}", fr.getId(), fr.getFloatingRent());
        return fr.getId();
    }

    // ── 策略实现 ────────────────────────────────────────────────

    /**
     * 固定提成：月营业额 × 提成率
     */
    private CalcResult calcFixed(BigDecimal monthlyRevenue, List<Map<String, Object>> stages) {
        BigDecimal commissionRate = BigDecimal.ZERO;
        if (!stages.isEmpty() && stages.get(0).get("commission_rate") != null) {
            commissionRate = new BigDecimal(stages.get(0).get("commission_rate").toString());
        }
        BigDecimal commissionAmount = monthlyRevenue
                .multiply(commissionRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        String formula = String.format("固定提成：营业额 %.2f × 提成率 %.2f%% = %.2f",
                monthlyRevenue, commissionRate, commissionAmount);
        return new CalcResult(null, commissionRate, commissionAmount, commissionAmount, formula, List.of());
    }

    /**
     * 阶梯提成：按档位累进计算，每档明细写入 opr_floating_rent_tier
     * 阶梯配置从 formula_params JSON 中读取：
     * {"tiers":[{"from":0,"to":100000,"rate":5},{"from":100000,"to":null,"rate":8}]}
     */
    private CalcResult calcTiered(BigDecimal monthlyRevenue, Map<String, Object> fee) {
        List<TierConfig> tiers = parseTiers(fee.get("formula_params"));

        // 无配置时使用默认5%单档
        if (tiers.isEmpty()) {
            tiers = List.of(new TierConfig(BigDecimal.ZERO, null, BigDecimal.valueOf(5)));
        }

        BigDecimal totalCommission = BigDecimal.ZERO;
        List<OprFloatingRentTier> tierRecords = new ArrayList<>();
        BigDecimal remaining = monthlyRevenue;

        for (int i = 0; i < tiers.size(); i++) {
            TierConfig tc = tiers.get(i);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal tierBase;
            if (tc.to() != null) {
                BigDecimal bandWidth = tc.to().subtract(tc.from() != null ? tc.from() : BigDecimal.ZERO);
                tierBase = remaining.min(bandWidth);
            } else {
                tierBase = remaining;
            }

            BigDecimal tierAmount = tierBase
                    .multiply(tc.rate())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            totalCommission = totalCommission.add(tierAmount);
            remaining = remaining.subtract(tierBase);

            OprFloatingRentTier tr = new OprFloatingRentTier();
            tr.setTierNo(i + 1);
            tr.setRevenueFrom(tc.from());
            tr.setRevenueTo(tc.to());
            tr.setRate(tc.rate());
            tr.setTierAmount(tierAmount);
            tierRecords.add(tr);
        }

        String formula = String.format("阶梯提成：月营业额 %.2f，共 %d 档，合计提成 %.2f",
                monthlyRevenue, tierRecords.size(), totalCommission);
        return new CalcResult(null, null, totalCommission, totalCommission, formula, tierRecords);
    }

    /**
     * 两者取高：固定租金 vs 月营业额×提成率，取较高值
     * 固定租金来自 inv_lease_contract_fee_stage.min_commission_amount
     * 差额部分才算浮动租金（即超出固定租金的部分）
     */
    private CalcResult calcHigherOf(BigDecimal monthlyRevenue, List<Map<String, Object>> stages) {
        BigDecimal commissionRate = BigDecimal.ZERO;
        BigDecimal fixedRent      = BigDecimal.ZERO;

        if (!stages.isEmpty()) {
            Map<String, Object> stage = stages.get(0);
            if (stage.get("commission_rate") != null) {
                commissionRate = new BigDecimal(stage.get("commission_rate").toString());
            }
            // min_commission_amount 充当"固定保底租金"
            if (stage.get("min_commission_amount") != null) {
                fixedRent = new BigDecimal(stage.get("min_commission_amount").toString());
            }
        }

        BigDecimal commissionAmount = monthlyRevenue
                .multiply(commissionRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // 浮动租金 = max(固定租金, 提成金额) - 固定租金（即超出保底的差额）
        BigDecimal higher       = fixedRent.max(commissionAmount);
        BigDecimal floatingRent = higher.subtract(fixedRent).max(BigDecimal.ZERO);

        String formula = String.format(
                "两者取高：固定保底 %.2f，提成 %.2f × %.2f%% = %.2f，取高值 %.2f，浮动差额 %.2f",
                fixedRent, monthlyRevenue, commissionRate, commissionAmount, higher, floatingRent);

        return new CalcResult(fixedRent, commissionRate, commissionAmount, floatingRent, formula, List.of());
    }

    // ── 辅助方法 ────────────────────────────────────────────────

    /**
     * 解析 formula_params JSON 中的阶梯配置
     * 期望格式：{"tiers":[{"from":0,"to":100000,"rate":5},{"from":100000,"to":null,"rate":8}]}
     */
    @SuppressWarnings("unchecked")
    private List<TierConfig> parseTiers(Object formulaParamsObj) {
        List<TierConfig> result = new ArrayList<>();
        if (formulaParamsObj == null) return result;
        try {
            String json = formulaParamsObj.toString();
            Map<String, Object> root = objectMapper.readValue(json, new TypeReference<>() {});
            Object tiersObj = root.get("tiers");
            if (!(tiersObj instanceof List<?> tierList)) return result;
            for (Object item : tierList) {
                if (!(item instanceof Map<?, ?> tierMap)) continue;
                BigDecimal from = tierMap.get("from") != null
                        ? new BigDecimal(tierMap.get("from").toString()) : BigDecimal.ZERO;
                BigDecimal to   = tierMap.get("to") != null
                        ? new BigDecimal(tierMap.get("to").toString()) : null;
                BigDecimal rate = tierMap.get("rate") != null
                        ? new BigDecimal(tierMap.get("rate").toString()) : BigDecimal.ZERO;
                result.add(new TierConfig(from, to, rate));
            }
        } catch (Exception e) {
            log.warn("[浮动租金] formula_params 解析失败，使用默认阶梯，原始值={}", formulaParamsObj, e);
        }
        return result;
    }

    /** 查询合同第一个关联商铺ID */
    private Long queryShopId(Long contractId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT s.shop_id FROM inv_lease_contract_shop s " +
                    "WHERE s.contract_id=? AND s.is_deleted=0 ORDER BY s.id LIMIT 1",
                    Long.class, contractId);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 生成应收计划（source_type=3 浮动租金）
     * 应收日期设为当月最后一天，同时回填 opr_floating_rent.receivable_id
     */
    private void generateReceivablePlan(Long contractId, Long shopId, OprFloatingRent fr) {
        YearMonth ym = YearMonth.parse(fr.getCalcMonth());

        // 查询合同台账（用于填 ledger_id）
        OprContractLedger ledger = ledgerMapper.selectOne(
                new LambdaQueryWrapper<OprContractLedger>()
                        .eq(OprContractLedger::getContractId, contractId)
                        .eq(OprContractLedger::getStatus, 0)   // 进行中
                        .last("LIMIT 1"));

        OprReceivablePlan plan = new OprReceivablePlan();
        plan.setContractId(contractId);
        plan.setLedgerId(ledger != null ? ledger.getId() : null);
        plan.setShopId(shopId);
        plan.setFeeName("浮动租金-" + fr.getCalcMonth());
        plan.setAmount(fr.getFloatingRent());
        plan.setReceivedAmount(BigDecimal.ZERO);
        plan.setBillingStart(ym.atDay(1));
        plan.setBillingEnd(ym.atEndOfMonth());
        plan.setDueDate(ym.atEndOfMonth());
        plan.setStatus(0);       // 待收
        plan.setPushStatus(0);   // 未推送
        plan.setSourceType(3);   // 浮动租金
        plan.setVersion(1);
        receivablePlanMapper.insert(plan);

        // 回填 receivable_id
        fr.setReceivableId(plan.getId());
        floatingRentMapper.updateById(fr);

        log.info("[浮动租金] 生成应收计划，receivableId={}, amount={}", plan.getId(), plan.getAmount());
    }
}
