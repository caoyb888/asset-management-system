import request from '@/api/request'
import type { PageResult } from '@/api/base/project'

// ───────────────── 主实体类型 ─────────────────

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
  intentionId: number
  signingEntity: string
  contractType: number   // 1标准 2临时 3补充协议
  status: number         // 0草稿 1审批中 2生效 3到期 4终止
  version: number
  isCurrent: number
  rentSchemeId: number
  contractStart: string
  contractEnd: string
  deliveryDate: string
  decorationStart: string
  decorationEnd: string
  openingDate: string
  paymentCycle: number
  billingMode: number
  totalAmount: number
  contractText?: string
  createdAt: string
  updatedAt: string
}

export interface ContractQuery {
  pageNum?: number
  pageSize?: number
  projectId?: number
  status?: number
  merchantId?: number
  keyword?: string
}

export interface ContractSaveDTO {
  contractName?: string
  contractType?: number
  projectId?: number
  merchantId?: number
  brandId?: number
  signingEntity?: string
  rentSchemeId?: number
  deliveryDate?: string
  decorationStart?: string
  decorationEnd?: string
  openingDate?: string
  contractStart?: string
  contractEnd?: string
  paymentCycle?: number
  billingMode?: number
  contractText?: string
}

// ───────────────── 子资源类型 ─────────────────

export interface ContractShopItemDTO {
  shopId: number
  buildingId?: number
  floorId?: number
  formatType?: string
  area?: number
  rentUnitPrice?: number
  propertyUnitPrice?: number
}

export interface ContractShopVO {
  id: number
  contractId: number
  shopId: number
  buildingId: number
  floorId: number
  formatType: string
  area: number
  rentUnitPrice: number
  propertyUnitPrice: number
}

export interface ContractFeeItemDTO {
  feeItemId?: number
  feeName?: string
  chargeType: number
  unitPrice?: number
  area?: number
  startDate?: string
  endDate?: string
  periodIndex?: number
  formulaParams?: Record<string, unknown>
}

export interface ContractFeeVO {
  id: number
  contractId: number
  feeItemId: number
  feeName: string
  chargeType: number
  unitPrice: number
  area: number
  amount: number
  startDate: string
  endDate: string
  formulaParams: Record<string, unknown>
}

export interface ContractFeeStageItemDTO {
  contractFeeId: number
  shopId?: number
  stageStart: string
  stageEnd: string
  unitPrice?: number
  commissionRate?: number
  minCommissionAmount?: number
}

export interface ContractFeeStageVO {
  id: number
  contractFeeId: number
  shopId: number
  stageStart: string
  stageEnd: string
  unitPrice: number
  commissionRate: number
  minCommissionAmount: number
  amount: number
}

export interface ContractBillingVO {
  id: number
  contractId: number
  feeItemId: number
  billingStart: string
  billingEnd: string
  dueDate: string
  amount: number
  billingType: number  // 1首账期 2普通
  status: number
}

export interface ContractApprovalCallbackDTO {
  approved: boolean
  approvalId?: string
  comment?: string
}

export interface CostResultVO {
  totalAmount: number
  fees: ContractFeeVO[]
}

export interface ContractVersionVO {
  id: number
  contractId: number
  version: number
  snapshotData: Record<string, unknown>
  changeReason: string
  createdBy: number
  createdAt: string
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

/** 新增合同（草稿） */
export function createContract(data: ContractSaveDTO) {
  return request.post<number, number>('/inv/contracts', data)
}

/** 编辑合同 */
export function updateContract(id: number, data: ContractSaveDTO) {
  return request.put<void, void>(`/inv/contracts/${id}`, data)
}

/** 删除合同 */
export function deleteContract(id: number) {
  return request.delete<void, void>(`/inv/contracts/${id}`)
}

/** 意向转合同 */
export function convertFromIntention(intentionId: number, data: { contractName: string; contractType: number }) {
  return request.post<number, number>(`/inv/contracts/from-intention/${intentionId}`, data)
}

/** 发起审批 */
export function submitContractApproval(id: number) {
  return request.post<void, void>(`/inv/contracts/${id}/submit-approval`)
}

/** 审批回调（Mock用） */
export function approvalCallback(id: number, data: ContractApprovalCallbackDTO) {
  return request.post<void, void>(`/inv/contracts/${id}/approval-callback`, data)
}

/** 更新合同状态 */
export function updateContractStatus(id: number, status: number) {
  return request.put<void, void>(`/inv/contracts/${id}/status`, null, { params: { status } })
}

// ─── 商铺关联 ───

/** 批量保存商铺关联（全量替换） */
export function saveContractShops(id: number, data: ContractShopItemDTO[]) {
  return request.post<void, void>(`/inv/contracts/${id}/shops`, data)
}

/** 查询合同关联商铺列表 */
export function getContractShops(id: number) {
  return request.get<ContractShopVO[], ContractShopVO[]>(`/inv/contracts/${id}/shops`)
}

// ─── 费项配置 ───

/** 批量保存费项配置（全量替换） */
export function saveContractFees(id: number, data: ContractFeeItemDTO[]) {
  return request.post<void, void>(`/inv/contracts/${id}/fees`, data)
}

/** 查询费项列表 */
export function getContractFees(id: number) {
  return request.get<ContractFeeVO[], ContractFeeVO[]>(`/inv/contracts/${id}/fees`)
}

// ─── 分铺计租阶段 ───

/** 批量保存分铺计租阶段 */
export function saveContractFeeStages(id: number, data: ContractFeeStageItemDTO[]) {
  return request.post<void, void>(`/inv/contracts/${id}/fee-stages`, data)
}

/** 查询分铺计租阶段 */
export function getContractFeeStages(id: number) {
  return request.get<ContractFeeStageVO[], ContractFeeStageVO[]>(`/inv/contracts/${id}/fee-stages`)
}

// ─── 费用与账期 ───

/** 生成费用明细 */
export function generateContractCost(id: number) {
  return request.post<CostResultVO, CostResultVO>(`/inv/contracts/${id}/generate-cost`)
}

/** 生成账期（全量替换） */
export function generateContractBilling(id: number) {
  return request.post<ContractBillingVO[], ContractBillingVO[]>(`/inv/contracts/${id}/billing`)
}

/** 查询账期列表 */
export function getContractBillingList(id: number) {
  return request.get<ContractBillingVO[], ContractBillingVO[]>(`/inv/contracts/${id}/billing`)
}

/** 查询合同版本历史 */
export function getContractVersions(id: number) {
  return request.get<ContractVersionVO[], ContractVersionVO[]>(`/inv/contracts/${id}/versions`)
}
