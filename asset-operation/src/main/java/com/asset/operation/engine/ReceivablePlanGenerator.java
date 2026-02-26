package com.asset.operation.engine;

import com.asset.common.exception.BizException;
import com.asset.operation.ledger.entity.OprContractLedger;
import com.asset.operation.ledger.entity.OprReceivablePlan;
import com.asset.operation.ledger.mapper.OprContractLedgerMapper;
import com.asset.operation.ledger.mapper.OprReceivablePlanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 应收计划生成器
 * 从 inv_lease_contract_billing（招商模块预计算账期）读取账期数据，
 * 转换写入 opr_receivable_plan。
 * 支持 source_type=1（合同生成），每次调用前校验台账状态。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReceivablePlanGenerator {

    private final OprContractLedgerMapper ledgerMapper;
    private final OprReceivablePlanMapper receivablePlanMapper;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 根据合同台账ID生成应收计划
     * 1. 从 inv_lease_contract_billing 读取已计算的账期
     * 2. 从 cfg_fee_item 读取费项名称
     * 3. 批量写入 opr_receivable_plan
     *
     * @param ledgerId 合同台账ID
     * @return 生成的应收计划数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int generate(Long ledgerId) {
        log.info("[应收计划生成器] 开始生成台账应收计划，ledgerId={}", ledgerId);

        // 1. 加载台账
        OprContractLedger ledger = ledgerMapper.selectById(ledgerId);
        if (ledger == null) {
            throw new BizException("台账不存在，id=" + ledgerId);
        }
        if (ledger.getReceivableStatus() != null && ledger.getReceivableStatus() == 1) {
            throw new BizException("应收计划已生成，请勿重复操作");
        }
        Long contractId = ledger.getContractId();

        // 2. 查询招商合同账期（inv_lease_contract_billing）
        String billingSQL = """
                SELECT b.id, b.contract_id, b.fee_item_id, b.billing_start, b.billing_end,
                       b.due_date, b.amount, b.billing_type
                FROM inv_lease_contract_billing b
                WHERE b.contract_id = ? AND b.is_deleted = 0
                ORDER BY b.fee_item_id, b.billing_start
                """;
        List<Map<String, Object>> billingRows = jdbcTemplate.queryForList(billingSQL, contractId);

        if (billingRows.isEmpty()) {
            log.warn("[应收计划生成器] 招商合同账期为空，contractId={}", contractId);
            throw new BizException("招商合同账期数据为空，无法生成应收计划（合同ID=" + contractId + "）");
        }

        // 3. 查询费项名称映射（cfg_fee_item）
        String feeItemSQL = """
                SELECT fi.id, fi.item_name
                FROM cfg_fee_item fi
                WHERE fi.is_deleted = 0
                """;
        Map<Long, String> feeNameMap = jdbcTemplate.queryForList(feeItemSQL)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> ((Number) row.get("id")).longValue(),
                        row -> (String) row.get("item_name")
                ));

        // 4. 查询合同第一个关联商铺（inv_lease_contract_shop）
        String shopSQL = """
                SELECT s.shop_id FROM inv_lease_contract_shop s
                WHERE s.contract_id = ? AND s.is_deleted = 0
                ORDER BY s.id LIMIT 1
                """;
        Long shopId = null;
        try {
            shopId = jdbcTemplate.queryForObject(shopSQL, Long.class, contractId);
        } catch (Exception ignored) {
            // 无商铺关联时不阻断
        }

        // 5. 构建 opr_receivable_plan 列表
        List<OprReceivablePlan> plans = new ArrayList<>(billingRows.size());
        for (Map<String, Object> row : billingRows) {
            Long feeItemId = ((Number) row.get("fee_item_id")).longValue();
            BigDecimal amount = new BigDecimal(row.get("amount").toString());

            // 跳过金额为0的账期（如免租期）
            if (amount.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            OprReceivablePlan plan = new OprReceivablePlan();
            plan.setLedgerId(ledgerId);
            plan.setContractId(contractId);
            plan.setShopId(shopId);
            plan.setFeeItemId(feeItemId);
            plan.setFeeName(feeNameMap.getOrDefault(feeItemId, "未知费项"));
            plan.setBillingStart(toLocalDate(row.get("billing_start")));
            plan.setBillingEnd(toLocalDate(row.get("billing_end")));
            plan.setDueDate(toLocalDate(row.get("due_date")));
            plan.setAmount(amount);
            plan.setReceivedAmount(BigDecimal.ZERO);
            plan.setStatus(0);       // 待收
            plan.setPushStatus(0);   // 未推送
            plan.setSourceType(1);   // 合同生成
            plan.setVersion(1);
            plans.add(plan);
        }

        if (plans.isEmpty()) {
            throw new BizException("合同账期全部为免租，无有效应收计划");
        }

        // 6. 批量插入
        for (OprReceivablePlan plan : plans) {
            receivablePlanMapper.insert(plan);
        }

        log.info("[应收计划生成器] 台账 {} 共生成 {} 条应收计划，contractId={}", ledgerId, plans.size(), contractId);
        return plans.size();
    }

    /** 将 JDBC 返回的日期对象转换为 LocalDate */
    private LocalDate toLocalDate(Object val) {
        if (val == null) return null;
        if (val instanceof LocalDate ld) return ld;
        if (val instanceof java.sql.Date d) return d.toLocalDate();
        if (val instanceof java.util.Date d) return d.toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        return LocalDate.parse(val.toString().substring(0, 10));
    }
}
