import request from '@/api/request'
import type { PageResult } from '@/api/base/project'

export interface RentPolicyVO {
  id: number
  policyCode: string
  policyName: string
  projectId: number
  projectName: string
  effectiveYear: string
  status: number    // 0草稿 1审批中 2生效 3驳回
  description: string
  createdAt: string
}

export interface RentPolicyQuery {
  page?: number
  size?: number
  policyName?: string
  projectId?: number
  status?: number
  effectiveYear?: string
}

export interface RentPolicySaveDTO {
  id?: number
  policyName: string
  projectId: number
  effectiveYear: string
  description?: string
}

export function getRentPolicyPage(params: RentPolicyQuery) {
  return request.get<PageResult<RentPolicyVO>, PageResult<RentPolicyVO>>('/inv/rent-policies', { params })
}

export function getRentPolicyDetail(id: number) {
  return request.get<RentPolicyVO, RentPolicyVO>(`/inv/rent-policies/${id}`)
}

export function createRentPolicy(data: RentPolicySaveDTO) {
  return request.post<number, number>('/inv/rent-policies', data)
}

export function updateRentPolicy(id: number, data: RentPolicySaveDTO) {
  return request.put<void, void>(`/inv/rent-policies/${id}`, data)
}

export function deleteRentPolicy(id: number) {
  return request.delete<void, void>(`/inv/rent-policies/${id}`)
}

export function submitRentPolicyApproval(id: number) {
  return request.post<void, void>(`/inv/rent-policies/${id}/submit-approval`)
}

/** 获取已生效的租决政策列表（供租金分解选择） */
export function getApprovedPolicyList() {
  return request.get<RentPolicyVO[], RentPolicyVO[]>('/inv/rent-policies/approved')
}
