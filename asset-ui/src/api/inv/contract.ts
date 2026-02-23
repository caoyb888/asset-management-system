import request from '@/api/request'
import type { PageResult } from '@/api/base/project'

// ───────────────── 类型定义 ─────────────────

export interface ContractVO {
  id: number
  contractCode: string
  contractName: string
  projectId: number
  projectName: string
  merchantId: number
  merchantName: string
  brandId: number
  brandName: string
  contractType: number   // 1标准 2临时 3补充协议
  status: number         // 0草稿 1审批中 2生效 3到期 4终止
  version: number
  isCurrent: number
  contractStart: string
  contractEnd: string
  rentSchemeId: number
  paymentCycle: number
  billingMode: number
  createdAt: string
}

export interface ContractQuery {
  page?: number
  size?: number
  contractCode?: string
  merchantName?: string
  status?: number
  projectId?: number
}

export interface ContractSaveDTO {
  id?: number
  contractName?: string
  contractType?: number
  projectId?: number
  merchantId?: number
  brandId?: number
  rentSchemeId?: number
  paymentCycle?: number
  billingMode?: number
  contractStart?: string
  contractEnd?: string
}

// ───────────────── API ─────────────────

/** 分页查询合同列表 */
export function getContractPage(params: ContractQuery) {
  return request.get<PageResult<ContractVO>, PageResult<ContractVO>>('/inv/contracts', { params })
}

/** 查询合同详情 */
export function getContractDetail(id: number) {
  return request.get<ContractVO, ContractVO>(`/inv/contracts/${id}`)
}

/** 新增合同 */
export function createContract(data: ContractSaveDTO) {
  return request.post<number, number>('/inv/contracts', data)
}

/** 编辑合同 */
export function updateContract(id: number, data: ContractSaveDTO) {
  return request.put<void, void>(`/inv/contracts/${id}`, data)
}

/** 变更合同状态 */
export function updateContractStatus(id: number, status: number) {
  return request.put<void, void>(`/inv/contracts/${id}/status`, { status })
}

/** 发起合同审批 */
export function submitContractApproval(id: number) {
  return request.post<void, void>(`/inv/contracts/${id}/submit-approval`)
}

/** 意向转合同 */
export function convertFromIntention(intentionId: number, data: { contractType: number; contractName: string }) {
  return request.post<number, number>(`/inv/contracts/from-intention/${intentionId}`, data)
}
