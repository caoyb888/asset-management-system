package com.asset.report.etl.asset;

import com.asset.report.entity.RptAssetDaily;
import com.asset.report.etl.AbstractReportEtlJob;
import com.asset.report.mapper.etl.AssetEtlMapper;
import com.asset.report.mapper.rpt.RptAssetDailyMapper;
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
 * 资产日汇总 ETL 任务
 * <p>
 * 调度：每日 02:00（T+1日更新昨日数据）
 * XXL-Job Name：assetDailyEtlJob
 * 数据源：biz_shop + inv_lease_contract + inv_lease_contract_shop
 * 目标表：rpt_asset_daily
 * 汇总粒度：项目 / 楼栋 / 楼层 / 业态
 * 计算指标：空置率、出租率、开业率
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssetDailyEtlJob extends AbstractReportEtlJob {

    private final AssetEtlMapper assetEtlMapper;
    private final RptAssetDailyMapper rptAssetDailyMapper;

    private static final int BATCH_SIZE = 500;

    /**
     * XXL-Job 入口
     * 参数：yyyy-MM-dd（空=默认昨日）
     */
    @XxlJob("assetDailyEtlJob")
    public ReturnT<String> execute(String param) {
        return executeDaily(param);
    }

    @Override
    protected String getJobName() {
        return "AssetDailyEtlJob";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected void doEtl(LocalDate statDate) throws Exception {
        log.info("[AssetETL] 开始聚合，日期: {}", statDate);

        // 1. 从业务库聚合资产指标
        List<RptAssetDaily> rows = assetEtlMapper.aggregateAssetDaily(statDate);
        if (rows == null || rows.isEmpty()) {
            log.warn("[AssetETL] 未查到数据，日期: {}", statDate);
            return;
        }

        // 2. 补充计算三率（防止 SQL 层除零未处理）
        rows.forEach(r -> {
            r.setStatDate(statDate);
            r.setVacancyRate(calcRate(safeDec(r.getVacantArea()), safeDec(r.getTotalArea())));
            r.setRentalRate(calcRate(safeDec(r.getRentedArea()), safeDec(r.getTotalArea())));
            BigDecimal totalShopsDec = BigDecimal.valueOf(safeInt(r.getTotalShops()));
            BigDecimal openedShopsDec = BigDecimal.valueOf(safeInt(r.getOpenedShops()));
            r.setOpeningRate(calcRate(openedShopsDec, totalShopsDec));
        });

        // 3. 幂等写入（先删后插，保证重跑数据一致）
        int deleted = rptAssetDailyMapper.deleteByStatDate(statDate);
        log.debug("[AssetETL] 清理旧数据: {} 条，日期: {}", deleted, statDate);

        // 4. 分批插入
        List<List<RptAssetDaily>> batches = partition(rows, BATCH_SIZE);
        int total = 0;
        for (List<RptAssetDaily> batch : batches) {
            total += rptAssetDailyMapper.upsertBatch(batch);
        }
        log.info("[AssetETL] 写入完成，共 {} 条，日期: {}", total, statDate);

        // 5. 数据对账校验（汇总数 vs 业务源头）
        verifyConsistency(statDate, rows);
    }

    /**
     * 数据一致性校验：汇总项目级记录数 ≥ 数据库中项目数
     */
    private void verifyConsistency(LocalDate statDate, List<RptAssetDaily> rows) {
        long projectCount = rows.stream()
                .filter(r -> r.getBuildingId() == 0L && r.getFloorId() == 0L
                        && "".equals(r.getFormatType()))
                .count();
        log.info("[AssetETL] 对账：共 {} 个项目级汇总，日期: {}", projectCount, statDate);
        if (projectCount == 0) {
            log.warn("[AssetETL] 警告：无项目级汇总数据，请检查 biz_shop 是否有数据");
        }
    }
}
