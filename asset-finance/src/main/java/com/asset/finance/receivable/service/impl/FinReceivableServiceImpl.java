package com.asset.finance.receivable.service.impl;

import com.alibaba.excel.EasyExcel;
import com.asset.common.exception.BizException;
import com.asset.finance.receivable.dto.*;
import com.asset.finance.receivable.entity.FinReceivable;
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

        // 按 contractId 分组汇总
        Map<Long, List<FinReceivable>> byContract = all.stream()
                .filter(r -> r.getContractId() != null)
                .collect(Collectors.groupingBy(FinReceivable::getContractId));

        return byContract.entrySet().stream().map(entry -> {
            Long contractId = entry.getKey();
            List<FinReceivable> items = entry.getValue();
            ReceivableSummaryVO vo = new ReceivableSummaryVO();
            vo.setContractId(contractId);
            // 冗余字段
            if (!items.isEmpty()) {
                vo.setProjectId(items.get(0).getProjectId());
                vo.setMerchantId(items.get(0).getMerchantId());
            }
            fillContractInfo(vo, contractId);
            // 金额汇总
            vo.setTotalOriginal(sum(items, FinReceivable::getOriginalAmount));
            vo.setTotalActual(sum(items, FinReceivable::getActualAmount));
            vo.setTotalReceived(sum(items, FinReceivable::getReceivedAmount));
            vo.setTotalReduction(sum(items, FinReceivable::getReductionAmount));
            BigDecimal outstanding = vo.getTotalActual().subtract(vo.getTotalReceived());
            vo.setTotalOutstanding(outstanding.max(BigDecimal.ZERO));
            List<FinReceivable> overdue = items.stream()
                    .filter(r -> r.getOverdueDays() != null && r.getOverdueDays() > 0)
                    .filter(r -> r.getStatus() != null && r.getStatus() < 2)
                    .collect(Collectors.toList());
            vo.setOverdueCount(overdue.size());
            BigDecimal overdueAmt = overdue.stream()
                    .map(r -> r.getActualAmount().subtract(r.getReceivedAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            vo.setOverdueAmount(overdueAmt.max(BigDecimal.ZERO));
            return vo;
        }).sorted(Comparator.comparing(ReceivableSummaryVO::getTotalOutstanding).reversed())
                .collect(Collectors.toList());
    }

    // ─── 欠费统计 ─────────────────────────────────────────────
    @Override
    public OverdueStatisticsVO overdueStatistics(Long projectId) {
        LambdaQueryWrapper<FinReceivable> w = new LambdaQueryWrapper<FinReceivable>()
                .in(FinReceivable::getStatus, 0, 1)  // 待收/部分收
                .gt(FinReceivable::getOverdueDays, 0)
                .eq(projectId != null, FinReceivable::getProjectId, projectId);
        List<FinReceivable> overdue = list(w);

        OverdueStatisticsVO vo = new OverdueStatisticsVO();
        BigDecimal amt30 = BigDecimal.ZERO, amt30to90 = BigDecimal.ZERO, amtOver90 = BigDecimal.ZERO;
        for (FinReceivable r : overdue) {
            int days = r.getOverdueDays() != null ? r.getOverdueDays() : 0;
            BigDecimal outstanding = r.getActualAmount().subtract(r.getReceivedAmount()).max(BigDecimal.ZERO);
            if (days <= 30) amt30 = amt30.add(outstanding);
            else if (days <= 90) amt30to90 = amt30to90.add(outstanding);
            else amtOver90 = amtOver90.add(outstanding);
        }
        vo.setOverdue30Amount(amt30);
        vo.setOverdue30To90Amount(amt30to90);
        vo.setOverdueOver90Amount(amtOver90);
        vo.setTotalOverdueAmount(amt30.add(amt30to90).add(amtOver90));
        vo.setTotalOverdueCount(overdue.size());

        // TOP10 欠费租户
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
        IPage<ReceivableDetailVO> page = pageQuery(query);
        List<ReceivableDetailVO> rows = page.getRecords();
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String fileName = URLEncoder.encode("应收明细_" +
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")), StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName + ".xlsx");
            EasyExcel.write(response.getOutputStream(), ReceivableDetailVO.class)
                    .sheet("应收明细").doWrite(rows);
        } catch (Exception e) {
            throw new BizException("导出失败：" + e.getMessage());
        }
    }

    // ─── 从营运计划同步（幂等） ─────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncFromPlan(Long planId) {
        // 幂等：已存在则跳过
        long exist = count(new LambdaQueryWrapper<FinReceivable>()
                .eq(FinReceivable::getPlanId, planId)
                .eq(FinReceivable::getIsDeleted, 0));
        if (exist > 0) {
            log.info("[应收同步] planId={} 已存在，跳过", planId);
            return;
        }
        try {
            Map<String, Object> plan = jdbcTemplate.queryForMap(
                    "SELECT * FROM opr_receivable_plan WHERE id=? AND is_deleted=0 LIMIT 1", planId);
            FinReceivable r = new FinReceivable();
            r.setPlanId(planId);
            r.setContractId(toLong(plan.get("contract_id")));
            r.setLedgerId(toLong(plan.get("ledger_id")));
            r.setShopId(toLong(plan.get("shop_id")));
            r.setFeeItemId(toLong(plan.get("fee_item_id")));
            r.setFeeName(str(plan.get("fee_name")));
            r.setBillingStart(toDate(plan.get("billing_start")));
            r.setBillingEnd(toDate(plan.get("billing_end")));
            r.setDueDate(toDate(plan.get("due_date")));
            BigDecimal amount = toDecimal(plan.get("amount"));
            r.setOriginalAmount(amount);
            r.setActualAmount(amount);
            r.setReceivedAmount(BigDecimal.ZERO);
            r.setReductionAmount(BigDecimal.ZERO);
            r.setStatus(0);
            r.setOverdueDays(0);
            // 补充项目/商家信息（从合同查）
            if (r.getContractId() != null) {
                try {
                    Map<String, Object> c = jdbcTemplate.queryForMap(
                            "SELECT project_id, merchant_id, brand_id FROM inv_lease_contract WHERE id=? LIMIT 1",
                            r.getContractId());
                    r.setProjectId(toLong(c.get("project_id")));
                    r.setMerchantId(toLong(c.get("merchant_id")));
                    r.setBrandId(toLong(c.get("brand_id")));
                } catch (Exception ignored) {}
            }
            // 归属月份
            if (r.getBillingStart() != null) {
                r.setAccrualMonth(r.getBillingStart().format(DateTimeFormatter.ofPattern("yyyy-MM")));
            }
            // 生成应收编号
            r.setReceivableCode(genReceivableCode());
            save(r);
            log.info("[应收同步] 已生成 fin_receivable id={}, planId={}", r.getId(), planId);
        } catch (Exception e) {
            log.error("[应收同步] 失败，planId={}，原因：{}", planId, e.getMessage());
            throw new BizException("同步应收失败：" + e.getMessage());
        }
    }

    // ─── 刷新逾期天数（每日 01:00 执行） ────────────────────────
    @Override
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional(rollbackFor = Exception.class)
    public void refreshOverdueDays() {
        LocalDate today = LocalDate.now();
        List<FinReceivable> pending = list(new LambdaQueryWrapper<FinReceivable>()
                .in(FinReceivable::getStatus, 0, 1)
                .isNotNull(FinReceivable::getDueDate));
        int count = 0;
        for (FinReceivable r : pending) {
            int days = (int) ChronoUnit.DAYS.between(r.getDueDate(), today);
            int overdue = Math.max(0, days);
            if (!Objects.equals(overdue, r.getOverdueDays())) {
                update(new LambdaUpdateWrapper<FinReceivable>()
                        .eq(FinReceivable::getId, r.getId())
                        .set(FinReceivable::getOverdueDays, overdue));
                count++;
            }
        }
        log.info("[逾期刷新] 完成，更新 {} 条", count);
    }

    // ─── 私有辅助 ──────────────────────────────────────────────
    private LambdaQueryWrapper<FinReceivable> buildWrapper(ReceivableQueryDTO q) {
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
            w.gt(FinReceivable::getOverdueDays, 0).in(FinReceivable::getStatus, 0, 1);
        }
        return w;
    }

    private ReceivableDetailVO toDetailVO(FinReceivable e) {
        ReceivableDetailVO vo = new ReceivableDetailVO();
        BeanUtils.copyProperties(e, vo);
        // 未收金额
        BigDecimal outstanding = e.getActualAmount().subtract(e.getReceivedAmount()).max(BigDecimal.ZERO);
        vo.setOutstandingAmount(outstanding);
        vo.setIsOverdue(e.getOverdueDays() != null && e.getOverdueDays() > 0);
        // 状态名称
        vo.setStatusName(statusName(e.getStatus()));
        // 冗余字段（合同/项目/商家）
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
        return switch (s) { case 0 -> "待收"; case 1 -> "部分收款"; case 2 -> "已收"; case 3 -> "已作废"; case 4 -> "已减免"; default -> ""; };
    }

    private BigDecimal sum(List<FinReceivable> list, java.util.function.Function<FinReceivable, BigDecimal> fn) {
        return list.stream().map(fn).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    private Long toLong(Object v) { if (v == null) return null; try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; } }
    private String str(Object v) { return v != null ? v.toString() : null; }
    private BigDecimal toDecimal(Object v) { if (v == null) return BigDecimal.ZERO; try { return new BigDecimal(v.toString()); } catch (Exception e) { return BigDecimal.ZERO; } }
    private LocalDate toDate(Object v) { if (v == null) return null; if (v instanceof LocalDate) return (LocalDate) v; try { return LocalDate.parse(v.toString().substring(0, 10)); } catch (Exception e) { return null; } }
}
