import request from '@/api/request'

// ─────────────────────────── 查询参数 ───────────────────────────

export interface FinQueryParam {
  projectId?: number | null
  feeItemType?: string
  merchantId?: number | null
  statDate?: string
  startMonth?: string
  endMonth?: string
  statMonth?: string
  compareMode?: 'NONE' | 'YOY' | 'MOM'
  pageNum?: number
  pageSize?: number
}

// ─────────────────────────── VO 类型 ───────────────────────────

/** 财务趋势数据点（多接口复用） */
export interface FinTrendVO {
  timeDim: string
  projectId: number | null
  receivableAmount: number | null
  receivedAmount: number | null
  outstandingAmount: number | null
  overdueAmount: number | null
  deductionAmount: number | null
  adjustmentAmount: number | null
  collectionRate: number | null
  overdueRate: number | null
  depositBalance: number | null
  prepayBalance: number | null
  receivableYoY: number | null
  receivedYoY: number | null
  collectionRateYoY: number | null
  overdueRateYoY: number | null
}

/** 账龄分析 VO */
export interface FinAgingAnalysisVO {
  statDate: string | null
  projectId: number | null
  merchantId: number | null
  contractId: number | null
  within30: number | null
  days3160: number | null
  days6190: number | null
  days91180: number | null
  days181365: number | null
  over365: number | null
  totalOutstanding: number | null
  within30Rate: number | null
  days3160Rate: number | null
  days6190Rate: number | null
  days91180Rate: number | null
  days181365Rate: number | null
  over365Rate: number | null
}

/** 财务数据看板聚合 VO */
export interface FinDashboardVO {
  latestMonth: string | null
  totalReceivable: number | null
  totalReceived: number | null
  totalOutstanding: number | null
  totalOverdue: number | null
  avgCollectionRate: number | null
  avgOverdueRate: number | null
  totalDepositBalance: number | null
  totalPrepayBalance: number | null
  receivableYoY: number | null
  receivedYoY: number | null
  collectionRateYoY: number | null
  overdueRateYoY: number | null
  financeTrend: FinTrendVO[]
  agingSummary: FinAgingAnalysisVO | null
  overdueTop10: FinAgingAnalysisVO[]
}

/** 应收汇总 VO */
export interface FinReceivableSummaryVO {
  timeDim: string
  projectId: number | null
  feeItemType: string | null
  receivableAmount: number | null
  receivedAmount: number | null
  outstandingAmount: number | null
  deductionAmount: number | null
  adjustmentAmount: number | null
  collectionRate: number | null
  receivableYoY: number | null
  receivedYoY: number | null
  collectionRateYoY: number | null
  receivableMoM: number | null
  collectionRateMoM: number | null
}

/** 收款汇总 VO */
export interface FinReceiptSummaryVO {
  timeDim: string
  projectId: number | null
  feeItemType: string | null
  receivableAmount: number | null
  receivedAmount: number | null
  outstandingAmount: number | null
  collectionRate: number | null
  receivedYoY: number | null
  collectionRateYoY: number | null
  receivedMoM: number | null
  collectionRateMoM: number | null
}

/** 欠款统计 VO */
export interface FinOutstandingSummaryVO {
  timeDim: string
  projectId: number | null
  feeItemType: string | null
  receivableAmount: number | null
  receivedAmount: number | null
  outstandingAmount: number | null
  overdueAmount: number | null
  overdueRate: number | null
  deductionAmount: number | null
  adjustmentAmount: number | null
  outstandingYoY: number | null
  overdueRateYoY: number | null
}

/** 逾期率统计 VO */
export interface FinOverdueRateVO {
  timeDim: string
  projectId: number | null
  receivableAmount: number | null
  overdueAmount: number | null
  overdueRate: number | null
  overdueRateYoY: number | null
  overdueRateMoM: number | null
  overdueAmountYoY: number | null
}

