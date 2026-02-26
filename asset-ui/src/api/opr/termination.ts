import request from '@/utils/request'

/** 合同解约查询参数 */
export interface TerminationQueryDTO {
  contractId?: number
  terminationType?: number
  status?: number
  pageNum?: number
  pageSize?: number
}

/** 合同解约主表 */
export interface OprContractTermination {
  id: number
  terminationCode: string
  contractId: number
  ledgerId?: number
  terminationType: number
  terminationDate: string
  reason?: string
  penaltyAmount: number
  refundDeposit: number
  unsettledAmount: number
  settlementAmount: number
  status: number
}

/** 分页查询解约列表 */
export function getTerminationPage(params: TerminationQueryDTO) {
  return request.get('/api/opr/terminations', { params })
}

/** 解约详情（含清算明细/审批流程） */
export function getTerminationById(id: number) {
  return request.get(`/api/opr/terminations/${id}`)
}

/** 新增解约单 */
export function createTermination(data: unknown) {
  return request.post('/api/opr/terminations', data)
}

/** 编辑解约单 */
export function updateTermination(id: number, data: unknown) {
  return request.put(`/api/opr/terminations/${id}`, data)
}

/** 计算清算金额 */
export function calculateSettlement(id: number) {
  return request.post(`/api/opr/terminations/${id}/calculate-settlement`)
}

/** 提交审批 */
export function submitTerminationApproval(id: number) {
  return request.post(`/api/opr/terminations/${id}/submit-approval`)
}

/** 执行解约（事务性多表联动） */
export function executeTermination(id: number) {
  return request.post(`/api/opr/terminations/${id}/execute`)
}
