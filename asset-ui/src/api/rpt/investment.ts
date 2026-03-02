import request from '@/api/request'

// ─────────────────────────── 查询参数 ───────────────────────────

export interface InvQueryParam {
  projectId?: number | null
  formatType?: string
  investmentManagerId?: number | null
  startDate?: string
  endDate?: string
  statDate?: string
  timeUnit?: 'DAY' | 'WEEK' | 'MONTH' | 'YEAR'
  compareMode?: 'NONE' | 'YOY' | 'MOM'
  pageNum?: number
  pageSize?: number
}

// ─────────────────────────── VO 类型 ───────────────────────────

/** 漏斗阶段数据 */
export interface FunnelVO {
  stage: string
  stageName: string
  count: number | null
  amount: number | null
  area: number | null
  conversionRate: number | null
  overallConversionRate: number | null
}

/** 招商趋势数据点 */
export interface InvTrendVO {
  timeDim: string
  intentionCount: number | null
  intentionSigned: number | null
  newIntention: number | null
  contractCount: number | null
  contractAmount: number | null
  contractArea: number | null
  newContract: number | null
  conversionRate: number | null
  avgRentPrice: number | null
  prevValue: number | null
  growthRate: number | null
}

/** 招商业绩 VO */
export interface PerformanceVO {
  statDate: string
  projectId: number | null
  investmentManagerId: number | null
  formatType: string
  intentionCount: number | null
  intentionSigned: number | null
  newIntention: number | null
  contractCount: number | null
  contractAmount: number | null
  contractArea: number | null
  newContract: number | null
  conversionRate: number | null
  avgRentPrice: number | null
}

/** 招商数据看板聚合 VO */
export interface InvDashboardVO {
  latestDate: string | null
  intentionCount: number | null
  intentionSigned: number | null
  newIntentionToday: number | null
  contractCount: number | null
  contractAmount: number | null
  contractArea: number | null
  newContractToday: number | null
  conversionRate: number | null
  avgRentPrice: number | null
  contractCountYoY: number | null
  contractAmountYoY: number | null
  avgRentPriceYoY: number | null
  conversionRateYoY: number | null
  funnel: FunnelVO[]
  intentionTrend: InvTrendVO[]
  contractTrend: InvTrendVO[]
  projectComparison: PerformanceVO[]
}

/** 意向客户统计 VO */
export interface IntentionStatsVO {
  timeDim: string
  intentionCount: number | null
  intentionSigned: number | null
  newIntention: number | null
  signedRate: number | null
  prevIntentionCount: number | null
  growthRate: number | null
}

/** 合同租赁情况统计 VO */
export interface ContractStatsVO {
  timeDim: string
  contractCount: number | null
  contractAmount: number | null
  contractArea: number | null
  newContract: number | null
  conversionRate: number | null
  avgRentPrice: number | null
  prevContractCount: number | null
  growthRate: number | null
}

/** 租金水平分析 VO */
export interface RentLevelVO {
  projectId: number | null
  formatType: string
  investmentManagerId: number | null
  avgRentPrice: number | null
  contractArea: number | null
  contractAmount: number | null
  contractCount: number | null
  prevAvgRentPrice: number | null
  avgRentPriceYoY: number | null
}

// ─────────────────────────── API 方法 ───────────────────────────

/** 招商数据看板（聚合接口） */
export function getInvDashboard(params?: InvQueryParam) {
  return request.get<any, InvDashboardVO>('/rpt/inv/dashboard', { params })
}

/** 意向客户统计（趋势） */
export function getIntentionStats(params?: InvQueryParam) {
  return request.get<any, IntentionStatsVO[]>('/rpt/inv/intention-stats', { params })
}

/** 客户跟进漏斗数据 */
export function getFunnel(params?: InvQueryParam) {
  return request.get<any, FunnelVO[]>('/rpt/inv/funnel', { params })
}

/** 合同租赁情况（趋势） */
export function getContractStats(params?: InvQueryParam) {
  return request.get<any, ContractStatsVO[]>('/rpt/inv/contract-stats', { params })
}

/** 招商业绩显差看板 */
export function getPerformance(params?: InvQueryParam) {
  return request.get<any, PerformanceVO[]>('/rpt/inv/performance', { params })
}

/** 租金水平分析（P1） */
export function getRentLevel(params?: InvQueryParam) {
  return request.get<any, RentLevelVO[]>('/rpt/inv/rent-level', { params })
}
