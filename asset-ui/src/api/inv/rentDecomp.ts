import request from '@/api/request'
import type { PageResult } from '@/api/base/project'

export interface RentDecompVO {
  id: number
  decompositionCode: string
  projectId: number
  projectName: string
  businessYear: string
  policyId: number
  policyName: string
  totalAnnualRent: number
  remark: string
  createdAt: string
}

export interface RentDecompQuery {
  page?: number
  size?: number
  projectName?: string
  businessYear?: string
  policyId?: number
}

export interface RentDecompSaveDTO {
  id?: number
  policyId: number
  businessYear: string
  remark?: string
}

export function getRentDecompPage(params: RentDecompQuery) {
  return request.get<PageResult<RentDecompVO>, PageResult<RentDecompVO>>('/inv/rent-decomps', { params })
}

export function getRentDecompDetail(id: number) {
  return request.get<RentDecompVO, RentDecompVO>(`/inv/rent-decomps/${id}`)
}

export function createRentDecomp(data: RentDecompSaveDTO) {
  return request.post<number, number>('/inv/rent-decomps', data)
}

export function updateRentDecomp(id: number, data: RentDecompSaveDTO) {
  return request.put<void, void>(`/inv/rent-decomps/${id}`, data)
}

export function deleteRentDecomp(id: number) {
  return request.delete<void, void>(`/inv/rent-decomps/${id}`)
}

/** 自动计算汇总 */
export function calculateRentDecomp(id: number) {
  return request.post<{ totalAnnualRent: number }, { totalAnnualRent: number }>(`/inv/rent-decomps/${id}/calculate`)
}

/** 导出 Excel */
export function exportRentDecomp(id: number) {
  return request.get<Blob, Blob>(`/inv/rent-decomps/${id}/export`, { responseType: 'blob' })
}
