import request from '@/api/request'

// ─── 类型定义 ──────────────────────────────────────────────────────────────

export interface ReceiptDetailItem {
  id?: number
  receiptId?: number
  feeItemId?: number
  feeName?: string
  amount: number
  remark?: string
}

export interface ReceiptDetailVO {
  id: number
  receiptCode: string
  contractId: number
  contractCode: string
  contractName: string
  projectId: number
  projectName: string
  merchantId: number
  merchantName: string
  brandId: number
  shopCode: string
  totalAmount: number
  paymentMethod: number
  paymentMethodName: string
  bankSerialNo: string
  payerName: string
  bankName: string
  bankAccount: string
  isUnnamed: number
  accountingEntity: string
  receiptDate: string
  receiver: string
  status: number
  statusName: string
  writeOffAmount: number
  prepayAmount: number
  details: ReceiptDetailItem[]
}

export interface ReceiptQueryDTO {
  contractId?: number
  merchantId?: number
  projectId?: number
  status?: number
  isUnnamed?: number
  paymentMethod?: number
  receiptDateFrom?: string
  receiptDateTo?: string
  receiptCode?: string
  pageNum?: number
  pageSize?: number
}

export interface ReceiptCreateDTO {
  contractId?: number
  brandId?: number
  shopCode?: string
  totalAmount: number
  paymentMethod?: number
  bankSerialNo?: string
  payerName?: string
  bankName?: string
  bankAccount?: string
  isUnnamed?: number
  accountingEntity?: string
  receiptDate: string
  receiver?: string
  details?: ReceiptDetailItem[]
}

// ─── API 函数 ──────────────────────────────────────────────────────────────

/** 收款单分页列表 */
export function getReceiptPage(params: ReceiptQueryDTO) {
  return request.get('/fin/receipts', { params })
}

/** 收款单详情（含拆分明细） */
export function getReceiptDetail(id: number) {
  return request.get(`/fin/receipts/${id}`)
}

/** 新增收款单 */
export function createReceipt(data: ReceiptCreateDTO) {
  return request.post('/fin/receipts', data)
}

/** 编辑收款单 */
export function updateReceipt(id: number, data: ReceiptCreateDTO) {
  return request.put(`/fin/receipts/${id}`, data)
}

/** 作废收款单 */
export function cancelReceipt(id: number, reason?: string) {
  return request.put(`/fin/receipts/${id}/cancel`, { reason })
}

/** 未名款项归名 */
export function bindReceipt(id: number, contractId: number) {
  return request.put(`/fin/receipts/${id}/bind`, { contractId })
}
