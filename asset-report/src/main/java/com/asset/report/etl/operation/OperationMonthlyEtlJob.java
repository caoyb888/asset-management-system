package com.asset.report.etl.operation;

import com.asset.report.entity.RptOperationMonthly;
import com.asset.report.etl.AbstractReportEtlJob;
import com.asset.report.mapper.etl.OperationEtlMapper;
import com.asset.report.mapper.rpt.RptOperationMonthlyMapper;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 营运月汇总 ETL 任务
 * <p>
 * 调度：每月1日 03:00（T+1月更新上月数据）
 * XXL-Job Name：operationMonthlyEtlJob
 * 数据源：opr_revenue_report + opr_contract_change + opr_passenger_flow + inv_lease_contract
 * 目标表：rpt_operation_monthly
 * 汇总粒度：项目 / 楼栋 / 业态
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperationMonthlyEtlJob extends AbstractReportEtlJob {

    private final OperationEtlMapper operationEtlMapper;
    private final RptOperationMonthlyMapper rptOperationMonthlyMapper;

    private static final int BATCH_SIZE = 200;

    @XxlJob("operationMonthlyEtlJob")
    public ReturnT<String> execute(String param) {
        return executeMonthly(param);
    }

    @Override
    protected String getJobName() {
        return "OperationMonthlyEtlJob";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected void doEtlMonthly(String statMonth) throws Exception {
        log.info("[OperationETL] 开始聚合，月份: {}", statMonth);

        List<RptOperationMonthly> rows = operationEtlMapper.aggregateOperationMonthly(statMonth);
        if (rows == null || rows.isEmpty()) {
            log.warn("[OperationETL] 未查到数据，月份: {}", statMonth);
            return;
        }

        rows.forEach(r -> r.setStatMonth(statMonth));

        // 幂等写入
        int deleted = rptOperationMonthlyMapper.deleteByStatMonth(statMonth);
        log.debug("[OperationETL] 清理旧数据: {} 条，月份: {}", deleted, statMonth);

        List<List<RptOperationMonthly>> batches = partition(rows, BATCH_SIZE);
        int total = 0;
        for (List<RptOperationMonthly> batch : batches) {
            total += rptOperationMonthlyMapper.upsertBatch(batch);
        }
        log.info("[OperationETL] 写入完成，共 {} 条，月份: {}", total, statMonth);
    }
}
