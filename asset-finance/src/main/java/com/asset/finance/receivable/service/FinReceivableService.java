package com.asset.finance.receivable.service;
import com.asset.finance.receivable.dto.*;
import com.asset.finance.receivable.entity.FinReceivable;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
public interface FinReceivableService extends IService<FinReceivable> {
    IPage<ReceivableDetailVO> pageQuery(ReceivableQueryDTO query);
    ReceivableDetailVO getDetailById(Long id);
    List<ReceivableSummaryVO> summaryByContract(ReceivableQueryDTO query);
    OverdueStatisticsVO overdueStatistics(Long projectId);
    void exportExcel(ReceivableQueryDTO query, HttpServletResponse response);
    /** 从营运模块应收计划同步推送生成应收明细（幂等，按planId） */
    void syncFromPlan(Long planId);
    /** 刷新逾期天数（每日定时任务） */
    void refreshOverdueDays();
}
