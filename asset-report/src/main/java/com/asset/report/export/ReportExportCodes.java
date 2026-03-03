package com.asset.report.export;

/**
 * 报表导出编码常量
 * <p>
 * 前端传入 reportCode 时使用此处定义的字符串常量，对应不同的报表类型。
 * 服务层根据 reportCode 分发到具体的数据提供者（ExportDefinition）。
 * </p>
 */
public final class ReportExportCodes {

    private ReportExportCodes() {}

    // ==================== 财务类报表 ====================

    /** 应收汇总报表 */
    public static final String FIN_RECEIVABLE_SUMMARY  = "FIN_RECEIVABLE_SUMMARY";
    /** 收款汇总报表 */
    public static final String FIN_RECEIPT_SUMMARY     = "FIN_RECEIPT_SUMMARY";
    /** 欠款统计报表 */
    public static final String FIN_OUTSTANDING_SUMMARY = "FIN_OUTSTANDING_SUMMARY";
    /** 账龄分析报表 */
    public static final String FIN_AGING_ANALYSIS      = "FIN_AGING_ANALYSIS";
    /** 逾期率统计 */
    public static final String FIN_OVERDUE_RATE        = "FIN_OVERDUE_RATE";
    /** 收缴率统计 */
    public static final String FIN_COLLECTION_RATE     = "FIN_COLLECTION_RATE";

    // ==================== 资产类报表 ====================

    /** 商铺租赁信息 */
    public static final String ASSET_SHOP_RENTAL       = "ASSET_SHOP_RENTAL";
    /** 空置率统计 */
    public static final String ASSET_VACANCY_RATE      = "ASSET_VACANCY_RATE";
    /** 品牌分布 */
    public static final String ASSET_BRAND_DIST        = "ASSET_BRAND_DIST";

    // ==================== 营运类报表 ====================

    /** 营收汇总分析 */
    public static final String OPR_REVENUE_SUMMARY     = "OPR_REVENUE_SUMMARY";
    /** 合同变更分析 */
    public static final String OPR_CONTRACT_CHANGES    = "OPR_CONTRACT_CHANGES";

    // ==================== 招商类报表 ====================

    /** 意向客户统计 */
    public static final String INV_INTENTION_STATS     = "INV_INTENTION_STATS";
    /** 招商业绩对比 */
    public static final String INV_PERFORMANCE         = "INV_PERFORMANCE";
}
