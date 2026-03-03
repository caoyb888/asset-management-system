import request from '@/api/request'

// ─────────────────────────── DTO / VO ───────────────────────────

export interface ScheduleTaskDTO {
  taskName: string
  reportCode: string
  cronExpression: string
  recipients: string[]
  ccRecipients?: string[]
  exportFormat?: 'EXCEL' | 'PDF'
  filterParams?: Record<string, any>
  enabled?: boolean
}

export interface ScheduleTaskVO {
  id: number
  taskCode: string
  taskName: string
  reportCode: string
  cronExpression: string
  recipients: string[]
  ccRecipients: string[]
  exportFormat: string
  lastRunTime?: string
  nextRunTime?: string
  runCount: number
  failCount: number
  /** 0=禁用 1=启用 */
  status: 0 | 1
  createdAt: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

// ─────────────────────────── API ───────────────────────────

/** 分页查询定时推送任务 */
export function pageScheduleTasks(params: { keyword?: string; pageNum?: number; pageSize?: number }) {
  return request.get<PageResult<ScheduleTaskVO>>('/rpt/common/schedule-tasks', { params })
}

/** 查询任务详情 */
export function getScheduleTask(id: number) {
  return request.get<ScheduleTaskVO>(`/rpt/common/schedule-tasks/${id}`)
}

/** 创建定时推送任务 */
export function createScheduleTask(dto: ScheduleTaskDTO) {
  return request.post<number>('/rpt/common/schedule-tasks', dto)
}

/** 更新定时推送任务 */
export function updateScheduleTask(id: number, dto: ScheduleTaskDTO) {
  return request.put<void>(`/rpt/common/schedule-tasks/${id}`, dto)
}

/** 删除定时推送任务 */
export function deleteScheduleTask(id: number) {
  return request.delete<void>(`/rpt/common/schedule-tasks/${id}`)
}

/** 启用/禁用切换 */
export function toggleScheduleTask(id: number) {
  return request.put<number>(`/rpt/common/schedule-tasks/${id}/toggle`)
}
