import request from '@/utils/request'

/** 合同台账查询参数 */
export interface LedgerQueryDTO {
  projectId?: number
  contractType?: number
  status?: number
  auditStatus?: number
  ledgerCode?: string
  pageNum?: number
  pageSize?: number
}

/** 合同台账主表 */
export interface OprContractLedger {
  id: number
  ledgerCode: string
  contractId: number
  projectId: number
  merchantId?: number
  brandId?: number
  contractType?: number
  contractStart?: string
  contractEnd?: string
  doubleSignStatus: number
  doubleSignDate?: string
  receivableStatus: number
  auditStatus: number
  status: number
  pushTime?: string
}

/** 应收计划 */
export interface OprReceivablePlan {
  id: number
  ledgerId: number
  contractId: number
  shopId?: number
  feeItemId?: number
  feeName?: string
  billingStart?: string
  billingEnd?: string
  dueDate?: string
  amount: number
  receivedAmount: number
  status: number
  pushStatus: number
  sourceType: number
  version: number
}

/** 分页查询台账列表 */
export function getLedgerPage(params: LedgerQueryDTO) {
  return request.get('/api/opr/ledgers', { params })
}

/** 台账详情 */
export function getLedgerById(id: number) {
  return request.get(`/api/opr/ledgers/${id}`)
}

/** 双签确认 */
export function doubleSign(id: number) {
  return request.put(`/api/opr/ledgers/${id}/double-sign`)
}

/** 生成应收计划 */
export function generateReceivable(id: number) {
  return request.post(`/api/opr/ledgers/${id}/generate-receivable`)
}

/** 审核台账 */
export function auditLedger(id: number, data: { auditStatus: number; comment?: string }) {
  return request.put(`/api/opr/ledgers/${id}/audit`, data)
}

/** 手动推送应收 */
export function pushReceivable(id: number) {
  return request.post(`/api/opr/ledgers/${id}/push-receivable`)
}

/** 一次性首款录入 */
export function addOneTimePayment(id: number, data: unknown) {
  return request.post(`/api/opr/ledgers/${id}/one-time-payment`, data)
}

/** 查询应收计划列表 */
export function listReceivables(id: number) {
  return request.get(`/api/opr/ledgers/${id}/receivables`)
}
