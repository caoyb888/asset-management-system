import request from '@/api/request'
import type { PageResult } from '@/api/base/project'

// ───────────────── 类型定义 ─────────────────

export interface IntentionVO {
  id: number
  intentionCode: string
  projectId: number
  projectName: string
  merchantId: number
  merchantName: string
  brandId: number
  brandName: string
  rentSchemeId: number
  schemeName: string
  status: number        // 0草稿 1审批中 2审批通过 3驳回 4已转合同
  version: number
  isCurrent: number
  totalAmount: number
  paymentCycle: number
  billingMode: number
  contractStart: string
  contractEnd: string
  deliveryDate: string
  decorationStart: string
  decorationEnd: string
  openingDate: string
  createdAt: string
  updatedAt: string
}

export interface IntentionQuery {
  page?: number
  size?: number
  projectId?: number
  projectName?: string
  merchantName?: string
  status?: number
}

export interface IntentionSaveDTO {
  id?: number
  projectId?: number
  merchantId?: number
  brandId?: number
  rentSchemeId?: number
  paymentCycle?: number
  billingMode?: number
  contractStart?: string
  contractEnd?: string
  deliveryDate?: string
  decorationStart?: string
  decorationEnd?: string
  openingDate?: string
  status?: number
}

// ───────────────── API ─────────────────

/** 分页查询意向协议 */
export function getIntentionPage(params: IntentionQuery) {
  return request.get<PageResult<IntentionVO>, PageResult<IntentionVO>>('/inv/intentions', { params })
}

/** 查询意向协议详情 */
export function getIntentionDetail(id: number) {
  return request.get<IntentionVO, IntentionVO>(`/inv/intentions/${id}`)
}

/** 新增意向协议（草稿） */
export function createIntention(data: IntentionSaveDTO) {
  return request.post<number, number>('/inv/intentions', data)
}

/** 编辑意向协议 */
export function updateIntention(id: number, data: IntentionSaveDTO) {
  return request.put<void, void>(`/inv/intentions/${id}`, data)
}

/** 删除意向协议 */
export function deleteIntention(id: number) {
  return request.delete<void, void>(`/inv/intentions/${id}`)
}

/** 暂存草稿 */
export function saveDraft(id: number, data: IntentionSaveDTO) {
  return request.put<void, void>(`/inv/intentions/${id}/draft`, data)
}

/** 发起审批 */
export function submitApproval(id: number) {
  return request.post<void, void>(`/inv/intentions/${id}/submit-approval`)
}

/** 生成费用 */
export function generateCost(id: number) {
  return request.post<{ totalAmount: number }, { totalAmount: number }>(`/inv/intentions/${id}/generate-cost`)
}

/** 生成账期 */
export function generateBilling(id: number) {
  return request.post<void, void>(`/inv/intentions/${id}/billing`)
}

/** 查询账期列表 */
export function getBillingList(id: number) {
  return request.get<unknown[], unknown[]>(`/inv/intentions/${id}/billing`)
}
