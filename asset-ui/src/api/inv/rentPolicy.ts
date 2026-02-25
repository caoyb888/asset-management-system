import request from '@/api/request'
import type { PageResult } from '@/api/base/project'

// ───────────────── 主实体类型 ─────────────────

export interface RentPolicyVO {
  id: number
  policyCode: string
  projectId: number
  policyType: number       // 1标准 2临时
  year1Rent: number
  year2Rent: number
  year1PropertyFee: number
  year2PropertyFee: number
  shopAttr: string
  formatType: string
  minLeaseTerm: number
  maxLeaseTerm: number
  rentGrowthRate: number
  feeGrowthRate: number
  freeRentPeriod: number
  depositMonths: number
  paymentCycle: number
  status: number           // 0草稿 1审批中 2通过 3驳回
  approvalId: string
  createdAt: string
}

export interface RentPolicyQuery {
  page?: number
  size?: number
  pageNum?: number
  pageSize?: number
  projectId?: number
  policyCode?: string   // 按编号模糊搜索
  status?: number
}

export interface RentPolicySaveDTO {
  projectId?: number
  policyType?: number
  year1Rent?: number
  year2Rent?: number
  year1PropertyFee?: number
  year2PropertyFee?: number
  shopAttr?: string
  formatType?: string
  minLeaseTerm?: number
  maxLeaseTerm?: number
  rentGrowthRate?: number
  feeGrowthRate?: number
  freeRentPeriod?: number
  depositMonths?: number
  paymentCycle?: number
}

// ───────────────── 分类指标类型 ─────────────────

export interface PolicyIndicatorVO {
  id?: number
  policyId?: number
  shopCategory: number    // 1主力店 2次主力店 3一般商铺
  rentPrice: number
  propertyFeePrice: number
  formatType: string
  rentGrowthRate: number
  feeGrowthRate: number
  freeRentMonths: number
  depositMonths: number
}

// ───────────────── API ─────────────────

/** 分页查询 */
export function getRentPolicyPage(params: RentPolicyQuery) {
  return request.get<PageResult<RentPolicyVO>, PageResult<RentPolicyVO>>(
    '/inv/rent-policies', { params })
}

/** 查询详情 */
export function getRentPolicyDetail(id: number) {
  return request.get<RentPolicyVO, RentPolicyVO>(`/inv/rent-policies/${id}`)
}

/** 获取已审批通过的政策列表 */
export function getApprovedPolicies(projectId?: number) {
  return request.get<RentPolicyVO[], RentPolicyVO[]>('/inv/rent-policies/approved',
    { params: projectId ? { projectId } : undefined })
}

/** 新增 */
export function createRentPolicy(data: RentPolicySaveDTO) {
  return request.post<number, number>('/inv/rent-policies', data)
}

/** 编辑 */
export function updateRentPolicy(id: number, data: RentPolicySaveDTO) {
  return request.put<void, void>(`/inv/rent-policies/${id}`, data)
}

/** 删除 */
export function deleteRentPolicy(id: number) {
  return request.delete<void, void>(`/inv/rent-policies/${id}`)
}

/** 提交审批 */
export function submitRentPolicyApproval(id: number) {
  return request.post<void, void>(`/inv/rent-policies/${id}/submit-approval`)
}

/** 审批回调（Mock 用） */
export function rentPolicyApprovalCallback(id: number, approved: boolean) {
  return request.post<void, void>(`/inv/rent-policies/${id}/approval-callback`, { approved })
}

/** 查询分类指标列表 */
export function getPolicyIndicators(id: number) {
  return request.get<PolicyIndicatorVO[], PolicyIndicatorVO[]>(
    `/inv/rent-policies/${id}/indicators`)
}

/** 批量保存分类指标（全量替换） */
export function savePolicyIndicators(id: number, data: PolicyIndicatorVO[]) {
  return request.post<void, void>(`/inv/rent-policies/${id}/indicators`, data)
}
