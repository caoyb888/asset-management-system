import request from '@/api/request'

/** 应收查询参数 */
export interface ReceivableQueryDTO {
  contractId?: number
  projectId?: number
  merchantId?: number
  feeItemId?: number
  status?: number
  accrualMonth?: string
  dueDateFrom?: string
  dueDateTo?: string
  overdue?: boolean
  receivableCode?: string
  pageNum?: number
  pageSize?: number
}

/** 应收明细 VO */
export interface ReceivableDetailVO {
  id: number
  receivableCode: string
  contractId: number
  contractCode: string
  contractName: string
  projectId: number
  projectName: string
  merchantId: number
  merchantName: string
  feeItemId: number
  feeName: string
  billingStart: string
  billingEnd: string
  dueDate: string
  accrualMonth: string
  originalAmount: number
  actualAmount: number
  receivedAmount: number
  reductionAmount: number
  outstandingAmount: number
  status: number
  statusName: string
  overdueDays: number
  isOverdue: boolean
}

/** 应收汇总 VO */
export interface ReceivableSummaryVO {
  contractId: number
  contractCode: string
  contractName: string
  merchantId: number
  merchantName: string
  projectId: number
  projectName: string
  totalOriginal: number
  totalActual: number
  totalReceived: number
  totalReduction: number
  totalOutstanding: number
  overdueCount: number
  overdueAmount: number
}

/** 欠费统计 VO */
export interface OverdueStatisticsVO {
  overdue30Amount: number
  overdue30To90Amount: number
  overdueOver90Amount: number
  totalOverdueAmount: number
  totalOverdueCount: number
  topDebtors: ReceivableSummaryVO[]
}

/** 减免调整参数 */
export interface ReductionDTO {
  receivableId: number
  reductionAmount: number
  reason: string
}

/** 分页查询应收明细 */
export function getReceivablePage(params: ReceivableQueryDTO) {
  return request.get('/api/fin/receivables', { params })
}

/** 按合同汇总应收 */
export function getReceivableSummary(params: { contractId?: number; projectId?: number; merchantId?: number }) {
  return request.get('/api/fin/receivables/summary', { params })
}

/** 欠费统计 */
export function getOverdueStatistics(params?: { projectId?: number }) {
  return request.get('/api/fin/receivables/overdue-statistics', { params })
}

/** 导出应收明细 Excel */
export function exportReceivable(params: ReceivableQueryDTO) {
  return request.get('/api/fin/receivables/export', { params, responseType: 'blob' })
}

/** 从营运应收计划同步（按台账planId） */
export function syncFromPlan(planId: number) {
  return request.post(`/api/fin/receivables/sync-from-plan/${planId}`)
}

/** 手动刷新逾期天数 */
export function refreshOverdueDays() {
  return request.post('/api/fin/receivables/refresh-overdue')
}

/** 减免调整 */
export function applyReduction(data: ReductionDTO) {
  return request.post('/api/fin/receivables/reduction', data)
}
