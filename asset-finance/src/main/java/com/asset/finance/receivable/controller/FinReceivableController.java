package com.asset.finance.receivable.controller;

import com.asset.common.model.R;
import com.asset.finance.receivable.dto.AdjustmentCreateDTO;
import com.asset.finance.receivable.dto.DeductionCreateDTO;
import com.asset.finance.receivable.dto.ReceivableQueryDTO;
import com.asset.finance.receivable.service.FinReceivableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "01-应收管理")
@RestController
@RequestMapping("/fin/receivables")
@RequiredArgsConstructor
public class FinReceivableController {

    private final FinReceivableService receivableService;

    @Operation(summary = "应收明细分页列表")
    @GetMapping
    public R<?> page(ReceivableQueryDTO query) { return R.ok(receivableService.pageQuery(query)); }

    @Operation(summary = "应收明细详情")
    @GetMapping("/{id}")
    public R<?> getById(@PathVariable Long id) { return R.ok(receivableService.getDetailById(id)); }

    @Operation(summary = "应收汇总（按合同）")
    @GetMapping("/summary")
    public R<?> summary(ReceivableQueryDTO query) { return R.ok(receivableService.summaryByContract(query)); }

    @Operation(summary = "欠费统计")
    @GetMapping("/overdue-statistics")
    public R<?> overdueStatistics(@RequestParam(required = false) Long projectId) {
        return R.ok(receivableService.overdueStatistics(projectId));
    }

    @Operation(summary = "导出应收明细Excel")
    @GetMapping("/export")
    public void exportExcel(ReceivableQueryDTO query, HttpServletResponse response) {
        receivableService.exportExcel(query, response);
    }

    @Operation(summary = "从营运计划同步应收（幂等）")
    @PostMapping("/sync-from-plan/{planId}")
    public R<?> syncFromPlan(@PathVariable Long planId) {
        receivableService.syncFromPlan(planId);
        return R.ok();
    }

    @Operation(summary = "手动刷新逾期天数")
    @PostMapping("/refresh-overdue")
    public R<?> refreshOverdue() {
        receivableService.refreshOverdueDays();
        return R.ok();
    }

    // ─── 减免管理 ─────────────────────────────────────────────────────────────

    @Operation(summary = "提交减免申请")
    @PostMapping("/deduction")
    public R<Long> applyDeduction(@RequestBody @Valid DeductionCreateDTO dto) {
        return R.ok(receivableService.applyDeduction(dto));
    }

    @Operation(summary = "减免审批回调")
    @PutMapping("/deduction/{id}/callback")
    public R<Void> deductionCallback(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String approvalId = (String) body.get("approvalId");
        boolean approved = Boolean.TRUE.equals(body.get("approved"));
        receivableService.deductionCallback(approvalId, approved);
        return R.ok();
    }

    // ─── 调整管理 ─────────────────────────────────────────────────────────────

    @Operation(summary = "提交调整申请")
    @PostMapping("/adjustment")
    public R<Long> applyAdjustment(@RequestBody @Valid AdjustmentCreateDTO dto) {
        return R.ok(receivableService.applyAdjustment(dto));
    }

    @Operation(summary = "调整审批回调")
    @PutMapping("/adjustment/{id}/callback")
    public R<Void> adjustmentCallback(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String approvalId = (String) body.get("approvalId");
        boolean approved = Boolean.TRUE.equals(body.get("approved"));
        receivableService.adjustmentCallback(approvalId, approved);
        return R.ok();
    }

    // ─── 账单打印 ─────────────────────────────────────────────────────────────

    @Operation(summary = "标记账单为已打印")
    @PostMapping("/mark-printed")
    public R<Void> markPrinted(@RequestBody List<Long> ids) {
        receivableService.markPrinted(ids);
        return R.ok();
    }
}
