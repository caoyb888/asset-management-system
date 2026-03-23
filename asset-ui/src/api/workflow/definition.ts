import { http } from '@/api/request'

/** 流程定义 */
export interface WfProcessDefinition {
  id: number
  processKey: string
  processName: string
  businessType: string
  bpmnXml?: string
  approverStrategy: string
  approverConfig?: string
  isEnabled: number
  version: number
  createdAt?: string
  updatedAt?: string
}

/** 审批人策略选项 */
export const APPROVER_STRATEGY_OPTIONS = [
  { value: 'ROLE', label: '按角色' },
  { value: 'DEPT_LEADER', label: '部门主管' },
  { value: 'SPECIFIC_USER', label: '指定用户' },
  { value: 'INITIATOR_LEADER', label: '发起人上级' },
]

export function approverStrategyLabel(code: string): string {
  return APPROVER_STRATEGY_OPTIONS.find(o => o.value === code)?.label ?? code
}

// ─── API ──────────────────────────────────────────────────

/** 查询流程定义列表 */
export function listDefinitions() {
  return http.get<WfProcessDefinition[]>('/wf/definitions')
}

/** 新增/更新流程定义 */
export function saveDefinition(data: Partial<WfProcessDefinition>) {
  return http.post<number>('/wf/definitions', data)
}

/** 启用/禁用流程定义 */
export function toggleDefinition(id: number) {
  return http.put(`/wf/definitions/${id}/toggle`)
}

/** 预览 BPMN 流程图 XML */
export function previewBpmnXml(key: string) {
  return http.get<string>(`/wf/definitions/${key}/preview`)
}
