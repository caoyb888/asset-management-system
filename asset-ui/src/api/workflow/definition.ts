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

/** 节点配置 DTO（与后端 NodeConfigDTO 对齐） */
export interface NodeConfigDTO {
  nodeId: string
  nodeType: 'START' | 'APPROVER' | 'CONDITION' | 'END'
  nodeName: string
  nodeOrder: number
  approverStrategy?: string
  roleCode?: string
  userId?: number
  userDisplayName?: string  // 前端展示用，不提交后端
  timeoutHours?: number
  conditionType?: string
  conditionOp?: string
  conditionValue?: number
  conditionExpr?: string
  remark?: string
}

/** 流程定义保存 DTO（扩展支持 nodeConfigs 可视化模式） */
export interface WfDefinitionSaveDTO {
  id?: number
  processKey?: string
  processName?: string
  businessType?: string
  isEnabled?: number
  nodeConfigs?: NodeConfigDTO[]
  bpmnXml?: string
}

/** 审批人策略选项 */
export const APPROVER_STRATEGY_OPTIONS = [
  { value: 'DEPT_LEADER', label: '部门主管（自动解析）' },
  { value: 'INITIATOR_LEADER', label: '发起人上级（自动解析）' },
  { value: 'ROLE', label: '指定角色' },
  { value: 'SPECIFIC_USER', label: '指定用户' },
]

/** 条件运算符选项 */
export const CONDITION_OP_OPTIONS = [
  { value: 'GTE', label: '≥（大于等于）' },
  { value: 'GT', label: '>（大于）' },
  { value: 'LTE', label: '≤（小于等于）' },
  { value: 'LT', label: '<（小于）' },
  { value: 'EQ', label: '=（等于）' },
]

export function approverStrategyLabel(code: string): string {
  return APPROVER_STRATEGY_OPTIONS.find(o => o.value === code)?.label ?? code
}

export function conditionOpLabel(op: string): string {
  const map: Record<string, string> = {
    GTE: '≥', GT: '>', LTE: '≤', LT: '<', EQ: '=',
  }
  return map[op] ?? op
}

// ─── API ──────────────────────────────────────────────────

/** 查询流程定义列表 */
export function listDefinitions() {
  return http.get<WfProcessDefinition[]>('/wf/definitions')
}

/** 新增/更新流程定义（支持 nodeConfigs 可视化模式和 bpmnXml 源码模式） */
export function saveDefinition(data: WfDefinitionSaveDTO) {
  return http.post<number>('/wf/definitions', data)
}

/** 启用/禁用流程定义 */
export function toggleDefinition(id: number) {
  return http.put(`/wf/definitions/${id}/toggle`)
}

/** 预览 BPMN 流程图 XML（按 processKey 查库） */
export function previewBpmnXml(key: string) {
  return http.get<string>(`/wf/definitions/${key}/preview`)
}

/** WD-05 查询某流程定义的节点配置列表（可视化设计器回显） */
export function getDefinitionNodes(id: number) {
  return http.get<NodeConfigDTO[]>(`/wf/definitions/${id}/nodes`)
}

/** WD-06 根据节点配置预览生成的 BPMN XML（不保存） */
export function previewBpmnByNodes(dto: { processKey: string; processName: string; nodeConfigs: NodeConfigDTO[] }) {
  return http.post<string>('/wf/definitions/preview-bpmn', dto)
}

/** WD-07 手动重新部署流程定义到 Flowable 引擎 */
export function redeployDefinition(id: number) {
  return http.post(`/wf/definitions/${id}/redeploy`)
}
