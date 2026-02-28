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
  adjustAmount: number
  deductionAmount: number
  actualAmount: number
  receivedAmount: number
  outstandingAmount: number
  status: number
  statusName: string
  overdueDays: number
  isOverdue: boolean
  isPrinted: number
  isInvoiced: number
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
  totalDeduction: number
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

/** 减免申请参数 */
export interface DeductionDTO {
  receivableId: number
  deductionAmount: number
  reason: string
}

/** 调整申请参数 */
export interface AdjustmentDTO {
  receivableId: number
  adjustType: 1 | 2    // 1增加/2减少
  adjustAmount: number
  reason: string
}

/** 分页查询应收明细 */
export function getReceivablePage(params: ReceivableQueryDTO) {
  return request.get('/fin/receivables', { params })
}

/** 按合同汇总应收 */
export function getReceivableSummary(params: { contractId?: number; projectId?: number; merchantId?: number }) {
  return request.get('/fin/receivables/summary', { params })
}

/** 欠费统计 */
export function getOverdueStatistics(params?: { projectId?: number }) {
  return request.get('/fin/receivables/overdue-statistics', { params })
}

/** 导出应收明细 Excel */
export function exportReceivable(params: ReceivableQueryDTO) {
  return request.get('/fin/receivables/export', { params, responseType: 'blob' })
}

/** 从营运应收计划同步 */
export function syncFromPlan(planId: number) {
  return request.post(`/fin/receivables/sync-from-plan/${planId}`)
}

/** 手动刷新欠费金额 */
export function refreshOverdueDays() {
  return request.post('/fin/receivables/refresh-overdue')
}

/** 申请减免 */
export function applyDeduction(data: DeductionDTO) {
  return request.post('/fin/receivables/deduction', data)
}

/** 申请调整 */
export function applyAdjustment(data: AdjustmentDTO) {
  return request.post('/fin/receivables/adjustment', data)
}

/** 标记账单为已打印 */
export function markPrinted(ids: number[]) {
  return request.post('/fin/receivables/mark-printed', ids)
}
