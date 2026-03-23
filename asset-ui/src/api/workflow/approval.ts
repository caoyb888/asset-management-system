import { http } from '@/api/request'

/** 审批记录 VO */
export interface ApprovalRecordVO {
  /** 操作类型：create/submit/approve/reject/revoke/reassign */
  action: string
  /** 操作人姓名 */
  operatorName: string
  /** 审批意见 */
  comment?: string
  /** 操作时间 */
  operateTime: string
  /** 节点名称 */
  nodeName?: string
}

/** 流程详情 VO */
export interface ProcessDetailVO {
  processInstanceId: string
  businessType: string
  businessTypeName: string
  businessId: number
  title: string
  /** 发起人 */
  initiator: string
  initiatorName: string
  /** 当前状态：1审批中 2已通过 3已驳回 4已撤回 5已作废 */
  status: number
  statusName: string
  projectName?: string
  createdAt: string
  completedAt?: string
  /** 审批记录 */
  records: ApprovalRecordVO[]
}

/** 审批操作参数 */
export interface ApprovalActionDTO {
  comment?: string
}

// ─── API ──────────────────────────────────────────────────

/** 通过审批 */
export function approveTask(processInstanceId: string, data?: ApprovalActionDTO) {
  return http.post(`/wf/approvals/${processInstanceId}/approve`, data)
}

/** 驳回审批 */
export function rejectTask(processInstanceId: string, data?: ApprovalActionDTO) {
  return http.post(`/wf/approvals/${processInstanceId}/reject`, data)
}

/** 撤回审批（发起人） */
export function revokeProcess(processInstanceId: string) {
  return http.post(`/wf/approvals/${processInstanceId}/revoke`)
}

/** 查询流程详情 */
export function getProcessDetail(processInstanceId: string) {
  return http.get<ProcessDetailVO>(`/wf/approvals/${processInstanceId}`)
}

/** 查询审批记录（timeline） */
export function getApprovalRecords(processInstanceId: string) {
  return http.get<ApprovalRecordVO[]>(`/wf/approvals/${processInstanceId}/records`)
}

/** 催办 */
export function urgeProcess(processInstanceId: string) {
  return http.post(`/wf/approvals/${processInstanceId}/urge`)
}

/** 按业务单据查流程 */
export function getProcessByBusiness(businessType: string, businessId: number) {
  return http.get<ProcessDetailVO>(`/wf/approvals/by-business`, {
    params: { businessType, businessId },
  })
}
