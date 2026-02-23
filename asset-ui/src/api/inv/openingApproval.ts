import request from '@/api/request'
import type { PageResult } from '@/api/base/project'

export interface OpeningApprovalVO {
  id: number
  approvalCode: string
  contractId: number
  contractCode: string
  merchantName: string
  shopNo: string
  plannedOpenDate: string
  status: number    // 0草稿 1审批中 2审批通过 3驳回
  remark: string
  createdAt: string
}

export interface OpeningApprovalQuery {
  page?: number
  size?: number
  status?: number
  contractCode?: string
}

export interface OpeningApprovalSaveDTO {
  id?: number
  contractId: number
  plannedOpenDate: string
  remark?: string
}

export function getOpeningApprovalPage(params: OpeningApprovalQuery) {
  return request.get<PageResult<OpeningApprovalVO>, PageResult<OpeningApprovalVO>>('/inv/opening-approvals', { params })
}

export function getOpeningApprovalDetail(id: number) {
  return request.get<OpeningApprovalVO, OpeningApprovalVO>(`/inv/opening-approvals/${id}`)
}

export function createOpeningApproval(data: OpeningApprovalSaveDTO) {
  return request.post<number, number>('/inv/opening-approvals', data)
}

export function submitOpeningApproval(id: number) {
  return request.post<void, void>(`/inv/opening-approvals/${id}/submit`)
}

export function deleteOpeningApproval(id: number) {
  return request.delete<void, void>(`/inv/opening-approvals/${id}`)
}
