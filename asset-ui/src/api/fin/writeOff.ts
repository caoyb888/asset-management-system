import request from '@/api/request'

// ─── 类型定义 ────────────────────────────────────────────────────────────────

export interface WriteOffDetailItem {
  id?: number
  writeOffId?: number
  receivableId: number
  feeItemId?: number
  accrualMonth?: string
  writeOffAmount: number
  overpayAmount?: number
}

export interface WriteOffDetailVO {
  id: number
  writeOffCode: string
  receiptId: number
  receiptCode?: string
  contractId: number
  contractCode?: string
  contractName?: string
  merchantId?: number
  merchantName?: string
  projectId?: number
  projectName?: string
  writeOffType: number
  writeOffTypeName?: string
  totalAmount: number
  status: number
  statusName?: string
  approvalId?: string
  createTime?: string
  updateTime?: string
  details?: WriteOffDetailItem[]
}

export interface WritableReceivableVO {
  id: number
  receivableCode?: string
  feeItemId?: number
  feeName?: string
  accrualMonth?: string
  billingStart?: string
  billingEnd?: string
  dueDate?: string
  actualAmount: number
  receivedAmount: number
  outstandingAmount: number
  status: number
}

export interface WriteOffQueryDTO {
  writeOffCode?: string
  receiptId?: number
  contractId?: number
  merchantId?: number
  projectId?: number
  writeOffType?: number
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface SubmitWriteOffRequest {
  receiptId: number
  writeOffType?: number
  items: Array<{
    receivableId: number
    feeItemId?: number
    accrualMonth?: string
    writeOffAmount: number
  }>
}

// ─── API 函数 ────────────────────────────────────────────────────────────────

/** 分页查询核销单列表 */
export function getWriteOffPage(params: WriteOffQueryDTO) {
  return request.get<any>('/fin/write-offs', { params })
}

/** 查询可核销应收记录 */
export function getWritableReceivables(contractId: number) {
  return request.get<WritableReceivableVO[]>('/fin/write-offs/writable-receivables', {
    params: { contractId },
  })
}

/** 提交核销申请 */
export function submitWriteOff(data: SubmitWriteOffRequest) {
  return request.post<number>('/fin/write-offs', data)
}

/** OA审批回调（通常由后台调用，前端提供手动触发入口） */
export function approveCallback(approvalId: string, approved: boolean, comment?: string) {
  return request.post<void>('/fin/write-offs/approval-callback', { approvalId, approved, comment })
}

/** 撤销核销单 */
export function cancelWriteOff(id: number) {
  return request.put<void>(`/fin/write-offs/${id}/cancel`)
}

/** 查看核销单详情 */
export function getWriteOffDetail(id: number) {
  return request.get<WriteOffDetailVO>(`/fin/write-offs/${id}`)
}
