package com.asset.finance.dashboard.controller;

import com.asset.common.model.R;
import com.asset.finance.dashboard.dto.DashboardSummaryVO;
import com.asset.finance.dashboard.dto.OverdueTopVO;
import com.asset.finance.dashboard.dto.ReceiptTrendVO;
import com.asset.finance.dashboard.service.FinDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 财务看板 Controller
 */
@Tag(name = "07-财务看板")
@RestController
@RequestMapping("/fin/dashboard")
@RequiredArgsConstructor
public class FinDashboardController {

    private final FinDashboardService dashboardService;

    /**
     * 汇总数据：四卡片 + 两饼图数据
     */
    @Operation(summary = "财务看板汇总")
    @GetMapping("/summary")
    public R<DashboardSummaryVO> summary() {
        return R.ok(dashboardService.getSummary());
    }

    /**
     * 近12个月收款趋势（折线图）
     */
    @Operation(summary = "近12个月收款趋势")
    @GetMapping("/receipt-trend")
    public R<List<ReceiptTrendVO>> receiptTrend() {
        return R.ok(dashboardService.getReceiptTrend());
    }

    /**
     * 欠费 TOP10 商家（水平柱状图）
     */
    @Operation(summary = "欠费TOP10商家")
    @GetMapping("/overdue-top")
    public R<List<OverdueTopVO>> overdueTop() {
        return R.ok(dashboardService.getOverdueTop());
    }
}
