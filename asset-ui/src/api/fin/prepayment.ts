import request from '@/api/request'

// ─── TS 类型 ─────────────────────────────────────────────────────────────────

export interface PrepayAccountVO {
  id: number
  contractId: number
  contractCode: string
  contractName: string
  merchantId: number
  merchantName: string
  projectId: number
  projectName: string
  feeItemId: number | null
  balance: number
  createTime: string
  updateTime: string
}

export interface PrepayTransaction {
  id: number
  accountId: number
  transType: number    // 1转入 2抵冲 3退款
  amount: number
  balanceAfter: number
  transDate: string
  sourceCode: string
  remark: string
  createTime: string
}

export interface PrepayQueryDTO {
  contractId?: number
  accountId?: number
  transType?: number
  pageNum?: number
  pageSize?: number
}

export interface PrepayDepositDTO {
  contractId: number
  amount: number
  sourceCode?: string
  remark?: string
}

export interface PrepayOffsetDTO {
  contractId: number
  receivableId: number
  amount: number
  remark?: string
}

export interface PrepayRefundDTO {
  contractId: number
  amount: number
  bankName?: string
  bankAccount?: string
  payee?: string
  remark?: string
}

// ─── API 函数 ─────────────────────────────────────────────────────────────────

/** 查询合同预收款账户 */
export function getPrepayAccount(contractId: number) {
  return request.get<PrepayAccountVO>('/fin/prepayments/account', { params: { contractId } })
}

/** 分页查询预收款流水 */
export function getPrepayTransactions(query: PrepayQueryDTO) {
  return request.get('/fin/prepayments/transactions', { params: query })
}

/** 手动录入预收款 */
export function depositPrepay(dto: PrepayDepositDTO) {
  return request.post('/fin/prepayments/deposit', dto)
}

/** 预收款抵冲应收 */
export function offsetPrepay(dto: PrepayOffsetDTO) {
  return request.post('/fin/prepayments/offset', dto)
}

/** 预收款退款 */
export function refundPrepay(dto: PrepayRefundDTO) {
  return request.post('/fin/prepayments/refund', dto)
}