/** 收缴率统计 VO */
export interface FinCollectionRateVO {
  timeDim: string
  projectId: number | null
  feeItemType: string | null
  receivableAmount: number | null
  receivedAmount: number | null
  outstandingAmount: number | null
  collectionRate: number | null
  collectionRateYoY: number | null
  collectionRateMoM: number | null
}

// ─────────────────────────── P1 VO 类型 ───────────────────────────

/** 凭证统计 VO */
export interface FinVoucherStatsVO {
  statMonth: string
  projectId: number | null
  totalVouchers: number | null
  pendingVouchers: number | null
  approvedVouchers: number | null
  uploadedVouchers: number | null
  totalDebit: number | null
  totalCredit: number | null
}

/** 保证金汇总 VO */
export interface FinDepositSummaryVO {
  timeDim: string
  projectId: number | null
  depositBalance: number | null
  depositBalanceYoY: number | null
  depositBalanceMoM: number | null
}

/** 预收款汇总 VO */
export interface FinPrepaySummaryVO {
  timeDim: string
  projectId: number | null
  prepayBalance: number | null
  prepayBalanceYoY: number | null
  prepayBalanceMoM: number | null
}

/** 减免/调整统计 VO */
export interface FinDeductionAdjustmentVO {
  timeDim: string
  projectId: number | null
  feeItemType: string | null
  receivableAmount: number | null
  deductionAmount: number | null
  adjustmentAmount: number | null
  deductionRate: number | null
  adjustmentRate: number | null
  deductionYoY: number | null
  adjustmentYoY: number | null
}

// ─────────────────────────── API 方法 ───────────────────────────

/** 财务数据看板（聚合） */
export function getFinDashboard(params?: FinQueryParam) {
  return request.get<any, FinDashboardVO>('/rpt/fin/dashboard', { params })
}

/** 应收汇总报表（含同比/环比） */
export function getReceivableSummary(params?: FinQueryParam) {
  return request.get<any, FinReceivableSummaryVO[]>('/rpt/fin/receivable-summary', { params })
}

/** 收款汇总报表（含同比/环比） */
export function getReceiptSummary(params?: FinQueryParam) {
  return request.get<any, FinReceiptSummaryVO[]>('/rpt/fin/receipt-summary', { params })
}

/** 欠款统计报表（含同比/环比） */
export function getOutstandingSummary(params?: FinQueryParam) {
  return request.get<any, FinOutstandingSummaryVO[]>('/rpt/fin/outstanding-summary', { params })
}

/** 账龄分析报表 */
export function getAgingAnalysis(params?: FinQueryParam) {
  return request.get<any, FinAgingAnalysisVO[]>('/rpt/fin/aging-analysis', { params })
}

/** 逾期率统计（含同比/环比） */
export function getOverdueRate(params?: FinQueryParam) {
  return request.get<any, FinOverdueRateVO[]>('/rpt/fin/overdue-rate', { params })
}

/** 收缴率统计（含同比/环比） */
export function getCollectionRate(params?: FinQueryParam) {
  return request.get<any, FinCollectionRateVO[]>('/rpt/fin/collection-rate', { params })
}

/** 凭证统计（P1） */
export function getVoucherStats(params?: FinQueryParam) {
  return request.get<any, FinVoucherStatsVO[]>('/rpt/fin/voucher-stats', { params })
}

/** 保证金汇总（P1） */
export function getDepositSummary(params?: FinQueryParam) {
  return request.get<any, FinDepositSummaryVO[]>('/rpt/fin/deposit-summary', { params })
}

/** 预收款汇总（P1） */
export function getPrepaySummary(params?: FinQueryParam) {
  return request.get<any, FinPrepaySummaryVO[]>('/rpt/fin/prepay-summary', { params })
}

/** 减免/调整统计（P1） */
export function getDeductionAdjustment(params?: FinQueryParam) {
  return request.get<any, FinDeductionAdjustmentVO[]>('/rpt/fin/deduction-adjustment', { params })
}
