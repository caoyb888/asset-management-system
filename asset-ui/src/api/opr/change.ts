import request from '@/utils/request'

/** 合同变更查询参数 */
export interface ChangeQueryDTO {
  contractId?: number
  ledgerId?: number
  status?: number
  pageNum?: number
  pageSize?: number
}

/** 合同变更主表 */
export interface OprContractChange {
  id: number
  changeCode: string
  contractId: number
  ledgerId?: number
  projectId?: number
  status: number
  effectiveDate?: string
  reason?: string
  approvalId?: string
  changeTypeCodes?: string[]
}

/** 分页查询变更列表 */
export function getChangePage(params: ChangeQueryDTO) {
  return request.get('/api/opr/contract-changes', { params })
}

/** 变更详情（含明细/快照） */
export function getChangeById(id: number) {
  return request.get(`/api/opr/contract-changes/${id}`)
}

/** 新增变更单 */
export function createChange(data: unknown) {
  return request.post('/api/opr/contract-changes', data)
}

/** 编辑变更单 */
export function updateChange(id: number, data: unknown) {
  return request.put(`/api/opr/contract-changes/${id}`, data)
}

/** 预览变更影响 */
export function previewImpact(id: number) {
  return request.post(`/api/opr/contract-changes/${id}/preview-impact`)
}

/** 提交审批 */
export function submitChangeApproval(id: number) {
  return request.post(`/api/opr/contract-changes/${id}/submit-approval`)
}

/** 合同变更历史时间线 */
export function getChangeHistory(contractId: number) {
  return request.get(`/api/opr/contract-changes/history/${contractId}`)
}
