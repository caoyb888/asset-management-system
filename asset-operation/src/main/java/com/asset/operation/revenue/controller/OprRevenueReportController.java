package com.asset.operation.revenue.controller;

import com.asset.common.model.R;
import com.asset.operation.revenue.dto.GenerateFloatingRentDTO;
import com.asset.operation.revenue.dto.RevenueReportCreateDTO;
import com.asset.operation.revenue.dto.RevenueReportQueryDTO;
import com.asset.operation.revenue.service.OprFloatingRentService;
import com.asset.operation.revenue.service.OprRevenueReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 营收填报 + 浮动租金 Controller
 * 路径前缀：/opr/revenue-reports  /opr/floating-rents
 */
@Tag(name = "03-营收填报与浮动租金")
@RestController
@RequiredArgsConstructor
public class OprRevenueReportController {

    private final OprRevenueReportService revenueReportService;
    private final OprFloatingRentService floatingRentService;

    // ── 营收填报 CRUD ────────────────────────────────────────────

    @Operation(summary = "营收填报 - 分页列表")
    @GetMapping("/opr/revenue-reports")
    public R<?> page(RevenueReportQueryDTO query) {
        return R.ok(revenueReportService.pageQuery(query));
    }

    @Operation(summary = "营收填报 - 新增（单日录入）")
    @PostMapping("/opr/revenue-reports")
    public R<?> create(@Valid @RequestBody RevenueReportCreateDTO dto) {
        return R.ok(revenueReportService.saveReport(dto));
    }

    @Operation(summary = "营收填报 - 修改（仅待确认可改）")
    @PutMapping("/opr/revenue-reports/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody RevenueReportCreateDTO dto) {
        revenueReportService.updateReport(id, dto);
        return R.ok();
    }

    // ── 导入/导出 ──────────────────────────────────────────────

    @Operation(summary = "营收填报 - Excel 批量导入")
    @PostMapping("/opr/revenue-reports/import")
    public R<?> importExcel(@RequestParam("file") MultipartFile file) {
        return R.ok(revenueReportService.importExcel(file));
    }

    @Operation(summary = "营收填报 - 导出 Excel")
    @GetMapping("/opr/revenue-reports/export")
    public void exportExcel(RevenueReportQueryDTO query, HttpServletResponse response) {
        revenueReportService.exportExcel(query, response);
    }

    @Operation(summary = "营收填报 - 下载导入模板")
    @GetMapping("/opr/revenue-reports/template")
    public void downloadTemplate(HttpServletResponse response) {
        revenueReportService.downloadTemplate(response);
    }

    // ── 日历视图辅助 ──────────────────────────────────────────

    @Operation(summary = "营收填报 - 查询指定合同指定月份每日明细（供日历视图着色）")
    @GetMapping("/opr/revenue-reports/daily-detail")
    public R<?> dailyDetail(@RequestParam Long contractId, @RequestParam String reportMonth) {
        return R.ok(revenueReportService.getDailyDetail(contractId, reportMonth));
    }

    // ── 浮动租金计算 ──────────────────────────────────────────

    @Operation(summary = "触发浮动租金计算（月度）")
    @PostMapping("/opr/revenue-reports/generate-floating-rent")
    public R<?> generateFloatingRent(@Valid @RequestBody GenerateFloatingRentDTO dto) {
        Long floatingRentId = revenueReportService.generateFloatingRent(dto);
        return R.ok(floatingRentId);
    }

    // ── 月度统计 ──────────────────────────────────────────────

    @Operation(summary = "营收月度汇总统计")
    @GetMapping("/opr/revenue-reports/statistics")
    public R<?> statistics(
            @RequestParam String reportMonth,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long contractId) {
        return R.ok(revenueReportService.getMonthlyStatistics(reportMonth, projectId, contractId));
    }

    // ── 浮动租金列表/详情 ──────────────────────────────────────

    @Operation(summary = "浮动租金 - 分页列表")
    @GetMapping("/opr/floating-rents")
    public R<?> floatingRentList(
            @RequestParam(required = false) Long contractId,
            @RequestParam(required = false) String calcMonth,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(floatingRentService.pageQuery(contractId, calcMonth, pageNum, pageSize));
    }

    @Operation(summary = "浮动租金 - 详情（含阶梯明细）")
    @GetMapping("/opr/floating-rents/{id}")
    public R<?> floatingRentDetail(@PathVariable Long id) {
        return R.ok(floatingRentService.detail(id));
    }

    @Operation(summary = "浮动租金 - 手动生成应收计划")
    @PostMapping("/opr/floating-rents/{id}/generate-receivable")
    public R<?> generateReceivable(@PathVariable Long id) {
        Long receivableId = floatingRentService.generateReceivable(id);
        return R.ok(receivableId);
    }
}
