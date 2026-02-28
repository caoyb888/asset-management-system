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

// ─────────────────────────── 拆分/合并 ───────────────────────────

export interface ShopSplitDTO {
  /** 原商铺ID */
  sourceShopId: number
  /** 拆分后的子商铺列表 */
  subShops: Array<{
    shopCode: string
    rentArea?: number | null
    buildingArea?: number | null
    operatingArea?: number | null
    plannedFormat?: string
  }>
}

export interface ShopMergeDTO {
  /** 待合并的商铺ID列表（至少2个） */
  shopIds: number[]
  /** 合并后的目标商铺编号 */
  targetShopCode: string
  /** 合并后的业态 */
  targetFormat?: string
}

/** 拆分商铺 */
export function splitShop(data: ShopSplitDTO) {
  return request.post<void, void>('/base/shops/split', data)
}

/** 合并商铺 */
export function mergeShop(data: ShopMergeDTO) {
  return request.post<void, void>('/base/shops/merge', data)
}

// ─────────────────────────── Excel 导入 ───────────────────────────

/** 批量导入商铺（Excel） */
export function importShops(file: File) {
  const fd = new FormData()
  fd.append('file', file)
  return request.post<{ successCount: number; failCount: number; errors: string[] }, any>('/base/shops/import', fd)
}

/** 下载商铺导入模板 */
export function downloadShopTemplate() {
  return request.get('/base/shops/template', { responseType: 'blob' })
}
