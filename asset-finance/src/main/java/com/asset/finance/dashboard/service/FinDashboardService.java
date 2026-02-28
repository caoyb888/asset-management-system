package com.asset.finance.dashboard.service;

import com.asset.finance.dashboard.dto.DashboardSummaryVO;
import com.asset.finance.dashboard.dto.OverdueTopVO;
import com.asset.finance.dashboard.dto.ReceiptTrendVO;

import java.util.List;

/**
 * 财务看板 Service
 */
public interface FinDashboardService {

    /** 汇总数据：四卡片 + 两饼图数据 */
    DashboardSummaryVO getSummary();

    /** 近12个月收款趋势（折线图） */
    List<ReceiptTrendVO> getReceiptTrend();

    /** 欠费 TOP10 商家（水平柱状图） */
    List<OverdueTopVO> getOverdueTop();
}
