package com.asset.operation.revenue.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.asset.common.exception.BizException;
import com.asset.operation.engine.FloatingRentCalculator;
import com.asset.operation.revenue.dto.*;
import com.asset.operation.revenue.entity.OprRevenueReport;
import com.asset.operation.revenue.excel.RevenueReportExcel;
import com.asset.operation.revenue.mapper.OprRevenueReportMapper;
import com.asset.operation.revenue.service.OprRevenueReportService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * 营收填报 Service 实现
 * 核心功能：按日录入营业额 / Excel 批量导入 / 触发浮动租金计算 / 月度统计
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OprRevenueReportServiceImpl extends ServiceImpl<OprRevenueReportMapper, OprRevenueReport>
        implements OprRevenueReportService {

    private final FloatingRentCalculator floatingRentCalculator;
    private final JdbcTemplate jdbcTemplate;

    // ── 查询 ────────────────────────────────────────────────────

    @Override
    public IPage<OprRevenueReport> pageQuery(RevenueReportQueryDTO query) {
        LambdaQueryWrapper<OprRevenueReport> wrapper = new LambdaQueryWrapper<OprRevenueReport>()
                .eq(query.getProjectId() != null,  OprRevenueReport::getProjectId,  query.getProjectId())
                .eq(query.getContractId() != null, OprRevenueReport::getContractId, query.getContractId())
                .eq(query.getMerchantId() != null, OprRevenueReport::getMerchantId, query.getMerchantId())
                .eq(query.getShopId() != null,     OprRevenueReport::getShopId,     query.getShopId())
                .eq(query.getReportMonth() != null, OprRevenueReport::getReportMonth, query.getReportMonth())
                .eq(query.getStatus() != null,     OprRevenueReport::getStatus,     query.getStatus())
                .ge(query.getReportDateFrom() != null,
                        OprRevenueReport::getReportDate, query.getReportDateFrom())
                .le(query.getReportDateTo() != null,
                        OprRevenueReport::getReportDate, query.getReportDateTo())
                .orderByDesc(OprRevenueReport::getReportDate);

        return page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    @Override
    public Map<String, Object> getDailyDetail(Long contractId, String reportMonth) {
        List<OprRevenueReport> list = list(
                new LambdaQueryWrapper<OprRevenueReport>()
                        .eq(OprRevenueReport::getContractId, contractId)
                        .eq(OprRevenueReport::getReportMonth, reportMonth)
                        .orderByAsc(OprRevenueReport::getReportDate));

        Map<String, Object> result = new LinkedHashMap<>();
        for (OprRevenueReport r : list) {
            result.put(r.getReportDate().toString(), r.getRevenueAmount());
        }
        return result;
    }

    // ── 写操作 ──────────────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OprRevenueReport saveReport(RevenueReportCreateDTO dto) {
        // 应用层唯一性校验（MBP 逻辑删除已自动过滤 is_deleted=1）
        if (count(new LambdaQueryWrapper<OprRevenueReport>()
                .eq(OprRevenueReport::getContractId, dto.getContractId())
                .eq(OprRevenueReport::getReportDate, dto.getReportDate())) > 0) {
            throw new BizException("该合同当日营业额已填报，请勿重复录入（"
                    + dto.getReportDate() + "）");
        }

        // 补全合同关联信息
        fillContractInfo(dto);

        OprRevenueReport entity = new OprRevenueReport();
        entity.setContractId(dto.getContractId());
        entity.setProjectId(dto.getProjectId());
        entity.setShopId(dto.getShopId());
        entity.setMerchantId(dto.getMerchantId());
        entity.setReportDate(dto.getReportDate());
        entity.setReportMonth(dto.getReportDate().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        entity.setRevenueAmount(dto.getRevenueAmount());
        entity.setStatus(0); // 待确认
        save(entity);
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReport(Long id, RevenueReportCreateDTO dto) {
        OprRevenueReport exist = getById(id);
        if (exist == null) throw new BizException("营收记录不存在，id=" + id);
        if (exist.getStatus() != 0) throw new BizException("仅待确认状态的记录可修改");
        exist.setRevenueAmount(dto.getRevenueAmount());
        updateById(exist);
    }

    // ── Excel 导入 ──────────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importExcel(MultipartFile file) {
        List<String> errorList = new ArrayList<>();
        List<OprRevenueReport> toSave  = new ArrayList<>();

        // 查询合同编号→ID 映射
        Map<String, Long> contractCodeMap = buildContractCodeMap();

        try {
            EasyExcel.read(file.getInputStream(), RevenueReportExcel.class,
                    new AnalysisEventListener<RevenueReportExcel>() {
                        int rowNo = 2; // 从第2行开始（第1行为表头）

                        @Override
                        public void invoke(RevenueReportExcel row, AnalysisContext ctx) {
                            String err = validateRow(row, contractCodeMap, rowNo);
                            if (err != null) {
                                errorList.add(err);
                            } else {
                                Long contractId = contractCodeMap.get(row.getContractCode().trim());
                                LocalDate date  = LocalDate.parse(row.getReportDate().trim());

                                // 重复检测（先查DB，再查本批次）
                                if (count(new LambdaQueryWrapper<OprRevenueReport>()
                                        .eq(OprRevenueReport::getContractId, contractId)
                                        .eq(OprRevenueReport::getReportDate, date)) > 0) {
                                    errorList.add("第" + rowNo + "行：合同" + row.getContractCode()
                                            + " 日期" + row.getReportDate() + " 已存在，跳过");
                                } else {
                                    OprRevenueReport rr = new OprRevenueReport();
                                    rr.setContractId(contractId);
                                    rr.setReportDate(date);
                                    rr.setReportMonth(date.format(DateTimeFormatter.ofPattern("yyyy-MM")));
                                    rr.setRevenueAmount(new BigDecimal(row.getRevenueAmount().trim()));
                                    rr.setStatus(0);
                                    toSave.add(rr);
                                }
                            }
                            rowNo++;
                        }

                        @Override
                        public void doAfterAllAnalysed(AnalysisContext ctx) {
                            log.info("[营收导入] 解析完成，有效行={}", toSave.size());
                        }
                    }).sheet().headRowNumber(1).doRead();
        } catch (IOException e) {
            throw new BizException("Excel 文件读取失败：" + e.getMessage());
        }

        // 批量保存有效数据
        for (OprRevenueReport r : toSave) {
            save(r);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", toSave.size());
        result.put("errorList", errorList);
        return result;
    }

    /** 导入行校验，返回错误信息（无误返回null） */
    private String validateRow(RevenueReportExcel row, Map<String, Long> contractCodeMap, int rowNo) {
        if (row.getContractCode() == null || row.getContractCode().isBlank()) {
            return "第" + rowNo + "行：合同编号不能为空";
        }
        if (!contractCodeMap.containsKey(row.getContractCode().trim())) {
            return "第" + rowNo + "行：合同编号" + row.getContractCode() + "不存在";
        }
        if (row.getReportDate() == null || row.getReportDate().isBlank()) {
            return "第" + rowNo + "行：填报日期不能为空";
        }
        try {
            LocalDate.parse(row.getReportDate().trim());
        } catch (DateTimeParseException e) {
            return "第" + rowNo + "行：日期格式错误（需YYYY-MM-DD），实际=" + row.getReportDate();
        }
        if (row.getRevenueAmount() == null || row.getRevenueAmount().isBlank()) {
            return "第" + rowNo + "行：营业额不能为空";
        }
        try {
            BigDecimal amount = new BigDecimal(row.getRevenueAmount().trim());
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                return "第" + rowNo + "行：营业额不能为负数";
            }
            // 异常值检测（超历史均值3倍触发警告，不阻断）
            // TODO: 可对接历史均值查询进行预警
        } catch (NumberFormatException e) {
            return "第" + rowNo + "行：营业额格式错误，实际=" + row.getRevenueAmount();
        }
        return null;
    }

    /** 构建合同编号→ID映射 */
    private Map<String, Long> buildContractCodeMap() {
        List<Map<String, Object>> contracts = jdbcTemplate.queryForList(
                "SELECT id, contract_code FROM inv_lease_contract WHERE is_deleted=0 AND is_current=1");
        Map<String, Long> map = new HashMap<>();
        for (Map<String, Object> row : contracts) {
            map.put((String) row.get("contract_code"), ((Number) row.get("id")).longValue());
        }
        return map;
    }

    // ── Excel 导出 ──────────────────────────────────────────────

    @Override
    public void exportExcel(RevenueReportQueryDTO query, HttpServletResponse response) {
        query.setPageSize(10000); // 最多导出10000条
        IPage<OprRevenueReport> page = pageQuery(query);

        List<RevenueReportExcel> excelList = new ArrayList<>();
        for (OprRevenueReport r : page.getRecords()) {
            RevenueReportExcel excel = new RevenueReportExcel();
            excel.setReportDate(r.getReportDate() != null ? r.getReportDate().toString() : "");
            excel.setRevenueAmount(r.getRevenueAmount() != null ? r.getRevenueAmount().toPlainString() : "0");
            excelList.add(excel);
        }

        setExcelResponse(response, "营收填报");
        try {
            EasyExcel.write(response.getOutputStream(), RevenueReportExcel.class)
                    .sheet("营收填报").doWrite(excelList);
        } catch (IOException e) {
            throw new BizException("导出失败：" + e.getMessage());
        }
    }

    @Override
    public void downloadTemplate(HttpServletResponse response) {
        // 写一行示例数据
        List<RevenueReportExcel> sample = List.of(
                buildSampleRow("HT202402010001", "SP-A101", "示例商家", "2026-01-01", "50000.00"));
        setExcelResponse(response, "营收填报导入模板");
        try {
            EasyExcel.write(response.getOutputStream(), RevenueReportExcel.class)
                    .sheet("填报模板").doWrite(sample);
        } catch (IOException e) {
            throw new BizException("模板下载失败：" + e.getMessage());
        }
    }

    private RevenueReportExcel buildSampleRow(String contractCode, String shopCode,
                                               String merchantName, String date, String amount) {
        RevenueReportExcel row = new RevenueReportExcel();
        row.setContractCode(contractCode);
        row.setShopCode(shopCode);
        row.setMerchantName(merchantName);
        row.setReportDate(date);
        row.setRevenueAmount(amount);
        row.setErrorMsg("");
        return row;
    }

    // ── 浮动租金 ────────────────────────────────────────────────

    @Override
    public Long generateFloatingRent(GenerateFloatingRentDTO dto) {
        return floatingRentCalculator.calculate(dto.getContractId(), dto.getCalcMonth());
    }

    // ── 月度统计 ────────────────────────────────────────────────

    @Override
    public RevenueStatisticsVO getMonthlyStatistics(String reportMonth, Long projectId, Long contractId) {
        // 月总天数
        YearMonth ym = YearMonth.parse(reportMonth);
        int totalDays = ym.lengthOfMonth();

        // 按合同汇总
        StringBuilder sql = new StringBuilder("""
                SELECT rr.contract_id,
                       rr.shop_id,
                       COALESCE(SUM(rr.revenue_amount), 0) AS monthly_revenue,
                       COUNT(DISTINCT rr.report_date)     AS report_days
                FROM opr_revenue_report rr
                WHERE rr.report_month = ? AND rr.is_deleted = 0
                """);
        List<Object> params = new ArrayList<>();
        params.add(reportMonth);
        if (projectId != null) { sql.append(" AND rr.project_id = ?"); params.add(projectId); }
        if (contractId != null) { sql.append(" AND rr.contract_id = ?"); params.add(contractId); }
        sql.append(" GROUP BY rr.contract_id, rr.shop_id");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params.toArray());

        // 合同编号映射
        Map<Long, String> contractCodeMap = buildContractCodeMapById();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        List<RevenueStatisticsVO.ContractMonthlyVO> details = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            Long cid           = ((Number) row.get("contract_id")).longValue();
            BigDecimal revenue = new BigDecimal(row.get("monthly_revenue").toString());
            int reportDaysNum  = ((Number) row.get("report_days")).intValue();

            totalRevenue = totalRevenue.add(revenue);

            RevenueStatisticsVO.ContractMonthlyVO vo = new RevenueStatisticsVO.ContractMonthlyVO();
            vo.setContractId(cid);
            vo.setContractCode(contractCodeMap.getOrDefault(cid, cid.toString()));
            vo.setShopId(row.get("shop_id") != null ? ((Number) row.get("shop_id")).longValue() : null);
            vo.setMonthlyRevenue(revenue);
            vo.setReportDays(reportDaysNum);
            vo.setTotalDays(totalDays);
            vo.setComplete(reportDaysNum >= totalDays);
            details.add(vo);
        }

        RevenueStatisticsVO result = new RevenueStatisticsVO();
        result.setReportMonth(reportMonth);
        result.setTotalRevenue(totalRevenue);
        result.setReportedContractCount(details.size());
        result.setTotalDays(totalDays);
        result.setDetails(details);
        return result;
    }

    // ── 私有辅助 ────────────────────────────────────────────────

    /** 从合同表补全 projectId / shopId / merchantId */
    private void fillContractInfo(RevenueReportCreateDTO dto) {
        try {
            Map<String, Object> contractInfo = jdbcTemplate.queryForMap(
                    "SELECT c.project_id, c.merchant_id, s.shop_id " +
                    "FROM inv_lease_contract c " +
                    "LEFT JOIN inv_lease_contract_shop s ON s.contract_id=c.id AND s.is_deleted=0 " +
                    "WHERE c.id=? AND c.is_deleted=0 LIMIT 1",
                    dto.getContractId());
            if (dto.getProjectId() == null && contractInfo.get("project_id") != null) {
                dto.setProjectId(((Number) contractInfo.get("project_id")).longValue());
            }
            if (dto.getMerchantId() == null && contractInfo.get("merchant_id") != null) {
                dto.setMerchantId(((Number) contractInfo.get("merchant_id")).longValue());
            }
            if (dto.getShopId() == null && contractInfo.get("shop_id") != null) {
                dto.setShopId(((Number) contractInfo.get("shop_id")).longValue());
            }
        } catch (Exception e) {
            log.warn("[营收填报] 补全合同信息失败，contractId={}", dto.getContractId(), e);
        }
    }

    /** 合同ID→编号映射 */
    private Map<Long, String> buildContractCodeMapById() {
        List<Map<String, Object>> contracts = jdbcTemplate.queryForList(
                "SELECT id, contract_code FROM inv_lease_contract WHERE is_deleted=0 AND is_current=1");
        Map<Long, String> map = new HashMap<>();
        for (Map<String, Object> row : contracts) {
            map.put(((Number) row.get("id")).longValue(), (String) row.get("contract_code"));
        }
        return map;
    }

    /** 设置 Excel 下载响应头 */
    private void setExcelResponse(HttpServletResponse response, String fileName) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + encodedName + ".xlsx");
    }
}
