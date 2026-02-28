import request from '@/api/request'

// ─── TS 类型 ─────────────────────────────────────────────────────────────────

export interface VoucherEntry {
  id: number
  voucherId: number
  sourceType: number | null   // 1收款单 2核销单 3应收单
  sourceId: number | null
  accountCode: string
  accountName: string
  debitAmount: number
  creditAmount: number
  summary: string
}

export interface VoucherDetailVO {
  id: number
  voucherCode: string
  projectId: number
  projectName: string
  accountSet: string
  payType: number           // 1收款 2付款
  payTypeName: string
  voucherDate: string
  totalDebit: number
  totalCredit: number
  status: number            // 0待审核 1已审核 2已上传
  statusName: string
  uploadTime: string | null
  remark: string
  createTime: string
  updateTime: string
  entries?: VoucherEntry[]
}

export interface VoucherEntryDTO {
  sourceType?: number
  sourceId?: number
  accountCode: string
  accountName: string
  debitAmount: number
  creditAmount: number
  summary?: string
}

export interface VoucherCreateDTO {
  projectId: number
  accountSet?: string
  payType: number
  voucherDate: string
  remark?: string
  entries: VoucherEntryDTO[]
}

export interface VoucherQueryDTO {
  voucherCode?: string
  projectId?: number
  accountSet?: string
  payType?: number
  status?: number
  dateFrom?: string
  dateTo?: string
  pageNum?: number
  pageSize?: number
}

// ─── API 函数 ─────────────────────────────────────────────────────────────────

/** 分页查询凭证列表 */
export function getVoucherPage(query: VoucherQueryDTO) {
  return request.get('/fin/vouchers', { params: query })
}

/** 查询凭证详情（含分录） */
export function getVoucherDetail(id: number) {
  return request.get<VoucherDetailVO>(`/fin/vouchers/${id}`)
}

/** 手动创建凭证 */
export function createVoucher(dto: VoucherCreateDTO) {
  return request.post<number>('/fin/vouchers', dto)
}

/** 从收款单自动生成凭证 */
export function generateFromReceipt(receiptId: number) {
  return request.post<number>(`/fin/vouchers/generate-from-receipt/${receiptId}`)
}

/** 审核凭证 */
export function auditVoucher(id: number) {
  return request.post(`/fin/vouchers/${id}/audit`)
}

/** 上传凭证到财务系统 */
export function uploadVoucher(id: number) {
  return request.post(`/fin/vouchers/${id}/upload`)
}

/** 删除凭证 */
export function deleteVoucher(id: number) {
  return request.delete(`/fin/vouchers/${id}`)
}
