import request from '@/api/request'

// ─────────────────────────── 查询参数 ───────────────────────────

export interface OprQueryParam {
  projectId?: number | null
  formatType?: string
  startMonth?: string
  endMonth?: string
  statMonth?: string
  compareMode?: 'NONE' | 'YOY' | 'MOM'
  pageNum?: number
  pageSize?: number
}

// ─────────────────────────── VO 类型 ───────────────────────────

/** 营运趋势数据点 */
export interface OprTrendVO {
  timeDim: string
  revenueAmount: number | null
  floatingRentAmount: number | null
  avgRevenuePerSqm: number | null
  passengerFlow: number | null
  avgDailyPassenger: number | null
  changeCount: number | null
  changeRentImpact: number | null
  expiringContracts: number | null
  terminatedContracts: number | null
  prevRevenueAmount: number | null
  revenueGrowthRate: number | null
}

/** 地区业务对比 VO（雷达图） */
export interface OprRegionCompareVO {
  projectId: number | null
  statMonth: string
  revenueAmount: number | null
  passengerFlow: number | null
  changeCount: number | null
  changeRentImpact: number | null
  terminatedContracts: number | null
  avgRevenuePerSqm: number | null
  expiringContracts: number | null
  revenueScore: number | null
  passengerScore: number | null
  avgRevenueScore: number | null
}

/** 到期预警 VO */
export interface OprExpiringContractVO {
  projectId: number | null
  statMonth: string
  expiringWithin30: number | null
  expiringWithin60: number | null
  expiringWithin90: number | null
  totalExpiring: number | null
}

/** 营运数据看板聚合 VO */
export interface OprDashboardVO {
  latestMonth: string | null
  totalRevenue: number | null
  floatingRentAmount: number | null
  avgRevenuePerSqm: number | null
  passengerFlow: number | null
  changeCount: number | null
  terminatedContracts: number | null
  revenueYoY: number | null
  passengerFlowYoY: number | null
  avgRevenuePerSqmYoY: number | null
  expiringWithin30: number | null
  expiringWithin60: number | null
  expiringWithin90: number | null
  revenueTrend: OprTrendVO[]
  passengerTrend: OprTrendVO[]
  projectComparison: OprRegionCompareVO[]
}

/** 营收汇总 VO */
export interface OprRevenueSummaryVO {
  timeDim: string
  projectId: number | null
  formatType: string
  revenueAmount: number | null
  floatingRentAmount: number | null
  avgRevenuePerSqm: number | null
  prevRevenueAmount: number | null
  revenueGrowthRate: number | null
}

/** 合同变更统计 VO */
export interface OprContractChangeVO {
  timeDim: string
  projectId: number | null
  formatType: string
  changeCount: number | null
  changeRentImpact: number | null
  prevChangeCount: number | null
  changeCountGrowthRate: number | null
}

/** 租金变更分析 VO */
export interface OprRentChangeVO {
  timeDim: string
  projectId: number | null
  formatType: string
  changeRentImpact: number | null
  changeCount: number | null
  avgChangeImpact: number | null
  prevChangeRentImpact: number | null
  changeRentGrowthRate: number | null
}

// ─────────────────────────── API 方法 ───────────────────────────

/** 营运数据看板（聚合） */
export function getOprDashboard(params?: OprQueryParam) {
  return request.get<any, OprDashboardVO>('/rpt/opr/dashboard', { params })
}

/** 营收填报汇总（含同比/环比） */
export function getRevenueSummary(params?: OprQueryParam) {
  return request.get<any, OprRevenueSummaryVO[]>('/rpt/opr/revenue-summary', { params })
}

/** 合同变更统计 */
export function getContractChanges(params?: OprQueryParam) {
  return request.get<any, OprContractChangeVO[]>('/rpt/opr/contract-changes', { params })
}

/** 租金变更分析 */
export function getRentChanges(params?: OprQueryParam) {
  return request.get<any, OprRentChangeVO[]>('/rpt/opr/rent-changes', { params })
}

/** 合同到期预警 */
export function getExpiringContracts(params?: OprQueryParam) {
  return request.get<any, OprExpiringContractVO[]>('/rpt/opr/expiring-contracts', { params })
}

/** 地区业务对比 */
export function getRegionCompare(params?: OprQueryParam) {
  return request.get<any, OprRegionCompareVO[]>('/rpt/opr/region-compare', { params })
}

// ─────────────────────────── P1 VO 类型 ───────────────────────────

/** 客流数据分析 VO */
export interface OprPassengerFlowVO {
  timeDim: string
  projectId: number | null
  passengerFlow: number | null
  avgDailyPassenger: number | null
  prevPassengerFlow: number | null
  growthRate: number | null
}

/** 解约统计 VO */
export interface OprTerminationStatsVO {
  timeDim: string
  projectId: number | null
  formatType: string
  terminatedContracts: number | null
  prevTerminatedContracts: number | null
  growthRate: number | null
}

/** 浮动租金统计 VO */
export interface OprFloatingRentVO {
  timeDim: string
  projectId: number | null
  formatType: string
  floatingRentAmount: number | null
  prevFloatingRentAmount: number | null
  growthRate: number | null
}

/** 合同台账变动 VO */
export interface OprLedgerChangeVO {
  timeDim: string
  projectId: number | null
  formatType: string
  changeCount: number | null
  changeRentImpact: number | null
  terminatedContracts: number | null
  expiringContracts: number | null
  prevChangeCount: number | null
  changeCountGrowthRate: number | null
}

// ─────────────────────────── P1 API 方法 ───────────────────────────

/** 客流数据分析（P1，支持同比/环比） */
export function getPassengerFlow(params?: OprQueryParam) {
  return request.get<any, OprPassengerFlowVO[]>('/rpt/opr/passenger-flow', { params })
}

/** 解约统计（P1，支持同比/环比） */
export function getTerminationStats(params?: OprQueryParam) {
  return request.get<any, OprTerminationStatsVO[]>('/rpt/opr/termination-stats', { params })
}

/** 浮动租金统计（P1，支持同比/环比） */
export function getFloatingRent(params?: OprQueryParam) {
  return request.get<any, OprFloatingRentVO[]>('/rpt/opr/floating-rent', { params })
}

/** 合同台账变动（P1，支持同比/环比） */
export function getLedgerChanges(params?: OprQueryParam) {
  return request.get<any, OprLedgerChangeVO[]>('/rpt/opr/ledger-changes', { params })
}
