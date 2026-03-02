package com.asset.report.etl.investment;

import com.asset.report.entity.RptInvestmentDaily;
import com.asset.report.etl.AbstractReportEtlJob;
import com.asset.report.mapper.etl.InvestmentEtlMapper;
import com.asset.report.mapper.rpt.RptInvestmentDailyMapper;
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
 * 招商日汇总 ETL 任务
 * <p>
 * 调度：每日 02:10（T+1日更新昨日数据）
 * XXL-Job Name：investmentDailyEtlJob
 * 数据源：inv_intention + inv_lease_contract
 * 目标表：rpt_investment_daily
 * 汇总粒度：项目 / 业态
 * 计算指标：意向转化率、平均租金单价
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InvestmentDailyEtlJob extends AbstractReportEtlJob {

    private final InvestmentEtlMapper investmentEtlMapper;
    private final RptInvestmentDailyMapper rptInvestmentDailyMapper;

    private static final int BATCH_SIZE = 500;

    @XxlJob("investmentDailyEtlJob")
    public ReturnT<String> execute(String param) {
        return executeDaily(param);
    }

    @Override
    protected String getJobName() {
        return "InvestmentDailyEtlJob";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected void doEtl(LocalDate statDate) throws Exception {
        log.info("[InvestmentETL] 开始聚合，日期: {}", statDate);

        List<RptInvestmentDaily> rows = investmentEtlMapper.aggregateInvestmentDaily(statDate);
        if (rows == null || rows.isEmpty()) {
            log.warn("[InvestmentETL] 未查到数据，日期: {}", statDate);
            return;
        }

        // 补充计算字段
        rows.forEach(r -> {
            r.setStatDate(statDate);
            // 意向转化率 = 合同数 / 意向数 * 100
            BigDecimal intentionDec = BigDecimal.valueOf(safeInt(r.getIntentionCount()));
            BigDecimal contractDec  = BigDecimal.valueOf(safeInt(r.getContractCount()));
            r.setConversionRate(calcRate(contractDec, intentionDec));
            // 平均租金 = 合同总金额 / 签约面积（月均，暂用总额直接存，查询时再除月数）
            if (r.getAvgRentPrice() == null) {
                r.setAvgRentPrice(BigDecimal.ZERO);
            }
        });

        // 幂等写入
        int deleted = rptInvestmentDailyMapper.deleteByStatDate(statDate);
        log.debug("[InvestmentETL] 清理旧数据: {} 条", deleted);

        List<List<RptInvestmentDaily>> batches = partition(rows, BATCH_SIZE);
        int total = 0;
        for (List<RptInvestmentDaily> batch : batches) {
            total += rptInvestmentDailyMapper.upsertBatch(batch);
        }
        log.info("[InvestmentETL] 写入完成，共 {} 条，日期: {}", total, statDate);
    }
}
