import request from '@/api/request'

/** 解约单查询参数 */
export interface TerminationQueryDTO {
  contractId?: number
  projectId?: number
  terminationType?: number
  status?: number
  terminationCode?: string
  pageNum?: number
  pageSize?: number
}

/** 新增/编辑解约单参数 */
export interface TerminationCreateDTO {
  contractId: number
  ledgerId?: number
  terminationType: number
  terminationDate: string
  reason?: string
  newContractId?: number
  penaltyRate?: number
}

/** 清算明细 */
export interface TerminationSettlementItem {
  id: number
  terminationId: number
  itemType: number      // 1未收租金/2违约金/3保证金退还/4其他
  itemName: string
  amount: number
  remark?: string
}

/** 解约单详情 VO */
export interface TerminationDetailVO {
  id: number
  terminationCode: string
  contractId: number
  ledgerId?: number
  projectId?: number
  merchantId?: number
  terminationType: number
  terminationTypeName?: string
  terminationDate: string
  reason?: string
  newContractId?: number
  penaltyAmount?: number
  refundDeposit?: number
  unsettledAmount?: number
  settlementAmount?: number
  status: number
  statusName?: string
  approvalId?: string
  contractCode?: string
  contractName?: string
  merchantName?: string
  projectName?: string
  shopCode?: string
  settlements?: TerminationSettlementItem[]
}

/** 审批回调参数 */
export interface ApprovalCallbackDTO {
  approvalId?: string
  status: number     // 2通过/3驳回
  comment?: string
}

/** 分页查询解约列表 */
export function getTerminationPage(params: TerminationQueryDTO) {
  return request.get('/api/opr/terminations', { params })
}

/** 解约单详情（含清算明细） */
export function getTerminationById(id: number) {
  return request.get(`/api/opr/terminations/${id}`)
}

/** 新增解约单 */
export function createTermination(data: TerminationCreateDTO) {
  return request.post('/api/opr/terminations', data)
}

/** 编辑解约单 */
export function updateTermination(id: number, data: TerminationCreateDTO) {
  return request.put(`/api/opr/terminations/${id}`, data)
}

/** 触发清算计算 */
export function calculateSettlement(id: number) {
  return request.post(`/api/opr/terminations/${id}/calculate-settlement`)
}

/** 提交审批 */
export function submitTerminationApproval(id: number) {
  return request.post(`/api/opr/terminations/${id}/submit-approval`)
}

/** 审批回调 */
export function terminationApprovalCallback(id: number, data: ApprovalCallbackDTO) {
  return request.post(`/api/opr/terminations/${id}/approval-callback`, data)
}
