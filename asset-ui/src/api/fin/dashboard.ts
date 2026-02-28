import request from '@/api/request'

export interface DashboardSummaryVO {
  monthReceivable: number
  monthReceived: number
  currentOverdue: number
  monthWriteOffCount: number
  feeTypeDistribution: NameValueVO[]
  writeOffTypeDistribution: NameValueVO[]
}

export interface NameValueVO {
  name: string
  value: number
}

export interface ReceiptTrendVO {
  month: string
  amount: number
}

export interface OverdueTopVO {
  merchantId: number
  merchantName: string
  overdueAmount: number
}

/** 财务看板汇总（四卡片 + 两饼图数据） */
export function getDashboardSummary() {
  return request.get<DashboardSummaryVO>('/fin/dashboard/summary')
}

/** 近12个月收款趋势 */
export function getReceiptTrend() {
  return request.get<ReceiptTrendVO[]>('/fin/dashboard/receipt-trend')
}

/** 欠费TOP10商家 */
export function getOverdueTop() {
  return request.get<OverdueTopVO[]>('/fin/dashboard/overdue-top')
}
