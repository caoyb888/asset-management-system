import { http } from '@/api/request'

/** 待办/已办/我发起的 — 通用查询参数 */
export interface TaskQueryDTO {
  businessType?: string
  title?: string
  pageNum?: number
  pageSize?: number
}

/** 待办任务 VO */
export interface TodoTaskVO {
  /** 流程实例 ID */
  processInstanceId: string
  /** Flowable 任务 ID */
  taskId: string
  /** 业务类型编码 */
  businessType: string
  /** 业务类型名称 */
  businessTypeName: string
  /** 业务单据 ID */
  businessId: number
  /** 审批标题 */
  title: string
  /** 发起人 */
  initiator: string
  /** 发起人姓名 */
  initiatorName: string
  /** 项目名称 */
  projectName?: string
  /** 任务创建时间 */
  createdAt: string
}

/** 已办任务 VO */
export interface DoneTaskVO {
  processInstanceId: string
  taskId: string
  businessType: string
  businessTypeName: string
  businessId: number
  title: string
  initiator: string
  initiatorName: string
  projectName?: string
  /** 审批结果：2通过 3驳回 */
  result: number
  resultName: string
  comment?: string
  /** 审批完成时间 */
  completedAt: string
  createdAt: string
}

/** 我发起的 VO */
export interface InitiatedVO {
  processInstanceId: string
  businessType: string
  businessTypeName: string
  businessId: number
  title: string
  /** 当前状态：1审批中 2已通过 3已驳回 4已撤回 */
  status: number
  statusName: string
  projectName?: string
  /** 当前审批人 */
  currentAssignee?: string
  createdAt: string
  completedAt?: string
}

/** 分页结果 */
export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
}

/** 业务类型选项 */
export const BUSINESS_TYPE_OPTIONS = [
  { value: 'INV_INTENTION', label: '意向协议审批' },
  { value: 'INV_OPENING', label: '开业审批' },
  { value: 'INV_RENT_DECOMP', label: '租金分解审批' },
  { value: 'OPR_CONTRACT_CHANGE', label: '合同变更审批' },
  { value: 'OPR_TERMINATION', label: '合同解约审批' },
  { value: 'FIN_WRITE_OFF', label: '核销审批' },
  { value: 'FIN_DEDUCTION', label: '减免审批' },
  { value: 'FIN_ADJUSTMENT', label: '调整审批' },
]

export function businessTypeLabel(code: string): string {
  return BUSINESS_TYPE_OPTIONS.find(o => o.value === code)?.label ?? code
}

/** 业务类型 → 前端路由映射 */
export const BUSINESS_ROUTE_MAP: Record<string, (bizId: number) => string> = {
  INV_INTENTION: (id) => `/inv/intentions/form?id=${id}`,
  INV_OPENING: (id) => `/inv/opening-approvals/form?id=${id}`,
  INV_RENT_DECOMP: (id) => `/inv/rent-decomps/form?id=${id}`,
  OPR_CONTRACT_CHANGE: (id) => `/opr/contract-changes/${id}`,
  OPR_TERMINATION: (id) => `/opr/terminations/${id}`,
  FIN_WRITE_OFF: (id) => `/fin/write-offs?highlight=${id}`,
  FIN_DEDUCTION: (id) => `/fin/receivables?highlight=${id}`,
  FIN_ADJUSTMENT: (id) => `/fin/receivables?highlight=${id}`,
}

// ─── API ──────────────────────────────────────────────────

/** 我的待办（分页） */
export function getTodoPage(params: TaskQueryDTO) {
  return http.get<PageResult<TodoTaskVO>>('/wf/tasks/todo', { params })
}

/** 我的已办（分页） */
export function getDonePage(params: TaskQueryDTO) {
  return http.get<PageResult<DoneTaskVO>>('/wf/tasks/done', { params })
}

/** 我发起的（分页） */
export function getInitiatedPage(params: TaskQueryDTO) {
  return http.get<PageResult<InitiatedVO>>('/wf/tasks/initiated', { params })
}

/** 待办数量统计 */
export function getTodoCount() {
  return http.get<number>('/wf/tasks/count')
}
