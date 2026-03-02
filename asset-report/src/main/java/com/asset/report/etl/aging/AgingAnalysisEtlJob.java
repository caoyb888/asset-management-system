package com.asset.report.etl.aging;

import com.asset.report.entity.RptAgingAnalysis;
import com.asset.report.etl.AbstractReportEtlJob;
import com.asset.report.mapper.etl.AgingEtlMapper;
import com.asset.report.mapper.rpt.RptAgingAnalysisMapper;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 账龄分析预计算 ETL 任务
 * <p>
 * 调度：每日 02:30（T+1日更新昨日数据）
 * XXL-Job Name：agingAnalysisEtlJob
 * 数据源：fin_receivable（按 due_date 与 statDate 的天数差分档）
 * 目标表：rpt_aging_analysis
 * 汇总粒度：项目 / 商家 / 合同 / 费项
 * 分档逻辑：
 *   within_30    : overdue 1-30天
 *   days_31_60   : overdue 31-60天
 *   days_61_90   : overdue 61-90天
 *   days_91_180  : overdue 91-180天
 *   days_181_365 : overdue 181-365天
 *   over_365     : overdue > 365天
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgingAnalysisEtlJob extends AbstractReportEtlJob {

    private final AgingEtlMapper agingEtlMapper;
    private final RptAgingAnalysisMapper rptAgingAnalysisMapper;

    private static final int BATCH_SIZE = 300;

    @XxlJob("agingAnalysisEtlJob")
    public ReturnT<String> execute(String param) {
        return executeDaily(param);
    }

    @Override
    protected String getJobName() {
        return "AgingAnalysisEtlJob";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected void doEtl(LocalDate statDate) throws Exception {
        log.info("[AgingETL] 开始预计算，日期: {}", statDate);

        List<RptAgingAnalysis> rows = agingEtlMapper.aggregateAgingAnalysis(statDate);
        if (rows == null || rows.isEmpty()) {
            log.info("[AgingETL] 无欠款数据（可能全部已收），日期: {}", statDate);
            return;
        }

        // 补充 statDate 和合计
        rows.forEach(r -> {
            r.setStatDate(statDate);
            // 校验并合计
            BigDecimal total = safeDec(r.getWithin30())
                    .add(safeDec(r.getDays3160()))
                    .add(safeDec(r.getDays6190()))
                    .add(safeDec(r.getDays91180()))
                    .add(safeDec(r.getDays181365()))
                    .add(safeDec(r.getOver365()));
            r.setTotalOutstanding(total);
        });

        // 过滤掉合计为0的记录（已清欠）
        rows.removeIf(r -> r.getTotalOutstanding().compareTo(BigDecimal.ZERO) <= 0);

        if (rows.isEmpty()) {
            log.info("[AgingETL] 过滤后无欠款数据，日期: {}", statDate);
            return;
        }

        // 幂等写入
        int deleted = rptAgingAnalysisMapper.deleteByStatDate(statDate);
        log.debug("[AgingETL] 清理旧数据: {} 条，日期: {}", deleted, statDate);

        List<List<RptAgingAnalysis>> batches = partition(rows, BATCH_SIZE);
        int total = 0;
        for (List<RptAgingAnalysis> batch : batches) {
            total += rptAgingAnalysisMapper.upsertBatch(batch);
        }
        log.info("[AgingETL] 写入完成，共 {} 条欠款记录，日期: {}", total, statDate);

        // 对账日志
        BigDecimal totalOutstanding = rows.stream()
                .map(r -> safeDec(r.getTotalOutstanding()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        log.info("[AgingETL] 对账：全库欠款合计 {} 元，日期: {}", totalOutstanding, statDate);
    }
}
