package com.asset.report.etl.finance;

import com.asset.report.entity.RptFinanceMonthly;
import com.asset.report.etl.AbstractReportEtlJob;
import com.asset.report.mapper.etl.FinanceEtlMapper;
import com.asset.report.mapper.rpt.RptFinanceMonthlyMapper;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 财务月汇总 ETL 任务
 * <p>
 * 调度：每月1日 03:10（T+1月更新上月数据）
 * XXL-Job Name：financeMonthlyEtlJob
 * 数据源：fin_receivable + fin_receipt + fin_deposit_account + fin_prepay_account
 * 目标表：rpt_finance_monthly
 * 汇总粒度：项目 / 费项
 * 计算指标：应收/已收/欠款/减免/逾期/收缴率/逾期率
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FinanceMonthlyEtlJob extends AbstractReportEtlJob {

    private final FinanceEtlMapper financeEtlMapper;
    private final RptFinanceMonthlyMapper rptFinanceMonthlyMapper;

    private static final int BATCH_SIZE = 200;

    @XxlJob("financeMonthlyEtlJob")
    public ReturnT<String> execute(String param) {
        return executeMonthly(param);
    }

    @Override
    protected String getJobName() {
        return "FinanceMonthlyEtlJob";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected void doEtlMonthly(String statMonth) throws Exception {
        log.info("[FinanceETL] 开始聚合，月份: {}", statMonth);

        // 1. 聚合应收/已收/欠款/逾期 按项目/费项维度
        List<RptFinanceMonthly> mainRows = financeEtlMapper.aggregateFinanceMonthly(statMonth);
        if (mainRows == null || mainRows.isEmpty()) {
            log.warn("[FinanceETL] 未查到财务数据，月份: {}", statMonth);
            return;
        }
        mainRows.forEach(r -> {
            r.setStatMonth(statMonth);
            // 计算收缴率
            r.setCollectionRate(calcRate(safeDec(r.getReceivedAmount()), safeDec(r.getReceivableAmount())));
            // 计算逾期率
            r.setOverdueRate(calcRate(safeDec(r.getOverdueAmount()), safeDec(r.getReceivableAmount())));
            // 计算欠款
            BigDecimal outstanding = safeDec(r.getReceivableAmount())
                    .subtract(safeDec(r.getReceivedAmount()));
            if (outstanding.compareTo(BigDecimal.ZERO) < 0) outstanding = BigDecimal.ZERO;
            r.setOutstandingAmount(outstanding);
        });

        // 2. 查询保证金/预收款余额（项目级汇总）
        List<RptFinanceMonthly> depositRows = financeEtlMapper.aggregateDepositBalance(statMonth);
        List<RptFinanceMonthly> prepayRows  = financeEtlMapper.aggregatePrepayBalance(statMonth);

        // 合并保证金/预收款余额到主数据（按 projectId+feeItemId=0 匹配）
        Map<Long, BigDecimal> depositMap = depositRows.stream()
                .filter(r -> r.getDepositBalance() != null)
                .collect(Collectors.toMap(RptFinanceMonthly::getProjectId,
                        RptFinanceMonthly::getDepositBalance, (a, b) -> a));
        Map<Long, BigDecimal> prepayMap = prepayRows.stream()
                .filter(r -> r.getPrepayBalance() != null)
                .collect(Collectors.toMap(RptFinanceMonthly::getProjectId,
                        RptFinanceMonthly::getPrepayBalance, (a, b) -> a));

        mainRows.forEach(r -> {
            if (r.getFeeItemId() == 0L) {
                // 仅在项目汇总行填入余额
                r.setDepositBalance(depositMap.getOrDefault(r.getProjectId(), BigDecimal.ZERO));
                r.setPrepayBalance(prepayMap.getOrDefault(r.getProjectId(), BigDecimal.ZERO));
            }
        });

        // 3. 幂等写入
        int deleted = rptFinanceMonthlyMapper.deleteByStatMonth(statMonth);
        log.debug("[FinanceETL] 清理旧数据: {} 条，月份: {}", deleted, statMonth);

        List<List<RptFinanceMonthly>> batches = partition(mainRows, BATCH_SIZE);
        int total = 0;
        for (List<RptFinanceMonthly> batch : batches) {
            total += rptFinanceMonthlyMapper.upsertBatch(batch);
        }
        log.info("[FinanceETL] 写入完成，共 {} 条，月份: {}", total, statMonth);

        // 4. 对账日志
        BigDecimal totalReceivable = mainRows.stream()
                .filter(r -> r.getFeeItemId() == 0L)
                .map(r -> safeDec(r.getReceivableAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        log.info("[FinanceETL] 对账：总应收 {} 元，月份: {}", totalReceivable, statMonth);
    }
}
