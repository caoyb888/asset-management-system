import request from '@/api/request'
import type { PageResult } from '@/api/base/project'

// ───────────────── 主实体类型 ─────────────────

export interface IntentionVO {
  id: number
  intentionCode: string
  intentionName: string
  projectId: number
  projectName: string
  merchantId: number
  merchantName: string
  brandId: number
  brandName: string
  signingEntity: string
  rentSchemeId: number
  schemeName: string
  status: number        // 0草稿 1审批中 2审批通过 3驳回 4已转合同
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
  pageNum?: number
  pageSize?: number
  projectId?: number
  status?: number
  merchantId?: number
  brandId?: number
  keyword?: string
}

export interface IntentionSaveDTO {
  intentionName?: string
  projectId?: number
  merchantId?: number
  brandId?: number
  signingEntity?: string
  rentSchemeId?: number
  paymentCycle?: number
  billingMode?: number
  contractStart?: string
  contractEnd?: string
  deliveryDate?: string
  decorationStart?: string
  decorationEnd?: string
  openingDate?: string
  agreementText?: string
}

// ───────────────── 子资源类型 ─────────────────

export interface IntentionShopItemDTO {
  shopId: number
  buildingId?: number
  floorId?: number
  formatType?: string
  area?: number
}

export interface IntentionFeeItemDTO {
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

/** 意向协议关联商铺 VO（后端 InvIntentionShop 对应） */
export interface IntentionShopVO {
  id: number
  intentionId: number
  shopId: number
  buildingId: number
  floorId: number
  formatType: string
  area: number
}

export interface IntentionFeeStageItemDTO {
  intentionFeeId: number
  shopId?: number
  stageStart: string
  stageEnd: string
  unitPrice?: number
  commissionRate?: number
  minCommissionAmount?: number
}

export interface ApprovalCallbackDTO {
  approved: boolean
  approvalId?: string
  comment?: string
}

// ───────────────── 查询结果类型 ─────────────────

export interface IntentionFeeVO {
  id: number
  intentionId: number
  feeItemId: number
  feeName: string
  chargeType: number
  unitPrice: number
  area: number
  startDate: string
  endDate: string
  amount: number
  formulaParams: Record<string, unknown>
}

export interface IntentionFeeStageVO {
  id: number
  intentionFeeId: number
  shopId: number
  stageStart: string
  stageEnd: string
  unitPrice: number
  commissionRate: number
  minCommissionAmount: number
  amount: number
}

export interface IntentionBillingVO {
  id: number
  intentionId: number
  intentionFeeId: number
  feeName: string
  billingStart: string
  billingEnd: string
  billingType: number   // 1首账期 0普通
  amount: number
}

export interface CostResultVO {
  totalAmount: number
  items: Array<{
    feeId: number
    feeName: string
    chargeType: number
    chargeTypeName: string
    amount: number
  }>
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

/** 审批回调（Mock用） */
export function approvalCallback(id: number, data: ApprovalCallbackDTO) {
  return request.post<void, void>(`/inv/intentions/${id}/approval-callback`, data)
}

// ─── 商铺关联 ───

/** 批量保存商铺关联（全量替换） */
export function saveShops(id: number, data: IntentionShopItemDTO[]) {
  return request.post<void, void>(`/inv/intentions/${id}/shops`, data)
}

/** 查询关联商铺列表 */
export function getShops(id: number) {
  return request.get<IntentionShopVO[], IntentionShopVO[]>(`/inv/intentions/${id}/shops`)
}

// ─── 费项配置 ───

/** 批量保存费项配置（全量替换） */
export function saveFees(id: number, data: IntentionFeeItemDTO[]) {
  return request.post<void, void>(`/inv/intentions/${id}/fees`, data)
}

/** 查询费项列表 */
export function getFees(id: number) {
  return request.get<IntentionFeeVO[], IntentionFeeVO[]>(`/inv/intentions/${id}/fees`)
}

// ─── 分铺计租阶段 ───

/** 批量保存分铺计租阶段（全量替换） */
export function saveFeeStages(id: number, data: IntentionFeeStageItemDTO[]) {
  return request.post<void, void>(`/inv/intentions/${id}/fee-stages`, data)
}

/** 查询分铺计租阶段列表 */
export function getFeeStages(id: number) {
  return request.get<IntentionFeeStageVO[], IntentionFeeStageVO[]>(`/inv/intentions/${id}/fee-stages`)
}

// ─── 费用与账期 ───

/** 生成费用明细（调用计算引擎，更新 total_amount） */
export function generateCost(id: number) {
  return request.post<CostResultVO, CostResultVO>(`/inv/intentions/${id}/generate-cost`)
}

/** 生成账期（全量替换） */
export function generateBilling(id: number) {
  return request.post<IntentionBillingVO[], IntentionBillingVO[]>(`/inv/intentions/${id}/billing`)
}

/** 查询账期列表 */
export function getBillingList(id: number) {
  return request.get<IntentionBillingVO[], IntentionBillingVO[]>(`/inv/intentions/${id}/billing`)
}
