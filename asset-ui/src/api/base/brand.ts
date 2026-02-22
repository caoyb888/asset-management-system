import request from '@/api/request'

export interface BrandContactVO {
  id: number
  brandId: number
  contactName: string
  phone: string
  email: string
  position: string
  isPrimary: number
}

export interface BrandVO {
  id: number
  brandCode: string
  brandNameCn: string
  brandNameEn: string
  formatType: string
  brandLevel: number
  brandLevelName: string
  cooperationType: number
  cooperationTypeName: string
  businessNature: number
  businessNatureName: string
  chainType: number
  chainTypeName: string
  projectStage: string
  groupName: string
  hqAddress: string
  mainCities: string
  website: string
  phone: string
  brandType: number
  brandTypeName: string
  avgRent: number
  minCustomerPrice: number
  brandIntro: string
  contacts: BrandContactVO[]
  createdAt: string
  updatedAt: string
}

export interface BrandQuery {
  pageNum: number
  pageSize: number
  brandNameCn?: string
  formatType?: string
  brandLevel?: number
  cooperationType?: number
  businessNature?: number
  brandType?: number
}

export interface BrandSaveDTO {
  id?: number
  brandCode?: string
  brandNameCn: string
  brandNameEn?: string
  formatType?: string
  brandLevel?: number
  cooperationType?: number
  businessNature?: number
  chainType?: number
  projectStage?: string
  groupName?: string
  hqAddress?: string
  mainCities?: string
  website?: string
  phone?: string
  brandType?: number
  avgRent?: number
  minCustomerPrice?: number
  brandIntro?: string
  contacts?: Array<{
    id?: number
    contactName?: string
    phone?: string
    email?: string
    position?: string
    isPrimary?: number
  }>
}

/** 分页查询品牌列表 */
export function getBrandPage(params: BrandQuery) {
  return request.get<any, any>('/base/brands', { params })
}

/** 查询品牌详情 */
export function getBrandDetail(id: number) {
  return request.get<BrandVO, BrandVO>(`/base/brands/${id}`)
}

/** 新增品牌 */
export function createBrand(data: BrandSaveDTO) {
  return request.post<number, number>('/base/brands', data)
}

/** 编辑品牌 */
export function updateBrand(id: number, data: BrandSaveDTO) {
  return request.put<void, void>(`/base/brands/${id}`, data)
}

/** 删除品牌 */
export function deleteBrand(id: number) {
  return request.delete<void, void>(`/base/brands/${id}`)
}
