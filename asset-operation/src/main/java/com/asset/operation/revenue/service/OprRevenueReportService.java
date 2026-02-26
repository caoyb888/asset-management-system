package com.asset.operation.revenue.service;

import com.asset.operation.revenue.dto.*;
import com.asset.operation.revenue.entity.OprRevenueReport;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** 营收填报 Service 接口 */
public interface OprRevenueReportService extends IService<OprRevenueReport> {

    /** 分页查询营收填报列表 */
    IPage<OprRevenueReport> pageQuery(RevenueReportQueryDTO query);

    /**
     * 新增营收填报（应用层唯一性校验：同一合同同一日期 is_deleted=0 不允许重复）
     * @return 保存后的实体
     */
    OprRevenueReport saveReport(RevenueReportCreateDTO dto);

    /** 修改营收填报（仅待确认状态可改） */
    void updateReport(Long id, RevenueReportCreateDTO dto);

    /**
     * Excel 批量导入营收填报
     * @return Map，keys: successCount / errorList(List<String>)
     */
    Map<String, Object> importExcel(MultipartFile file);

    /** 导出营收报表（Excel） */
    void exportExcel(RevenueReportQueryDTO query, HttpServletResponse response);

    /** 下载导入模板 */
    void downloadTemplate(HttpServletResponse response);

    /**
     * 触发浮动租金计算（调用 FloatingRentCalculator 引擎）
     * @return 生成的浮动租金记录ID
     */
    Long generateFloatingRent(GenerateFloatingRentDTO dto);

    /**
     * 月度营收汇总统计
     * @param reportMonth 月份（YYYY-MM）
     * @param projectId   项目ID（可选）
     * @param contractId  合同ID（可选）
     */
    RevenueStatisticsVO getMonthlyStatistics(String reportMonth, Long projectId, Long contractId);

    /**
     * 查询指定合同指定月份的每日填报明细（用于日历视图着色）
     * @return key=日期(YYYY-MM-DD), value=营业额
     */
    Map<String, Object> getDailyDetail(Long contractId, String reportMonth);
}
