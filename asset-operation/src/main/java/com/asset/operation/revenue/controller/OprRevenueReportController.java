package com.asset.operation.revenue.controller;

import com.asset.common.model.R;
import com.asset.operation.revenue.service.OprRevenueReportService;
import com.asset.operation.revenue.service.OprFloatingRentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 营收填报 Controller
 * 阶段三（第4周）实现：营收CRUD/导入导出/浮动租金计算
 */
@Tag(name = "03-营收填报与浮动租金")
@RestController
@RequestMapping("/opr/revenue-reports")
@RequiredArgsConstructor
public class OprRevenueReportController {

    private final OprRevenueReportService revenueReportService;
    private final OprFloatingRentService floatingRentService;

    @Operation(summary = "营收列表查询")
    @GetMapping
    public R<?> page() {
        // TODO 阶段三实现
        return R.ok();
    }

    @Operation(summary = "新增营收填报")
    @PostMapping
    public R<?> create(@RequestBody Object dto) {
        // TODO 阶段三实现
        return R.ok();
    }

    @Operation(summary = "修改营收填报（仅待确认可改）")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @RequestBody Object dto) {
        // TODO 阶段三实现
        return R.ok();
    }

    @Operation(summary = "批量导入营收（Excel）")
    @PostMapping("/import")
    public R<?> importExcel(@RequestParam("file") MultipartFile file) {
        // TODO 阶段三实现
        return R.ok();
    }

    @Operation(summary = "导出营收报表（Excel）")
    @GetMapping("/export")
    public void exportExcel() {
        // TODO 阶段三实现
    }

    @Operation(summary = "下载导入模板")
    @GetMapping("/template")
    public void downloadTemplate() {
        // TODO 阶段三实现
    }

    @Operation(summary = "触发浮动租金计算（月度）")
    @PostMapping("/generate-floating-rent")
    public R<?> generateFloatingRent(@RequestBody Object dto) {
        // TODO 阶段三实现
        return R.ok();
    }

    @Operation(summary = "营收月度汇总统计")
    @GetMapping("/statistics")
    public R<?> statistics() {
        // TODO 阶段三实现
        return R.ok();
    }
}
