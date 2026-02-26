package com.asset.operation.revenue.service.impl;

import com.asset.common.exception.BizException;
import com.asset.operation.ledger.entity.OprContractLedger;
import com.asset.operation.ledger.entity.OprReceivablePlan;
import com.asset.operation.ledger.mapper.OprContractLedgerMapper;
import com.asset.operation.ledger.mapper.OprReceivablePlanMapper;
import com.asset.operation.revenue.dto.FloatingRentDetailVO;
import com.asset.operation.revenue.entity.OprFloatingRent;
import com.asset.operation.revenue.entity.OprFloatingRentTier;
import com.asset.operation.revenue.mapper.OprFloatingRentMapper;
import com.asset.operation.revenue.mapper.OprFloatingRentTierMapper;
import com.asset.operation.revenue.service.OprFloatingRentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * 浮动租金 Service 实现
 * 提供列表查询、详情（含阶梯）、手动生成应收计划功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OprFloatingRentServiceImpl extends ServiceImpl<OprFloatingRentMapper, OprFloatingRent>
        implements OprFloatingRentService {

    private final OprFloatingRentTierMapper floatingRentTierMapper;
    private final OprReceivablePlanMapper receivablePlanMapper;
    private final OprContractLedgerMapper ledgerMapper;

    private static final int CHARGE_TYPE_FIXED    = 2;
    private static final int CHARGE_TYPE_TIERED   = 3;
    private static final int CHARGE_TYPE_HIGHER_OF = 4;

    // ── 查询 ────────────────────────────────────────────────────

    @Override
    public IPage<OprFloatingRent> pageQuery(Long contractId, String calcMonth, int pageNum, int pageSize) {
        LambdaQueryWrapper<OprFloatingRent> wrapper = new LambdaQueryWrapper<OprFloatingRent>()
                .eq(contractId != null, OprFloatingRent::getContractId, contractId)
                .eq(calcMonth != null,  OprFloatingRent::getCalcMonth,  calcMonth)
                .orderByDesc(OprFloatingRent::getCalcMonth);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public FloatingRentDetailVO detail(Long id) {
        OprFloatingRent fr = getById(id);
        if (fr == null) throw new BizException("浮动租金记录不存在，id=" + id);

        FloatingRentDetailVO vo = new FloatingRentDetailVO();
        vo.setId(fr.getId());
        vo.setContractId(fr.getContractId());
        vo.setShopId(fr.getShopId());
        vo.setCalcMonth(fr.getCalcMonth());
        vo.setMonthlyRevenue(fr.getMonthlyRevenue());
        vo.setFixedRent(fr.getFixedRent());
        vo.setCommissionRate(fr.getCommissionRate());
        vo.setCommissionAmount(fr.getCommissionAmount());
        vo.setFloatingRent(fr.getFloatingRent());
        vo.setCalcFormula(fr.getCalcFormula());
        vo.setReceivableId(fr.getReceivableId());

        // 阶梯明细
        List<OprFloatingRentTier> tierEntities = floatingRentTierMapper.selectList(
                new LambdaQueryWrapper<OprFloatingRentTier>()
                        .eq(OprFloatingRentTier::getFloatingRentId, id)
                        .orderByAsc(OprFloatingRentTier::getTierNo));

        List<FloatingRentDetailVO.TierDetailVO> tiers = new ArrayList<>();
        for (OprFloatingRentTier t : tierEntities) {
            FloatingRentDetailVO.TierDetailVO tv = new FloatingRentDetailVO.TierDetailVO();
            tv.setTierNo(t.getTierNo());
            tv.setRevenueFrom(t.getRevenueFrom());
            tv.setRevenueTo(t.getRevenueTo());
            tv.setRate(t.getRate());
            tv.setTierAmount(t.getTierAmount());
            tiers.add(tv);
        }
        vo.setTiers(tiers);

        // 收费方式名称（根据阶梯数量判断）
        if (!tiers.isEmpty()) {
            vo.setChargeType(CHARGE_TYPE_TIERED);
            vo.setChargeTypeName("阶梯提成");
        } else if (fr.getFixedRent() != null && fr.getFixedRent().compareTo(BigDecimal.ZERO) > 0) {
            vo.setChargeType(CHARGE_TYPE_HIGHER_OF);
            vo.setChargeTypeName("两者取高");
        } else {
            vo.setChargeType(CHARGE_TYPE_FIXED);
            vo.setChargeTypeName("固定提成");
        }

        return vo;
    }

    // ── 生成应收 ────────────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateReceivable(Long id) {
        OprFloatingRent fr = getById(id);
        if (fr == null) throw new BizException("浮动租金记录不存在，id=" + id);
        if (fr.getReceivableId() != null) {
            throw new BizException("应收计划已生成（receivableId=" + fr.getReceivableId() + "），请勿重复操作");
        }
        if (fr.getFloatingRent() == null || fr.getFloatingRent().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("浮动租金金额为0，无需生成应收");
        }

        YearMonth ym = YearMonth.parse(fr.getCalcMonth());

        // 查询台账
        OprContractLedger ledger = ledgerMapper.selectOne(
                new LambdaQueryWrapper<OprContractLedger>()
                        .eq(OprContractLedger::getContractId, fr.getContractId())
                        .eq(OprContractLedger::getStatus, 0)
                        .last("LIMIT 1"));

        OprReceivablePlan plan = new OprReceivablePlan();
        plan.setContractId(fr.getContractId());
        plan.setLedgerId(ledger != null ? ledger.getId() : null);
        plan.setShopId(fr.getShopId());
        plan.setFeeName("浮动租金-" + fr.getCalcMonth());
        plan.setAmount(fr.getFloatingRent());
        plan.setReceivedAmount(BigDecimal.ZERO);
        plan.setBillingStart(ym.atDay(1));
        plan.setBillingEnd(ym.atEndOfMonth());
        plan.setDueDate(ym.atEndOfMonth());
        plan.setStatus(0);
        plan.setPushStatus(0);
        plan.setSourceType(3);
        plan.setVersion(1);
        receivablePlanMapper.insert(plan);

        // 回填
        fr.setReceivableId(plan.getId());
        updateById(fr);

        log.info("[浮动租金] 手动生成应收计划，floatingRentId={}, receivableId={}", id, plan.getId());
        return plan.getId();
    }
}
