import request from '@/api/request'
import type { PageResult } from '@/api/base/project'

// ───────────────── 计租方案 ─────────────────

export interface RentSchemeVO {
  id: number
  schemeCode: string
  schemeName: string
  chargeType: number       // 1固定 2固定提成 3阶梯提成 4两者取高 5一次性
  paymentCycle: number     // 1月付 2季付 3半年付 4年付 5一次性
  billingMode: number      // 1预付 2当期 3后付
  formulaJson: Record<string, unknown>
  strategyBeanName: string
  status: number           // 0停用 1启用
  description: string
}

export interface RentSchemeSaveDTO {
  id?: number
  schemeCode: string
  schemeName: string
  chargeType: number
  paymentCycle: number
  billingMode: number
  formulaJson?: Record<string, unknown>
  strategyBeanName?: string
  status: number
  description?: string
}

/** 查询启用的计租方案列表（选择器用） */
export function getRentSchemeList() {
  return request.get<RentSchemeVO[], RentSchemeVO[]>('/inv/config/rent-schemes')
}

/** 查询全部计租方案（含停用，管理页用） */
export function getRentSchemeAllList() {
  return request.get<RentSchemeVO[], RentSchemeVO[]>('/inv/config/rent-schemes', { params: { showAll: true } })
}

/** 启用/停用计租方案 */
export function toggleRentSchemeStatus(id: number, status: 0 | 1) {
  return request.put<void, void>(`/inv/config/rent-schemes/${id}/status`, { status })
}

export function getRentSchemeDetail(id: number) {
  return request.get<RentSchemeVO, RentSchemeVO>(`/inv/config/rent-schemes/${id}`)
}

export function createRentScheme(data: RentSchemeSaveDTO) {
  return request.post<number, number>('/inv/config/rent-schemes', data)
}

export function updateRentScheme(id: number, data: RentSchemeSaveDTO) {
  return request.put<void, void>(`/inv/config/rent-schemes/${id}`, data)
}

export function deleteRentScheme(id: number) {
  return request.delete<void, void>(`/inv/config/rent-schemes/${id}`)
}

// ───────────────── 收款项目 ─────────────────

export interface FeeItemVO {
  id: number
  itemCode: string
  itemName: string
  itemType: number    // 1租金 2保证金 3其他
  isRequired: number  // 0否 1是
  sortOrder: number
  status: number      // 0停用 1启用
}

export interface FeeItemSaveDTO {
  id?: number
  itemCode: string
  itemName: string
  itemType: number
  isRequired: number
  sortOrder: number
  status: number
}

/** 查询启用的收款项目列表（选择器用） */
export function getFeeItemList() {
  return request.get<FeeItemVO[], FeeItemVO[]>('/inv/config/fee-items')
}

/** 查询全部收款项目（含停用，管理页用） */
export function getFeeItemAllList() {
  return request.get<FeeItemVO[], FeeItemVO[]>('/inv/config/fee-items', { params: { showAll: true } })
}

/** 启用/停用收款项目 */
export function toggleFeeItemStatus(id: number, status: 0 | 1) {
  return request.put<void, void>(`/inv/config/fee-items/${id}/status`, { status })
}

/** 批量更新排序 */
export function updateFeeItemSort(items: Array<{ id: number; sortOrder: number }>) {
  return request.put<void, void>('/inv/config/fee-items/sort', items)
}

export function createFeeItem(data: FeeItemSaveDTO) {
  return request.post<number, number>('/inv/config/fee-items', data)
}

export function updateFeeItem(id: number, data: FeeItemSaveDTO) {
  return request.put<void, void>(`/inv/config/fee-items/${id}`, data)
}

export function deleteFeeItem(id: number) {
  return request.delete<void, void>(`/inv/config/fee-items/${id}`)
}
