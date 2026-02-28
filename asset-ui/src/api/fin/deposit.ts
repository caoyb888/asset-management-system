import request from '@/api/request'

// ─── TS 类型 ─────────────────────────────────────────────────────────────────

export interface DepositAccountVO {
  id: number
  contractId: number
  contractCode: string
  contractName: string
  merchantId: number
  merchantName: string
  projectId: number
  projectName: string
  feeItemId: number
  feeItemName: string
  balance: number
  totalIn: number
  totalOffset: number
  totalRefund: number
  totalForfeit: number
  createTime: string
  updateTime: string
}

export interface DepositTransaction {
  id: number
  accountId: number
  transType: number     // 1收入 2冲抵 3退款 4罚没
  amount: number
  balanceAfter: number
  transDate: string
  sourceCode: string
  reason: string
  status: number        // 0待审核 1已审核 2已驳回
  approvalId: string
  createTime: string
}

export interface DepositQueryDTO {
  contractId?: number
  accountId?: number
  transType?: number
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface DepositPayInDTO {
  contractId: number
  amount: number
  sourceCode?: string
  reason?: string
}

export interface DepositOffsetDTO {
  contractId: number
  receivableId: number
  amount: number
  reason?: string
}

export interface DepositRefundDTO {
  contractId: number
  amount: number
  reason?: string
  bankName?: string
  bankAccount?: string
  payee?: string
}

export interface DepositForfeitDTO {
  contractId: number
  amount: number
  reason: string
}

// ─── API 函数 ─────────────────────────────────────────────────────────────────

/** 查询合同保证金账户余额卡片 */
export function getDepositAccount(contractId: number) {
  return request.get<DepositAccountVO>('/fin/deposits/account', { params: { contractId } })
}

/** 分页查询保证金流水 */
export function getDepositTransactions(query: DepositQueryDTO) {
  return request.get('/fin/deposits/transactions', { params: query })
}

/** 缴纳保证金（直接生效） */
export function payInDeposit(dto: DepositPayInDTO) {
  return request.post('/fin/deposits/pay-in', dto)
}

/** 申请冲抵应收 */
export function applyOffset(dto: DepositOffsetDTO) {
  return request.post<number>('/fin/deposits/offset', dto)
}

/** 申请退款 */
export function applyRefund(dto: DepositRefundDTO) {
  return request.post<number>('/fin/deposits/refund', dto)
}

/** 申请罚没 */
export function applyForfeit(dto: DepositForfeitDTO) {
  return request.post<number>('/fin/deposits/forfeit', dto)
}

/** OA审批回调（开发/测试手动触发） */
export function depositApprovalCallback(approvalId: string, approved: boolean) {
  return request.post('/fin/deposits/approval-callback', { approvalId, approved })
}
