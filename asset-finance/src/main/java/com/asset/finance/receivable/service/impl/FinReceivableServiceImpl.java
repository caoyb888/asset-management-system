package com.asset.finance.receivable.service.impl;

import com.alibaba.excel.EasyExcel;
import com.asset.common.exception.BizException;
import com.asset.finance.common.adapter.OaApprovalAdapter;
import com.asset.finance.common.exception.FinBizException;
import com.asset.finance.common.exception.FinErrorCode;
import com.asset.finance.common.util.FinCodeGenerator;
import com.asset.finance.receivable.dto.*;
import com.asset.finance.receivable.entity.FinReceivable;
import com.asset.finance.receivable.entity.FinReceivableAdjustment;
import com.asset.finance.receivable.entity.FinReceivableDeduction;
import com.asset.finance.receivable.mapper.FinReceivableAdjustmentMapper;
import com.asset.finance.receivable.mapper.FinReceivableDeductionMapper;
import com.asset.finance.receivable.mapper.FinReceivableMapper;
import com.asset.finance.receivable.service.FinReceivableService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinReceivableServiceImpl extends ServiceImpl<FinReceivableMapper, FinReceivable>
        implements FinReceivableService {

    private final JdbcTemplate jdbcTemplate;
    private final FinReceivableDeductionMapper deductionMapper;
    private final FinReceivableAdjustmentMapper adjustmentMapper;
    private final FinCodeGenerator codeGenerator;
    private final OaApprovalAdapter oaApprovalAdapter;

    // ─── 分页查询 ───────────────────────────────────────────
    @Override
    public IPage<ReceivableDetailVO> pageQuery(ReceivableQueryDTO query) {
        LambdaQueryWrapper<FinReceivable> w = buildWrapper(query);
        int pageNum = query.getPageNum() != null ? query.getPageNum() : 1;
        int pageSize = query.getPageSize() != null ? query.getPageSize() : 20;
        IPage<FinReceivable> entityPage = page(new Page<>(pageNum, pageSize), w);
        return entityPage.convert(this::toDetailVO);
    }

    @Override
    public ReceivableDetailVO getDetailById(Long id) {
        FinReceivable e = getById(id);
        if (e == null) throw new BizException("应收记录不存在，id=" + id);
        return toDetailVO(e);
    }

    // ─── 汇总（按合同） ───────────────────────────────────────
    @Override
    public List<ReceivableSummaryVO> summaryByContract(ReceivableQueryDTO query) {
        LambdaQueryWrapper<FinReceivable> w = buildWrapper(query);
        List<FinReceivable> all = list(w);

        LocalDate today = LocalDate.now();
        Map<Long, List<FinReceivable>> byContract = all.stream()
                .filter(r -> r.getContractId() != null)
                .collect(Collectors.groupingBy(FinReceivable::getContractId));

        return byContract.entrySet().stream().map(entry -> {
            Long contractId = entry.getKey();
            List<FinReceivable> items = entry.getValue();
            ReceivableSummaryVO vo = new ReceivableSummaryVO();
            vo.setContractId(contractId);
            if (!items.isEmpty()) {
                vo.setProjectId(items.get(0).getProjectId());
                vo.setMerchantId(items.get(0).getMerchantId());
            }
            fillContractInfo(vo, contractId);
            vo.setTotalOriginal(sum(items, FinReceivable::getOriginalAmount));
            vo.setTotalActual(sum(items, FinReceivable::getActualAmount));
            vo.setTotalReceived(sum(items, FinReceivable::getReceivedAmount));
            vo.setTotalDeduction(sum(items, FinReceivable::getDeductionAmount));
            vo.setTotalOutstanding(sum(items, FinReceivable::getOutstandingAmount).max(BigDecimal.ZERO));

            // 逾期：due_date < today 且状态为待收/部分收
            List<FinReceivable> overdue = items.stream()
                    .filter(r -> r.getDueDate() != null && r.getDueDate().isBefore(today))
                    .filter(r -> r.getStatus() != null && r.getStatus() < 2)
                    .collect(Collectors.toList());
            vo.setOverdueCount(overdue.size());
            vo.setOverdueAmount(sum(overdue, FinReceivable::getOutstandingAmount).max(BigDecimal.ZERO));
            return vo;
        }).sorted(Comparator.comparing(ReceivableSummaryVO::getTotalOutstanding).reversed())
                .collect(Collectors.toList());
    }

    // ─── 欠费统计 ─────────────────────────────────────────────
    @Override
    public OverdueStatisticsVO overdueStatistics(Long projectId) {
        LocalDate today = LocalDate.now();
        // 查所有待收/部分收且已逾期
        LambdaQueryWrapper<FinReceivable> w = new LambdaQueryWrapper<FinReceivable>()
                .in(FinReceivable::getStatus, 0, 1)
                .lt(FinReceivable::getDueDate, today)
                .eq(projectId != null, FinReceivable::getProjectId, projectId);
        List<FinReceivable> overdue = list(w);

        OverdueStatisticsVO vo = new OverdueStatisticsVO();
        BigDecimal amt30 = BigDecimal.ZERO, amt30to90 = BigDecimal.ZERO, amtOver90 = BigDecimal.ZERO;
        for (FinReceivable r : overdue) {
            int days = (int) ChronoUnit.DAYS.between(r.getDueDate(), today);
            BigDecimal outstanding = r.getOutstandingAmount() != null
                    ? r.getOutstandingAmount().max(BigDecimal.ZERO) : BigDecimal.ZERO;
            if (days <= 30) amt30 = amt30.add(outstanding);
            else if (days <= 90) amt30to90 = amt30to90.add(outstanding);
            else amtOver90 = amtOver90.add(outstanding);
        }
        vo.setOverdue30Amount(amt30);
        vo.setOverdue30To90Amount(amt30to90);
        vo.setOverdueOver90Amount(amtOver90);
        vo.setTotalOverdueAmount(amt30.add(amt30to90).add(amtOver90));
        vo.setTotalOverdueCount(overdue.size());

        ReceivableQueryDTO q2 = new ReceivableQueryDTO();
        q2.setProjectId(projectId);
        q2.setOverdue(true);
        q2.setPageSize(Integer.MAX_VALUE);
        List<ReceivableSummaryVO> top10 = summaryByContract(q2).stream().limit(10).collect(Collectors.toList());
        vo.setTopDebtors(top10);

        return vo;
    }

    // ─── 导出 Excel ──────────────────────────────────────────
    @Override
    public void exportExcel(ReceivableQueryDTO query, HttpServletResponse response) {
        query.setPageNum(1);
        query.setPageSize(Integer.MAX_VALUE);
        IPage<ReceivableDetailVO> p = pageQuery(query);
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String fileName = URLEncoder.encode("应收明细_" +
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")), StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName + ".xlsx");
            EasyExcel.write(response.getOutputStream(), ReceivableDetailVO.class)
                    .sheet("应收明细").doWrite(p.getRecords());
        } catch (Exception e) {
            throw new BizException("导出失败：" + e.getMessage());
        }
    }

    // ─── 从营运计划同步（幂等：以 ledger_id + fee_item_id + billing_start 唯一） ─
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncFromPlan(Long planId) {
        try {
            Map<String, Object> plan = jdbcTemplate.queryForMap(
                    "SELECT * FROM opr_receivable_plan WHERE id=? AND is_deleted=0 LIMIT 1", planId);
            Long ledgerId = toLong(plan.get("ledger_id"));
            Long feeItemId = toLong(plan.get("fee_item_id"));
            LocalDate billingStart = toDate(plan.get("billing_start"));

            // 幂等：相同台账+费项+账期已存在则跳过
            long exist = count(new LambdaQueryWrapper<FinReceivable>()
                    .eq(ledgerId != null, FinReceivable::getLedgerId, ledgerId)
                    .eq(feeItemId != null, FinReceivable::getFeeItemId, feeItemId)
                    .eq(billingStart != null, FinReceivable::getBillingStart, billingStart)
                    .eq(FinReceivable::getIsDeleted, 0));
            if (exist > 0) {
                log.info("[应收同步] ledgerId={} feeItemId={} billingStart={} 已存在，跳过", ledgerId, feeItemId, billingStart);
                return;
            }

            FinReceivable r = new FinReceivable();
            r.setLedgerId(ledgerId);
            r.setContractId(toLong(plan.get("contract_id")));
            r.setShopId(toLong(plan.get("shop_id")));
            r.setFeeItemId(feeItemId);
            r.setFeeName(str(plan.get("fee_name")));
            r.setBillingStart(billingStart);
            r.setBillingEnd(toDate(plan.get("billing_end")));
            r.setDueDate(toDate(plan.get("due_date")));

            BigDecimal amount = toDecimal(plan.get("amount"));
            r.setOriginalAmount(amount);
            r.setAdjustAmount(BigDecimal.ZERO);
            r.setDeductionAmount(BigDecimal.ZERO);
            r.setActualAmount(amount);
            r.setReceivedAmount(BigDecimal.ZERO);
            r.setOutstandingAmount(amount);
            r.setStatus(0);

            // 补充项目/商家信息（从合同查）
            if (r.getContractId() != null) {
                try {
                    Map<String, Object> c = jdbcTemplate.queryForMap(
                            "SELECT project_id, merchant_id FROM inv_lease_contract WHERE id=? LIMIT 1",
                            r.getContractId());
                    r.setProjectId(toLong(c.get("project_id")));
                    r.setMerchantId(toLong(c.get("merchant_id")));
                } catch (Exception ignored) {}
            }
            if (r.getBillingStart() != null) {
                r.setAccrualMonth(r.getBillingStart().format(DateTimeFormatter.ofPattern("yyyy-MM")));
            }
            r.setReceivableCode(genReceivableCode());
            save(r);
            log.info("[应收同步] 已生成 fin_receivable id={}, planId={}", r.getId(), planId);
        } catch (Exception e) {
            log.error("[应收同步] 失败，planId={}，原因：{}", planId, e.getMessage());
            throw new BizException("同步应收失败：" + e.getMessage());
        }
    }

    // ─── 刷新欠费金额（每日 01:00 执行） ────────────────────────
    @Override
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional(rollbackFor = Exception.class)
    public void refreshOverdueDays() {
        // 重新计算 outstanding_amount = actual_amount - received_amount，确保数据一致
        List<FinReceivable> pending = list(new LambdaQueryWrapper<FinReceivable>()
                .in(FinReceivable::getStatus, 0, 1));
        int count = 0;
        for (FinReceivable r : pending) {
            BigDecimal expected = r.getActualAmount().subtract(r.getReceivedAmount()).max(BigDecimal.ZERO);
            if (r.getOutstandingAmount() == null || expected.compareTo(r.getOutstandingAmount()) != 0) {
                update(new LambdaUpdateWrapper<FinReceivable>()
                        .eq(FinReceivable::getId, r.getId())
                        .set(FinReceivable::getOutstandingAmount, expected));
                count++;
            }
        }
        log.info("[欠费刷新] 完成，更新 {} 条", count);
    }

    // ─── 减免申请 ─────────────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long applyDeduction(DeductionCreateDTO dto) {
        // 加锁查询应收记录
        FinReceivable receivable = baseMapper.selectByIdForUpdate(dto.getReceivableId());
        if (receivable == null) throw new BizException("应收记录不存在");
        if (receivable.getStatus() == null || receivable.getStatus() >= 2) {
            throw new FinBizException(FinErrorCode.FIN_4004, "该应收记录不可减免（状态不符）");
        }

        // 校验：减免金额不超过欠费额
        BigDecimal outstanding = receivable.getOutstandingAmount() != null
                ? receivable.getOutstandingAmount() : BigDecimal.ZERO;
        if (dto.getDeductionAmount().compareTo(outstanding) > 0) {
            throw new FinBizException(FinErrorCode.FIN_4004,
                    String.format("减免金额 %.2f 超过欠费额 %.2f", dto.getDeductionAmount(), outstanding));
        }

        // 创建减免单（status=0 待审批）
        FinReceivableDeduction deduction = new FinReceivableDeduction();
        deduction.setDeductionCode(codeGenerator.deductionCode());
        deduction.setReceivableId(dto.getReceivableId());
        deduction.setContractId(receivable.getContractId());
        deduction.setDeductionAmount(dto.getDeductionAmount());
        deduction.setReason(dto.getReason());
        deduction.setStatus(0);
        deductionMapper.insert(deduction);

        // 提交 OA 审批
        try {
            String approvalId = oaApprovalAdapter.submitApproval(
                    "FIN_DEDUCTION", deduction.getId(), "减免审批-" + deduction.getDeductionCode());
            deduction.setApprovalId(approvalId);
            deductionMapper.updateById(deduction);
        } catch (Exception e) {
            log.warn("[减免] OA提交失败，减免单 {} 将手动推进", deduction.getDeductionCode(), e);
        }

        return deduction.getId();
    }

    // ─── 减免审批回调 ─────────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductionCallback(String approvalId, boolean approved) {
        FinReceivableDeduction deduction = deductionMapper.selectByApprovalIdForUpdate(approvalId);
        if (deduction == null) throw new FinBizException(FinErrorCode.FIN_4009, "找不到减免单，approvalId=" + approvalId);
        if (deduction.getStatus() != 0) {
            log.warn("[减免回调] 减免单 {} 已处理（status={}），忽略重复回调", deduction.getDeductionCode(), deduction.getStatus());
            return;
        }

        if (!approved) {
            deduction.setStatus(2);
            deductionMapper.updateById(deduction);
            return;
        }

        // 审批通过：加锁应收记录并更新金额
        FinReceivable receivable = baseMapper.selectByIdForUpdate(deduction.getReceivableId());
        if (receivable == null) throw new BizException("应收记录不存在");

        BigDecimal newDeduction = (receivable.getDeductionAmount() == null ? BigDecimal.ZERO : receivable.getDeductionAmount())
                .add(deduction.getDeductionAmount());
        receivable.setDeductionAmount(newDeduction);
        recalcAmounts(receivable);

        int updated = baseMapper.updateById(receivable);
        if (updated == 0) throw new FinBizException(FinErrorCode.FIN_5001, "应收记录乐观锁冲突");

        deduction.setStatus(1);
        deductionMapper.updateById(deduction);

        log.info("[减免回调] 减免单 {} 审批通过，应收 {} 减免 {} 元",
                deduction.getDeductionCode(), receivable.getReceivableCode(), deduction.getDeductionAmount());
    }

    // ─── 调整申请 ─────────────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long applyAdjustment(AdjustmentCreateDTO dto) {
        FinReceivable receivable = baseMapper.selectByIdForUpdate(dto.getReceivableId());
        if (receivable == null) throw new BizException("应收记录不存在");
        if (receivable.getStatus() == null || receivable.getStatus() >= 2) {
            throw new FinBizException(FinErrorCode.FIN_4004, "该应收记录不可调整（状态不符）");
        }

        FinReceivableAdjustment adjustment = new FinReceivableAdjustment();
        adjustment.setAdjustmentCode(codeGenerator.adjustmentCode());
        adjustment.setReceivableId(dto.getReceivableId());
        adjustment.setContractId(receivable.getContractId());
        adjustment.setAdjustType(dto.getAdjustType());
        adjustment.setAdjustAmount(dto.getAdjustAmount());
        adjustment.setReason(dto.getReason());
        adjustment.setStatus(0);
        adjustmentMapper.insert(adjustment);

        try {
            String approvalId = oaApprovalAdapter.submitApproval(
                    "FIN_ADJUSTMENT", adjustment.getId(), "调整审批-" + adjustment.getAdjustmentCode());
            adjustment.setApprovalId(approvalId);
            adjustmentMapper.updateById(adjustment);
        } catch (Exception e) {
            log.warn("[调整] OA提交失败，调整单 {} 将手动推进", adjustment.getAdjustmentCode(), e);
        }

        return adjustment.getId();
    }

    // ─── 调整审批回调 ─────────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustmentCallback(String approvalId, boolean approved) {
        FinReceivableAdjustment adjustment = adjustmentMapper.selectByApprovalIdForUpdate(approvalId);
        if (adjustment == null) throw new FinBizException(FinErrorCode.FIN_4009, "找不到调整单，approvalId=" + approvalId);
        if (adjustment.getStatus() != 0) {
            log.warn("[调整回调] 调整单 {} 已处理（status={}），忽略重复回调", adjustment.getAdjustmentCode(), adjustment.getStatus());
            return;
        }

        if (!approved) {
            adjustment.setStatus(2);
            adjustmentMapper.updateById(adjustment);
            return;
        }

        FinReceivable receivable = baseMapper.selectByIdForUpdate(adjustment.getReceivableId());
        if (receivable == null) throw new BizException("应收记录不存在");

        BigDecimal prev = receivable.getAdjustAmount() == null ? BigDecimal.ZERO : receivable.getAdjustAmount();
        BigDecimal delta = adjustment.getAdjustType() == 1
                ? adjustment.getAdjustAmount()          // 增加
                : adjustment.getAdjustAmount().negate(); // 减少（存为负delta）
        receivable.setAdjustAmount(prev.add(delta));
        recalcAmounts(receivable);

        int updated = baseMapper.updateById(receivable);
        if (updated == 0) throw new FinBizException(FinErrorCode.FIN_5001, "应收记录乐观锁冲突");

        adjustment.setStatus(1);
        adjustmentMapper.updateById(adjustment);

        log.info("[调整回调] 调整单 {} 审批通过，应收 {} 调整 {} 元（类型={}）",
                adjustment.getAdjustmentCode(), receivable.getReceivableCode(),
                adjustment.getAdjustAmount(), adjustment.getAdjustType());
    }

    // ─── 账单打印标记 ─────────────────────────────────────────────────────────
    @Override
    public void markPrinted(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        update(new LambdaUpdateWrapper<FinReceivable>()
                .in(FinReceivable::getId, ids)
                .set(FinReceivable::getIsPrinted, 1));
        log.info("[账单打印] 标记 {} 条应收记录为已打印", ids.size());
    }

    /**
     * 重算应收金额（减免/调整回调后必须调用）
     * actual = original + adjust - deduction（不能为负）
     * outstanding = actual - received（不能为负）
     */
    private void recalcAmounts(FinReceivable r) {
        BigDecimal original = r.getOriginalAmount() == null ? BigDecimal.ZERO : r.getOriginalAmount();
        BigDecimal adjust   = r.getAdjustAmount()   == null ? BigDecimal.ZERO : r.getAdjustAmount();
        BigDecimal deduct   = r.getDeductionAmount() == null ? BigDecimal.ZERO : r.getDeductionAmount();
        BigDecimal received = r.getReceivedAmount()  == null ? BigDecimal.ZERO : r.getReceivedAmount();

        BigDecimal actual = original.add(adjust).subtract(deduct).max(BigDecimal.ZERO);
        BigDecimal outstanding = actual.subtract(received).max(BigDecimal.ZERO);

        r.setActualAmount(actual);
        r.setOutstandingAmount(outstanding);

        // 更新状态
        if (outstanding.compareTo(BigDecimal.ZERO) == 0 && actual.compareTo(BigDecimal.ZERO) == 0) {
            r.setStatus(3); // 全额减免
        } else if (outstanding.compareTo(BigDecimal.ZERO) == 0) {
            r.setStatus(2); // 已收清
        }
    }

    // ─── 私有辅助 ──────────────────────────────────────────────
    private LambdaQueryWrapper<FinReceivable> buildWrapper(ReceivableQueryDTO q) {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<FinReceivable> w = new LambdaQueryWrapper<FinReceivable>()
                .eq(q.getContractId() != null, FinReceivable::getContractId, q.getContractId())
                .eq(q.getProjectId() != null, FinReceivable::getProjectId, q.getProjectId())
                .eq(q.getMerchantId() != null, FinReceivable::getMerchantId, q.getMerchantId())
                .eq(q.getFeeItemId() != null, FinReceivable::getFeeItemId, q.getFeeItemId())
                .eq(q.getStatus() != null, FinReceivable::getStatus, q.getStatus())
                .eq(q.getAccrualMonth() != null && !q.getAccrualMonth().isBlank(),
                        FinReceivable::getAccrualMonth, q.getAccrualMonth())
                .ge(q.getDueDateFrom() != null, FinReceivable::getDueDate, q.getDueDateFrom())
                .le(q.getDueDateTo() != null, FinReceivable::getDueDate, q.getDueDateTo())
                .like(q.getReceivableCode() != null && !q.getReceivableCode().isBlank(),
                        FinReceivable::getReceivableCode, q.getReceivableCode())
                .orderByDesc(FinReceivable::getDueDate);
        if (Boolean.TRUE.equals(q.getOverdue())) {
            w.lt(FinReceivable::getDueDate, today).in(FinReceivable::getStatus, 0, 1);
        }
        return w;
    }

    private ReceivableDetailVO toDetailVO(FinReceivable e) {
        ReceivableDetailVO vo = new ReceivableDetailVO();
        BeanUtils.copyProperties(e, vo);
        // 计算欠费和逾期状态
        LocalDate today = LocalDate.now();
        vo.setIsOverdue(e.getDueDate() != null && e.getDueDate().isBefore(today)
                && e.getStatus() != null && e.getStatus() < 2);
        if (e.getDueDate() != null && e.getDueDate().isBefore(today)) {
            vo.setOverdueDays((int) ChronoUnit.DAYS.between(e.getDueDate(), today));
        } else {
            vo.setOverdueDays(0);
        }
        vo.setStatusName(statusName(e.getStatus()));
        // 冗余字段
        if (e.getContractId() != null) {
            try {
                Map<String, Object> c = jdbcTemplate.queryForMap(
                        "SELECT contract_code, contract_name FROM inv_lease_contract WHERE id=? LIMIT 1",
                        e.getContractId());
                vo.setContractCode(str(c.get("contract_code")));
                vo.setContractName(str(c.get("contract_name")));
            } catch (Exception ignored) {}
        }
        if (e.getProjectId() != null) {
            try { vo.setProjectName(jdbcTemplate.queryForObject(
                    "SELECT project_name FROM biz_project WHERE id=? LIMIT 1", String.class, e.getProjectId()));
            } catch (Exception ignored) {}
        }
        if (e.getMerchantId() != null) {
            try { vo.setMerchantName(jdbcTemplate.queryForObject(
                    "SELECT merchant_name FROM biz_merchant WHERE id=? LIMIT 1", String.class, e.getMerchantId()));
            } catch (Exception ignored) {}
        }
        return vo;
    }

    private void fillContractInfo(ReceivableSummaryVO vo, Long contractId) {
        try {
            Map<String, Object> c = jdbcTemplate.queryForMap(
                    "SELECT contract_code, contract_name FROM inv_lease_contract WHERE id=? LIMIT 1", contractId);
            vo.setContractCode(str(c.get("contract_code")));
            vo.setContractName(str(c.get("contract_name")));
        } catch (Exception ignored) {}
        if (vo.getProjectId() != null) {
            try { vo.setProjectName(jdbcTemplate.queryForObject(
                    "SELECT project_name FROM biz_project WHERE id=? LIMIT 1", String.class, vo.getProjectId()));
            } catch (Exception ignored) {}
        }
        if (vo.getMerchantId() != null) {
            try { vo.setMerchantName(jdbcTemplate.queryForObject(
                    "SELECT merchant_name FROM biz_merchant WHERE id=? LIMIT 1", String.class, vo.getMerchantId()));
            } catch (Exception ignored) {}
        }
    }

    private String genReceivableCode() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "AR" + date;
        try {
            String max = jdbcTemplate.queryForObject(
                    "SELECT MAX(receivable_code) FROM fin_receivable WHERE receivable_code LIKE ? AND is_deleted=0",
                    String.class, prefix + "%");
            if (max != null && max.length() >= 10) {
                long seq = Long.parseLong(max.substring(10)) + 1;
                return prefix + String.format("%06d", seq);
            }
        } catch (Exception ignored) {}
        return prefix + "000001";
    }

    private String statusName(Integer s) {
        if (s == null) return "";
        return switch (s) {
            case 0 -> "待收"; case 1 -> "部分收款"; case 2 -> "已收清";
            case 3 -> "已减免"; case 4 -> "已作废"; default -> "";
        };
    }

    private BigDecimal sum(List<FinReceivable> list, java.util.function.Function<FinReceivable, BigDecimal> fn) {
        return list.stream().map(fn).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    private Long toLong(Object v) { if (v == null) return null; try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; } }
    private String str(Object v) { return v != null ? v.toString() : null; }
    private BigDecimal toDecimal(Object v) { if (v == null) return BigDecimal.ZERO; try { return new BigDecimal(v.toString()); } catch (Exception e) { return BigDecimal.ZERO; } }
    private LocalDate toDate(Object v) { if (v == null) return null; if (v instanceof LocalDate) return (LocalDate) v; try { return LocalDate.parse(v.toString().substring(0, 10)); } catch (Exception e) { return null; } }
}
