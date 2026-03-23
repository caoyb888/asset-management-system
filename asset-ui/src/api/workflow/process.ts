import { http } from '@/api/request'
import type { PageResult } from '@/api/workflow/task'

/** 流程实例查询参数 */
export interface ProcessPageQuery {
  businessType?: string
  title?: string
  status?: number
  initiatorId?: number
  projectId?: number
  pageNum?: number
  pageSize?: number
}

/** 流程实例 VO */
export interface ProcessInstanceVO {
  id: number
  processKey: string
  flowableInstanceId?: string
  businessType: string
  businessTypeName: string
  businessId: number
  title: string
  initiatorId: number
  initiatorName: string
  projectId?: number
  currentAssigneeId?: number
  currentNodeName?: string
  status: number
  statusName: string
  resultComment?: string
  priority: number
  startedAt?: string
  finishedAt?: string
  durationMs?: number
  createdAt?: string
}

/** 审批效率统计 */
export interface ApprovalStatistics {
  total: number
  pendingCount: number
  in_progressCount: number
  approvedCount: number
  rejectedCount: number
  revokedCount: number
  cancelledCount: number
  avgDurationMs: number
  approvalRate: number
}

// ─── API ──────────────────────────────────────────────────

/** 流程实例分页查询（管理员） */
export function getProcessPage(params: ProcessPageQuery) {
  return http.get<PageResult<ProcessInstanceVO>>('/wf/processes', { params })
}

/** 审批效率统计 */
export function getStatistics() {
  return http.get<ApprovalStatistics>('/wf/processes/statistics')
}

/** 作废流程（管理员） */
export function cancelProcess(id: number) {
  return http.post(`/wf/processes/${id}/cancel`)
}
