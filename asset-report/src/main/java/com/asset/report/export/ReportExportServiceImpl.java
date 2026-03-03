package com.asset.report.export;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.asset.common.security.util.SecurityUtil;
import com.asset.report.asset.ReportAssetService;
import com.asset.report.common.param.ReportQueryParam;
import com.asset.report.common.permission.ReportPermissionContext;
import com.asset.report.config.ReportExportConfig;
import com.asset.report.entity.RptGenerationLog;
import com.asset.report.export.dto.ExportTaskDTO;
import com.asset.report.export.vo.ExportTaskStatusVO;
import com.asset.report.finance.ReportFinanceService;
import com.asset.report.investment.ReportInvestmentService;
import com.asset.report.mapper.rpt.RptGenerationLogMapper;
import com.asset.report.operation.ReportOperationService;
import com.asset.report.vo.asset.BrandDistributionVO;
import com.asset.report.vo.asset.RateTrendVO;
import com.asset.report.vo.asset.ShopRentalVO;
import com.asset.report.vo.fin.*;
import com.asset.report.vo.inv.IntentionStatsVO;
import com.asset.report.vo.inv.PerformanceVO;
import com.asset.report.vo.opr.OprContractChangeVO;
import com.asset.report.vo.opr.OprRevenueSummaryVO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 报表导出 Service 实现
 * <p>
 * 核心流程：
 * <ol>
 *   <li>Redis 缓存命中检测（相同参数 30 分钟内复用）</li>
 *   <li>创建 {@code rpt_generation_log} 记录（status=2）</li>
 *   <li>@Async 提交到 {@code reportExportExecutor} 线程池</li>
 *   <li>线程池内：查数据 → EasyExcel 写文件 → 更新 log 状态 → 写 Redis 缓存</li>
 * </ol>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportExportServiceImpl implements ReportExportService {

    private final RptGenerationLogMapper logMapper;
    private final ReportExportConfig exportConfig;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    private final ReportFinanceService finService;
    private final ReportAssetService assetService;
    private final ReportOperationService oprService;
    private final ReportInvestmentService invService;

    private static final String CACHE_PREFIX = "rpt:export:cache:";
    private static final long CACHE_TTL_MIN = 30L;
    private static final DateTimeFormatter LOG_CODE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final AtomicInteger SEQ = new AtomicInteger(0);

    // ==================== 主流程 ====================

    @Override
    public String submitExport(ExportTaskDTO dto) {
        // 1. 生成缓存 key（reportCode + format + sorted params）
        String cacheKey = buildCacheKey(dto);

        // 2. 检查 Redis 缓存：相同参数 30 分钟内已有成功记录
        String cachedLogCode = redis.opsForValue().get(CACHE_PREFIX + cacheKey);
        if (cachedLogCode != null) {
            RptGenerationLog cached = logMapper.selectByLogCode(cachedLogCode);
            if (cached != null && cached.getStatus() == RptGenerationLog.STATUS_SUCCESS) {
                log.info("[Export] 命中缓存，直接返回 logCode={}", cachedLogCode);
                return cachedLogCode;
            }
        }

        // 3. 创建 log 记录（status=2: 进行中）
        String logCode = generateLogCode();
        String paramsJson = serializeParams(dto.getParams());
        String format = dto.getFormat() == null ? RptGenerationLog.FORMAT_EXCEL : dto.getFormat().toUpperCase();
        String fileName = buildFileName(dto.getReportCode(), format);

        Long userId = SecurityUtil.getCurrentUserId();
        RptGenerationLog log = new RptGenerationLog()
                .setLogCode(logCode)
                .setReportId(0L)
                .setGenerationType(RptGenerationLog.TYPE_MANUAL)
                .setTriggeredBy(userId)
                .setFileFormat(format)
                .setFileName(fileName)
                .setFilterParams(paramsJson)
                .setDataCount(0)
                .setStatus(RptGenerationLog.STATUS_PENDING);
        logMapper.insert(log);

        // 4. 捕获权限上下文（ThreadLocal 在异步线程不可见，需显式传递）
        List<Long> permIds = ReportPermissionContext.get();
        boolean finViewPerm = ReportPermissionContext.hasFinViewPerm();
        String username = SecurityUtil.getCurrentUsername();

        // 5. 提交异步任务
        doExportAsync(logCode, dto, permIds, finViewPerm, username, cacheKey);

        return logCode;
    }

    @Override
    public ExportTaskStatusVO queryStatus(String logCode) {
        RptGenerationLog record = logMapper.selectByLogCode(logCode);
        if (record == null) {
            return ExportTaskStatusVO.fail(logCode, "任务不存在");
        }
        return switch (record.getStatus()) {
            case RptGenerationLog.STATUS_SUCCESS ->
                    ExportTaskStatusVO.success(logCode, record.getFileName(),
                            record.getDataCount(), record.getDurationMs());
            case RptGenerationLog.STATUS_FAIL ->
                    ExportTaskStatusVO.fail(logCode, record.getErrorMsg());
            default -> ExportTaskStatusVO.pending(logCode);
        };
    }

    @Override
    public List<RptGenerationLog> myLogs(int limit) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == 0L) return Collections.emptyList();
        return logMapper.selectByUser(userId, Math.min(limit, 50));
    }

    // ==================== 异步导出 ====================

    @Async("reportExportExecutor")
    public void doExportAsync(String logCode, ExportTaskDTO dto,
                              List<Long> permIds, boolean finViewPerm,
                              String username, String cacheKey) {
        long startMs = System.currentTimeMillis();
        // 注入权限上下文（异步线程独立）
        ReportPermissionContext.set(permIds);
        ReportPermissionContext.setFinViewPerm(finViewPerm);
        try {
            String format = dto.getFormat() == null ? RptGenerationLog.FORMAT_EXCEL : dto.getFormat().toUpperCase();
            ReportQueryParam param = buildQueryParam(dto.getParams());

            if (RptGenerationLog.FORMAT_PDF.equals(format)) {
                // PDF 暂不支持（Puppeteer 方案需单独部署，此处优雅降级）
                throw new UnsupportedOperationException("PDF 导出暂未支持，请选择 Excel 格式");
            }

            // 获取 Excel 定义
            ExportDefinition def = resolveDefinition(dto.getReportCode(), param);

            // 确保目录存在
            String dir = exportConfig.getOrCreateExportDir();
            String filePath = dir + File.separator + logCode + ".xlsx";

            // 写 Excel（流式，内存友好），带水印
            String exportTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .format(LocalDateTime.now());
            int rowCount = writeExcel(filePath, def, username, exportTime);

            long elapsed = System.currentTimeMillis() - startMs;
            File file = new File(filePath);

            // 更新 log 为成功
            logMapper.update(null,
                    new LambdaUpdateWrapper<RptGenerationLog>()
                            .eq(RptGenerationLog::getLogCode, logCode)
                            .set(RptGenerationLog::getStatus, RptGenerationLog.STATUS_SUCCESS)
                            .set(RptGenerationLog::getFilePath, filePath)
                            .set(RptGenerationLog::getFileSize, file.exists() ? file.length() : 0L)
                            .set(RptGenerationLog::getDataCount, rowCount)
                            .set(RptGenerationLog::getDurationMs, (int) elapsed)
                            .set(RptGenerationLog::getUpdatedAt, LocalDateTime.now()));

            // 写 Redis 缓存（30 分钟有效）
            redis.opsForValue().set(CACHE_PREFIX + cacheKey, logCode, CACHE_TTL_MIN, TimeUnit.MINUTES);

            log.info("[Export] 成功：logCode={}, rows={}, elapsed={}ms", logCode, rowCount, elapsed);

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startMs;
            log.error("[Export] 失败：logCode={}", logCode, e);
            String errMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (errMsg.length() > 500) errMsg = errMsg.substring(0, 500);
            final String finalErrMsg = errMsg;
            logMapper.update(null,
                    new LambdaUpdateWrapper<RptGenerationLog>()
                            .eq(RptGenerationLog::getLogCode, logCode)
                            .set(RptGenerationLog::getStatus, RptGenerationLog.STATUS_FAIL)
                            .set(RptGenerationLog::getErrorMsg, finalErrMsg)
                            .set(RptGenerationLog::getDurationMs, (int) elapsed)
                            .set(RptGenerationLog::getUpdatedAt, LocalDateTime.now()));
        } finally {
            ReportPermissionContext.clear();
        }
    }

    // ==================== Excel 写入 ====================

    private int writeExcel(String filePath, ExportDefinition def,
                           String exportUser, String exportTime) {
        List<List<Object>> rows = def.dataRows();
        try (ExcelWriter writer = EasyExcel.write(filePath)
                .head(def.headers())
                .registerWriteHandler(new ExcelWatermarkHandler(exportUser, exportTime))
                .build()) {
            WriteSheet sheet = EasyExcel.writerSheet(def.sheetName()).build();
            // 分批写入，每批 5000 行，避免 OOM
            int batchSize = 5000;
            for (int i = 0; i < rows.size(); i += batchSize) {
                int end = Math.min(i + batchSize, rows.size());
                writer.write(rows.subList(i, end), sheet);
            }
        }
        return rows.size();
    }

    // ==================== 导出定义路由 ====================

    private ExportDefinition resolveDefinition(String reportCode, ReportQueryParam param) {
        return switch (reportCode) {
            // ── 财务类 ──
            case ReportExportCodes.FIN_RECEIVABLE_SUMMARY ->
                    finReceivableSummary(param);
            case ReportExportCodes.FIN_RECEIPT_SUMMARY ->
                    finReceiptSummary(param);
            case ReportExportCodes.FIN_OUTSTANDING_SUMMARY ->
                    finOutstandingSummary(param);
            case ReportExportCodes.FIN_AGING_ANALYSIS ->
                    finAgingAnalysis(param);
            case ReportExportCodes.FIN_OVERDUE_RATE ->
                    finOverdueRate(param);
            case ReportExportCodes.FIN_COLLECTION_RATE ->
                    finCollectionRate(param);
            // ── 资产类 ──
            case ReportExportCodes.ASSET_SHOP_RENTAL ->
                    assetShopRental(param);
            case ReportExportCodes.ASSET_VACANCY_RATE ->
                    assetVacancyRate(param);
            case ReportExportCodes.ASSET_BRAND_DIST ->
                    assetBrandDist(param);
            // ── 营运类 ──
            case ReportExportCodes.OPR_REVENUE_SUMMARY ->
                    oprRevenueSummary(param);
            case ReportExportCodes.OPR_CONTRACT_CHANGES ->
                    oprContractChanges(param);
            // ── 招商类 ──
            case ReportExportCodes.INV_INTENTION_STATS ->
                    invIntentionStats(param);
            case ReportExportCodes.INV_PERFORMANCE ->
                    invPerformance(param);
            default -> throw new IllegalArgumentException("不支持的报表编码：" + reportCode);
        };
    }

    // ==================== 各报表数据定义 ====================

    private ExportDefinition finReceivableSummary(ReportQueryParam p) {
        List<FinReceivableSummaryVO> data = finService.receivableSummary(p);
        List<List<String>> headers = headers(
                "月份", "项目ID", "费项类型",
                "应收金额（元）", "已收金额（元）", "欠款金额（元）", "减免金额（元）",
                "调整金额（元）", "收缴率(%)", "同比收缴率(%)", "环比收缴率(%)");
        List<List<Object>> rows = data.stream().map(r -> row(
                r.getTimeDim(), r.getProjectId(), r.getFeeItemType(),
                dec(r.getReceivableAmount()), dec(r.getReceivedAmount()),
                dec(r.getOutstandingAmount()), dec(r.getDeductionAmount()),
                dec(r.getAdjustmentAmount()), pct(r.getCollectionRate()),
                pct(r.getCollectionRateYoY()), pct(r.getCollectionRateMoM())
        )).toList();
        return new ExportDefinition("应收汇总", headers, rows);
    }

    private ExportDefinition finReceiptSummary(ReportQueryParam p) {
        List<FinReceiptSummaryVO> data = finService.receiptSummary(p);
        List<List<String>> headers = headers(
                "月份", "项目ID", "费项类型",
                "应收金额（元）", "已收金额（元）", "欠款金额（元）",
                "收缴率(%)", "同比收缴率(%)", "环比收缴率(%)");
        List<List<Object>> rows = data.stream().map(r -> row(
                r.getTimeDim(), r.getProjectId(), r.getFeeItemType(),
                dec(r.getReceivableAmount()), dec(r.getReceivedAmount()),
                dec(r.getOutstandingAmount()), pct(r.getCollectionRate()),
                pct(r.getCollectionRateYoY()), pct(r.getCollectionRateMoM())
        )).toList();
        return new ExportDefinition("收款汇总", headers, rows);
    }

    private ExportDefinition finOutstandingSummary(ReportQueryParam p) {
        List<FinOutstandingSummaryVO> data = finService.outstandingSummary(p);
        List<List<String>> headers = headers(
                "月份", "项目ID", "费项类型",
                "应收金额（元）", "已收金额（元）", "欠款金额（元）",
                "逾期金额（元）", "逾期率(%)", "减免金额（元）", "调整金额（元）",
                "欠款同比(%)", "逾期率同比(%)");
        List<List<Object>> rows = data.stream().map(r -> row(
                r.getTimeDim(), r.getProjectId(), r.getFeeItemType(),
                dec(r.getReceivableAmount()), dec(r.getReceivedAmount()),
                dec(r.getOutstandingAmount()), dec(r.getOverdueAmount()),
                pct(r.getOverdueRate()), dec(r.getDeductionAmount()),
                dec(r.getAdjustmentAmount()), pct(r.getOutstandingYoY()),
                pct(r.getOverdueRateYoY())
        )).toList();
        return new ExportDefinition("欠款统计", headers, rows);
    }

    private ExportDefinition finAgingAnalysis(ReportQueryParam p) {
        List<FinAgingAnalysisVO> data = finService.agingAnalysis(p);
        List<List<String>> headers = headers(
                "统计日期", "项目ID", "商家ID", "合同ID",
                "欠款合计（元）",
                "30天内（元）", "31-60天（元）", "61-90天（元）",
                "91-180天（元）", "181-365天（元）", "365天以上（元）",
                "30天内占比(%)", "31-60天占比(%)", "61-90天占比(%)",
                "91-180天占比(%)", "181-365天占比(%)", "365天以上占比(%)");
        List<List<Object>> rows = data.stream().map(r -> row(
                r.getStatDate(), r.getProjectId(), r.getMerchantId(), r.getContractId(),
                dec(r.getTotalOutstanding()),
                dec(r.getWithin30()), dec(r.getDays3160()), dec(r.getDays6190()),
                dec(r.getDays91180()), dec(r.getDays181365()), dec(r.getOver365()),
                pct(r.getWithin30Rate()), pct(r.getDays3160Rate()), pct(r.getDays6190Rate()),
                pct(r.getDays91180Rate()), pct(r.getDays181365Rate()), pct(r.getOver365Rate())
        )).toList();
        return new ExportDefinition("账龄分析", headers, rows);
    }

    private ExportDefinition finOverdueRate(ReportQueryParam p) {
        List<FinOverdueRateVO> data = finService.overdueRate(p);
        List<List<String>> headers = headers(
                "月份", "项目ID", "应收金额（元）", "逾期金额（元）",
                "逾期率(%)", "同比逾期率(%)", "环比逾期率(%)", "逾期金额同比(%)");
        List<List<Object>> rows = data.stream().map(r -> row(
                r.getTimeDim(), r.getProjectId(),
                dec(r.getReceivableAmount()), dec(r.getOverdueAmount()),
                pct(r.getOverdueRate()), pct(r.getOverdueRateYoY()),
                pct(r.getOverdueRateMoM()), pct(r.getOverdueAmountYoY())
        )).toList();
        return new ExportDefinition("逾期率统计", headers, rows);
    }

    private ExportDefinition finCollectionRate(ReportQueryParam p) {
        List<FinCollectionRateVO> data = finService.collectionRate(p);
        List<List<String>> headers = headers(
                "月份", "项目ID", "费项类型",
                "应收金额（元）", "已收金额（元）", "欠款金额（元）",
                "收缴率(%)", "同比收缴率(%)", "环比收缴率(%)");
        List<List<Object>> rows = data.stream().map(r -> row(
                r.getTimeDim(), r.getProjectId(), r.getFeeItemType(),
                dec(r.getReceivableAmount()), dec(r.getReceivedAmount()),
                dec(r.getOutstandingAmount()), pct(r.getCollectionRate()),
                pct(r.getCollectionRateYoY()), pct(r.getCollectionRateMoM())
        )).toList();
        return new ExportDefinition("收缴率统计", headers, rows);
    }

    private ExportDefinition assetShopRental(ReportQueryParam p) {
        List<ShopRentalVO> data = assetService.shopRental(p).getRecords();
        List<List<String>> headers = headers(
                "统计日期", "项目ID", "楼栋ID", "楼层ID", "业态",
                "商铺总数", "已租商铺数", "空置商铺数", "已开业商铺数",
                "总面积（㎡）", "已租面积（㎡）", "空置面积（㎡）",
                "出租率(%)", "空置率(%)", "开业率(%)");
        List<List<Object>> rows = data.stream().map(r -> row(
                r.getStatDate(), r.getProjectId(), r.getBuildingId(), r.getFloorId(), r.getFormatType(),
                r.getTotalShops(), r.getRentedShops(), r.getVacantShops(), r.getOpenedShops(),
                dec(r.getTotalArea()), dec(r.getRentedArea()), dec(r.getVacantArea()),
                pct(r.getRentalRate()), pct(r.getVacancyRate()), pct(r.getOpeningRate())
        )).toList();
        return new ExportDefinition("商铺租赁信息", headers, rows);
    }

    private ExportDefinition assetVacancyRate(ReportQueryParam p) {
        List<RateTrendVO> data = assetService.vacancyRate(p);
        List<List<String>> headers = headers(
                "时间维度", "当期空置率(%)", "对比期空置率(%)",
                "增长率(%)", "商铺总数", "已租商铺数",
                "总面积（㎡）", "已租面积（㎡）");
        List<List<Object>> rows = data.stream().map(r -> row(
                r.getTimeDim(), pct(r.getValue()), pct(r.getPrevValue()),
                pct(r.getGrowthRate()), r.getTotalShops(), r.getRentedShops(),
                dec(r.getTotalArea()), dec(r.getRentedArea())
        )).toList();
        return new ExportDefinition("空置率统计", headers, rows);
    }

    private ExportDefinition assetBrandDist(ReportQueryParam p) {
        List<BrandDistributionVO> data = assetService.brandDistribution(p);
        List<List<String>> headers = headers(
                "业态", "商铺总数", "已租商铺数", "空置商铺数", "已开业商铺数",
                "总面积（㎡）", "已租面积（㎡）",
                "出租率(%)", "空置率(%)", "商铺数占比(%)", "面积占比(%)");
        List<List<Object>> rows = data.stream().map(r -> row(
                r.getFormatType(), r.getTotalShops(), r.getRentedShops(),
                r.getVacantShops(), r.getOpenedShops(),
                dec(r.getTotalArea()), dec(r.getRentedArea()),
                pct(r.getRentalRate()), pct(r.getVacancyRate()),
                pct(r.getShopPercentage()), pct(r.getAreaPercentage())
        )).toList();
        return new ExportDefinition("品牌分布", headers, rows);
    }

    private ExportDefinition oprRevenueSummary(ReportQueryParam p) {
        List<OprRevenueSummaryVO> data = oprService.revenueSummary(p);
        List<List<String>> headers = headers(
                "月份", "项目ID", "业态",
                "营收金额（元）", "浮动租金（元）", "坪效（元/㎡）",
                "同比增长(%)", "环比增长(%)");
        List<List<Object>> rows = data.stream().map(r -> row(
                r.getTimeDim(), r.getProjectId(), r.getFormatType(),
                dec(r.getRevenueAmount()), dec(r.getFloatingRentAmount()),
                dec(r.getAvgRevenuePerSqm()),
                pct(r.getRevenueGrowthRate()), pct(r.getPrevRevenueAmount())
        )).toList();
        return new ExportDefinition("营收汇总", headers, rows);
    }

    private ExportDefinition oprContractChanges(ReportQueryParam p) {
        List<OprContractChangeVO> data = oprService.contractChanges(p);
        List<List<String>> headers = headers(
                "月份", "项目ID", "业态", "变更数量",
                "上期变更数量", "变更数量增长率(%)", "租金影响额（元）");
        List<List<Object>> rows = data.stream().map(r -> row(
                r.getTimeDim(), r.getProjectId(), r.getFormatType(),
                r.getChangeCount(), r.getPrevChangeCount(),
                pct(r.getChangeCountGrowthRate()), dec(r.getChangeRentImpact())
        )).toList();
        return new ExportDefinition("合同变更分析", headers, rows);
    }

    private ExportDefinition invIntentionStats(ReportQueryParam p) {
        List<IntentionStatsVO> data = invService.intentionStats(p);
        List<List<String>> headers = headers(
                "时间维度", "意向协议数", "已签意向数", "新增意向数",
                "签约率(%)", "对比期意向数", "增长率(%)");
        List<List<Object>> rows = data.stream().map(r -> row(
                r.getTimeDim(), r.getIntentionCount(), r.getIntentionSigned(),
                r.getNewIntention(), pct(r.getSignedRate()),
                r.getPrevIntentionCount(), pct(r.getGrowthRate())
        )).toList();
        return new ExportDefinition("意向客户统计", headers, rows);
    }

    private ExportDefinition invPerformance(ReportQueryParam p) {
        List<PerformanceVO> data = invService.performance(p);
        List<List<String>> headers = headers(
                "统计日期", "项目ID", "招商负责人ID", "合同签约面积（㎡）", "合同数量",
                "意向转化率(%)", "平均租金单价（元/㎡）");
        List<List<Object>> rows = data.stream().map(r -> row(
                r.getStatDate(), r.getProjectId(), r.getInvestmentManagerId(),
                dec(r.getContractArea()), r.getContractCount(),
                pct(r.getConversionRate()), dec(r.getAvgRentPrice())
        )).toList();
        return new ExportDefinition("招商业绩对比", headers, rows);
    }

    // ==================== 辅助方法 ====================

    private String generateLogCode() {
        String ts = LocalDateTime.now().format(LOG_CODE_FMT);
        int seq = SEQ.incrementAndGet() % 1000;
        return String.format("LOG_%s_%03d", ts, seq);
    }

    private String buildFileName(String reportCode, String format) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String ext = RptGenerationLog.FORMAT_PDF.equals(format) ? ".pdf" : ".xlsx";
        return reportCode.toLowerCase().replace('_', '-') + "_" + ts + ext;
    }

    private String buildCacheKey(ExportTaskDTO dto) {
        try {
            TreeMap<String, Object> sorted = dto.getParams() == null
                    ? new TreeMap<>() : new TreeMap<>(dto.getParams());
            String raw = dto.getReportCode() + "|" + dto.getFormat() + "|"
                    + objectMapper.writeValueAsString(sorted);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return dto.getReportCode() + "_" + System.currentTimeMillis();
        }
    }

    private String serializeParams(Map<String, Object> params) {
        try {
            return params == null ? "{}" : objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            return "{}";
        }
    }

    private ReportQueryParam buildQueryParam(Map<String, Object> params) {
        if (params == null) return new ReportQueryParam();
        try {
            String json = objectMapper.writeValueAsString(params);
            return objectMapper.readValue(json, ReportQueryParam.class);
        } catch (Exception e) {
            log.warn("[Export] 参数转换失败，使用空参数", e);
            return new ReportQueryParam();
        }
    }

    /** 构建 EasyExcel 单级列头（每列一个字符串） */
    private static List<List<String>> headers(String... cols) {
        List<List<String>> result = new ArrayList<>();
        for (String col : cols) {
            result.add(Collections.singletonList(col));
        }
        return result;
    }

    /** 构建一行数据 */
    private static List<Object> row(Object... values) {
        return new ArrayList<>(Arrays.asList(values));
    }

    /** BigDecimal → String 保留两位小数（null 时返回空串） */
    private static String dec(BigDecimal v) {
        return v == null ? "" : v.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    /** Number → 百分比字符串（null 时返回空串） */
    private static String pct(BigDecimal v) {
        return v == null ? "" : v.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private static String pct(Number v) {
        if (v == null) return "";
        BigDecimal bd = v instanceof BigDecimal ? (BigDecimal) v : new BigDecimal(v.toString());
        return bd.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private static String dec(Number v) {
        if (v == null) return "";
        BigDecimal bd = v instanceof BigDecimal ? (BigDecimal) v : new BigDecimal(v.toString());
        return bd.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    // ==================== 同步文件生成（供定时推送 Job 调用）====================

    @Override
    public String generateFileSync(String reportCode, String paramsJson,
                                   String format, String logCode) {
        if (RptGenerationLog.FORMAT_PDF.equals(format)) {
            throw new UnsupportedOperationException("PDF 导出暂未支持，请选择 Excel 格式");
        }

        // 解析参数
        Map<String, Object> paramsMap;
        try {
            if (paramsJson == null || paramsJson.isBlank() || "{}".equals(paramsJson)) {
                paramsMap = new java.util.HashMap<>();
            } else {
                paramsMap = objectMapper.readValue(paramsJson,
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.warn("[Export-Sync] 参数解析失败，使用空参数: {}", e.getMessage());
            paramsMap = new java.util.HashMap<>();
        }

        ReportQueryParam param = buildQueryParam(paramsMap);
        ExportDefinition def = resolveDefinition(reportCode, param);

        String dir = exportConfig.getOrCreateExportDir();
        String filePath = dir + File.separator + logCode + ".xlsx";
        String exportTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .format(LocalDateTime.now());
        writeExcel(filePath, def, "定时任务", exportTime);

        log.info("[Export-Sync] 文件生成成功：reportCode={}, path={}", reportCode, filePath);
        return filePath;
    }
}
