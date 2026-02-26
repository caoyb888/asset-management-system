import request from '@/api/request'

/** 变更类型编码 */
export const CHANGE_TYPE_OPTIONS = [
  { code: 'RENT',    label: '租金变更' },
  { code: 'FEE',     label: '费项单价变更' },
  { code: 'TERM',    label: '租期变更' },
  { code: 'AREA',    label: '面积变更' },
  { code: 'BRAND',   label: '品牌变更' },
  { code: 'TENANT',  label: '租户主体变更' },
  { code: 'COMPANY', label: '公司名称变更' },
  { code: 'CLAUSE',  label: '合同条款变更' },
]

export function changeTypeLabel(code: string): string {
  return CHANGE_TYPE_OPTIONS.find(o => o.code === code)?.label ?? code
}

/** 合同变更查询参数 */
export interface ChangeQueryDTO {
  contractId?: number
  ledgerId?: number
  projectId?: number
  changeCode?: string
  status?: number
  changeTypeCode?: string
  pageNum?: number
  pageSize?: number
}

/** 合同变更主表（列表用） */
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
  changeTypeCodes?: string[]  // 后端填充
  createdAt?: string
}

/** 变更字段明细 */
export interface OprContractChangeDetail {
  id: number
  changeId: number
  fieldName: string
  fieldLabel: string
  oldValue?: string
  newValue?: string
  dataType?: string
}

/** 变更单详情 VO */
export interface ChangeDetailVO extends OprContractChange {
  changeTypeNames?: string[]
  details?: OprContractChangeDetail[]
  statusName?: string
  contractCode?: string
  contractName?: string
  merchantName?: string
  projectName?: string
}

/** 新增/编辑变更单参数 */
export interface ChangeCreateDTO {
  contractId: number
  ledgerId?: number
  changeTypeCodes: string[]
  effectiveDate: string
  reason?: string
  changeFields?: Record<string, unknown>
}

/** 审批回调 */
export interface ApprovalCallbackDTO {
  approvalId?: string
  status: 2 | 3  // 2通过/3驳回
  comment?: string
}

/** 变更影响预览结果 */
export interface ChangeImpactVO {
  affectedPlanCount: number
  originalTotalAmount: number
  newTotalAmount: number
  amountDiff: number
  fieldComparisons?: { field: string; label: string; oldValue: string; newValue: string }[]
  impactDesc?: string
}

/** 分页查询变更列表 */
export function getChangePage(params: ChangeQueryDTO) {
  return request.get('/api/opr/contract-changes', { params })
}

/** 变更详情（含明细/快照） */
export function getChangeById(id: number) {
  return request.get<ChangeDetailVO>(`/api/opr/contract-changes/${id}`)
}

/** 新增变更单 */
export function createChange(data: ChangeCreateDTO) {
  return request.post<number>('/api/opr/contract-changes', data)
}

/** 编辑变更单 */
export function updateChange(id: number, data: ChangeCreateDTO) {
  return request.put(`/api/opr/contract-changes/${id}`, data)
}

/** 预览变更影响 */
export function previewImpact(id: number) {
  return request.post<ChangeImpactVO>(`/api/opr/contract-changes/${id}/preview-impact`)
}

/** 提交审批 */
export function submitChangeApproval(id: number) {
  return request.post(`/api/opr/contract-changes/${id}/submit-approval`)
}

/** 审批回调（管理员手动触发或 OA 回调） */
export function approvalCallback(id: number, data: ApprovalCallbackDTO) {
  return request.post(`/api/opr/contract-changes/${id}/approval-callback`, data)
}

/** 合同变更历史时间线 */
export function getChangeHistory(contractId: number) {
  return request.get<ChangeDetailVO[]>(`/api/opr/contract-changes/history/${contractId}`)
}
