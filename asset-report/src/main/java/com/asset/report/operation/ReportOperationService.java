package com.asset.report.operation;

import com.asset.report.common.param.ReportQueryParam;
import com.asset.report.vo.opr.*;

import java.util.List;

/**
 * 营运类报表 Service 接口
 * <p>
 * 所有方法依赖 {@link com.asset.report.common.permission.ReportPermissionContext}
 * 中已注入的数据权限列表，业务方法内部通过 {@code ReportPermissionContext.get()} 获取。
 * </p>
 */
public interface ReportOperationService {

    // ==================== P0 接口 ====================

    /**
     * 营运数据看板（聚合接口）
     * <p>返回：核心指标 + 同比 + 到期预警 + 近12月趋势 + 项目对比</p>
     */
    OprDashboardVO dashboard(ReportQueryParam param);

    /**
     * 营收填报汇总（支持时间聚合 + 同比/环比）
     */
    List<OprRevenueSummaryVO> revenueSummary(ReportQueryParam param);

    /**
     * 合同变更统计（支持同比/环比）
     */
    List<OprContractChangeVO> contractChanges(ReportQueryParam param);

    /**
     * 租金变更分析（支持同比/环比）
     */
    List<OprRentChangeVO> rentChanges(ReportQueryParam param);

    /**
     * 合同到期预警（30/60/90 天分档）
     */
    List<OprExpiringContractVO> expiringContracts(ReportQueryParam param);

    /**
     * 地区业务对比（各项目多维指标，雷达图格式）
     */
    List<OprRegionCompareVO> regionCompare(ReportQueryParam param);

    // ==================== P1 接口 ====================

    /**
     * 客流数据分析（P1，支持同比/环比）
     */
    List<OprPassengerFlowVO> passengerFlow(ReportQueryParam param);

    /**
     * 解约统计（P1，支持同比/环比）
     */
    List<OprTerminationStatsVO> terminationStats(ReportQueryParam param);

    /**
     * 浮动租金统计（P1，支持同比/环比）
     */
    List<OprFloatingRentVO> floatingRent(ReportQueryParam param);
}
