import request from '@/api/request'
import type { PageResult } from './project'

// ─────────────────────────── 类型定义 ───────────────────────────

export interface ShopVO {
  id: number
  projectId: number
  projectName: string
  buildingId: number
  buildingName: string
  floorId: number
  floorName: string
  shopCode: string
  shopType: number
  shopTypeName: string
  rentArea: number
  measuredArea: number
  buildingArea: number
  operatingArea: number
  shopStatus: number
  shopStatusName: string
  countLeasingRate: number
  countRentalRate: number
  countOpeningRate: number
  signedFormat: string
  plannedFormat: string
  ownerName: string
  ownerContact: string
  ownerPhone: string
  createdAt: string
  updatedAt: string
}

export interface ShopQuery {
  pageNum?: number
  pageSize?: number
  projectId?: number | null
  buildingId?: number | null
  floorId?: number | null
  shopCode?: string
  shopStatus?: number | ''
  shopType?: number | ''
  signedFormat?: string
}

export interface ShopSaveDTO {
  id?: number
  projectId: number | null
  buildingId: number | null
  floorId: number | null
  shopCode: string
  shopType?: number | null
  rentArea?: number | null
  measuredArea?: number | null
  buildingArea?: number | null
  operatingArea?: number | null
  shopStatus?: number | null
  countLeasingRate?: number
  countRentalRate?: number
  countOpeningRate?: number
  signedFormat?: string
  plannedFormat?: string
  ownerName?: string
  ownerContact?: string
  ownerPhone?: string
}

// ─────────────────────────── API ───────────────────────────

/** 分页查询商铺列表 */
export function getShopPage(params: ShopQuery) {
  return request.get<PageResult<ShopVO>, PageResult<ShopVO>>(
    '/base/shops',
    { params },
  )
}

/** 查询商铺详情 */
export function getShopById(id: number) {
  return request.get<ShopVO, ShopVO>(`/base/shops/${id}`)
}

/** 新增商铺 */
export function createShop(data: ShopSaveDTO) {
  return request.post<number, number>('/base/shops', data)
}

/** 编辑商铺 */
export function updateShop(id: number, data: ShopSaveDTO) {
  return request.put<void, void>(`/base/shops/${id}`, data)
}

/** 删除商铺 */
export function deleteShop(id: number) {
  return request.delete<void, void>(`/base/shops/${id}`)
}
