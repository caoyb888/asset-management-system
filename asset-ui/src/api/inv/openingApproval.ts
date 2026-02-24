import request from '@/api/request'
import type { PageResult } from '@/api/base/project'

// ───────────────── 主实体类型 ─────────────────

export interface OpeningApprovalVO {
  id: number
  approvalCode: string
  projectId: number
  buildingId: number
  floorId: number
  shopId: number
  contractId: number
  merchantId: number
  plannedOpeningDate: string
  actualOpeningDate: string
  status: number    // 0草稿 1审批中 2通过 3驳回
  approvalId: string
  remark: string
  createdAt: string
}

export interface OpeningApprovalQuery {
  pageNum?: number
  pageSize?: number
  projectId?: number
  contractId?: number
  status?: number
}

export interface OpeningApprovalSaveDTO {
  projectId?: number
  buildingId?: number
  floorId?: number
  shopId?: number
  contractId?: number
  merchantId?: number
  plannedOpeningDate?: string
  actualOpeningDate?: string
  remark?: string
}

// ───────────────── 附件类型 ─────────────────

export interface OpeningAttachmentVO {
  id: number
  openingApprovalId: number
  fileName: string
  fileUrl: string
  fileType: string
  fileSize: number
}

// ───────────────── API ─────────────────

/** 分页查询 */
export function getOpeningApprovalPage(params: OpeningApprovalQuery) {
  return request.get<PageResult<OpeningApprovalVO>, PageResult<OpeningApprovalVO>>(
    '/inv/opening-approvals', { params })
}

/** 查询详情 */
export function getOpeningApprovalDetail(id: number) {
  return request.get<OpeningApprovalVO, OpeningApprovalVO>(`/inv/opening-approvals/${id}`)
}

/** 新增 */
export function createOpeningApproval(data: OpeningApprovalSaveDTO) {
  return request.post<number, number>('/inv/opening-approvals', data)
}

/** 编辑 */
export function updateOpeningApproval(id: number, data: OpeningApprovalSaveDTO) {
  return request.put<void, void>(`/inv/opening-approvals/${id}`, data)
}

/** 删除 */
export function deleteOpeningApproval(id: number) {
  return request.delete<void, void>(`/inv/opening-approvals/${id}`)
}

/** 提交审批（草稿→审批中） */
export function submitOpeningApproval(id: number) {
  return request.post<void, void>(`/inv/opening-approvals/${id}/submit`)
}

/** 审批回调（Mock 用） */
export function openingApprovalCallback(id: number, approved: boolean) {
  return request.post<void, void>(`/inv/opening-approvals/${id}/approval-callback`, { approved })
}

/** 查询附件列表 */
export function getOpeningAttachments(id: number) {
  return request.get<OpeningAttachmentVO[], OpeningAttachmentVO[]>(
    `/inv/opening-approvals/${id}/attachments`)
}

/** 新增附件记录 */
export function addOpeningAttachment(id: number, data: Partial<OpeningAttachmentVO>) {
  return request.post<number, number>(`/inv/opening-approvals/${id}/attachments`, data)
}

/** 删除附件 */
export function deleteOpeningAttachment(attachmentId: number) {
  return request.delete<void, void>(`/inv/opening-approvals/attachments/${attachmentId}`)
}
