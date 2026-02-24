import request from '@/api/request'
import type { PageResult } from '@/api/base/project'

// ───────────────── 主实体类型 ─────────────────

export interface RentDecompVO {
  id: number
  decompCode: string
  projectId: number
  policyId: number
  totalAnnualRent: number
  totalAnnualFee: number
  status: number    // 0草稿 1审批中 2通过 3驳回
  approvalId: string
  createdAt: string
}

export interface RentDecompQuery {
  pageNum?: number
  pageSize?: number
  projectId?: number
  status?: number
}

export interface RentDecompSaveDTO {
  projectId?: number
  policyId?: number
}

// ───────────────── 明细类型 ─────────────────

export interface RentDecompDetailVO {
  id?: number
  decompId?: number
  shopCategory: number    // 1主力 2次主力 3一般
  formatType?: string
  rentUnitPrice: number
  propertyUnitPrice: number
  area: number
  annualRent?: number
  annualFee?: number
  remark?: string
}

export interface CalculateResult {
  totalAnnualRent: number
  totalAnnualFee: number
  detailCount: number
}

// ───────────────── API ─────────────────

/** 分页查询 */
export function getRentDecompPage(params: RentDecompQuery) {
  return request.get<PageResult<RentDecompVO>, PageResult<RentDecompVO>>(
    '/inv/rent-decomps', { params })
}

/** 查询详情 */
export function getRentDecompDetail(id: number) {
  return request.get<RentDecompVO, RentDecompVO>(`/inv/rent-decomps/${id}`)
}

/** 新增 */
export function createRentDecomp(data: RentDecompSaveDTO) {
  return request.post<number, number>('/inv/rent-decomps', data)
}

/** 编辑 */
export function updateRentDecomp(id: number, data: RentDecompSaveDTO) {
  return request.put<void, void>(`/inv/rent-decomps/${id}`, data)
}

/** 删除 */
export function deleteRentDecomp(id: number) {
  return request.delete<void, void>(`/inv/rent-decomps/${id}`)
}

/** 查询明细列表 */
export function getRentDecompDetails(id: number) {
  return request.get<RentDecompDetailVO[], RentDecompDetailVO[]>(
    `/inv/rent-decomps/${id}/details`)
}

/** 批量保存明细（全量替换） */
export function saveRentDecompDetails(id: number, data: RentDecompDetailVO[]) {
  return request.post<void, void>(`/inv/rent-decomps/${id}/details`, data)
}

/** 自动汇总计算 */
export function calculateRentDecomp(id: number) {
  return request.post<CalculateResult, CalculateResult>(`/inv/rent-decomps/${id}/calculate`)
}
