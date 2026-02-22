import request from '@/api/request'

export interface MerchantContactVO {
  id: number
  merchantId: number
  contactName: string
  phone: string
  email: string
  position: string
  isPrimary: number
}

export interface MerchantInvoiceVO {
  id: number
  merchantId: number
  invoiceTitle: string
  taxNumber: string
  bankName: string
  bankAccount: string
  address: string
  phone: string
  isDefault: number
}

export interface MerchantVO {
  id: number
  projectId: number
  projectName: string
  merchantCode: string
  merchantName: string
  merchantAttr: number
  merchantAttrName: string
  merchantNature: number
  merchantNatureName: string
  formatType: string
  naturalPerson: string
  idCard: string
  address: string
  phone: string
  merchantLevel: number
  merchantLevelName: string
  auditStatus: number
  auditStatusName: string
  contacts: MerchantContactVO[]
  invoices: MerchantInvoiceVO[]
  createdAt: string
  updatedAt: string
}

export interface MerchantQuery {
  pageNum: number
  pageSize: number
  projectId?: number
  merchantName?: string
  merchantAttr?: number
  merchantNature?: number
  formatType?: string
  merchantLevel?: number
  auditStatus?: number
}

export interface MerchantSaveDTO {
  id?: number
  projectId: number
  merchantCode?: string
  merchantName: string
  merchantAttr?: number
  merchantNature?: number
  formatType?: string
  naturalPerson?: string
  idCard?: string
  address?: string
  phone?: string
  merchantLevel?: number
  auditStatus?: number
  contacts?: Array<{
    id?: number
    contactName?: string
    phone?: string
    email?: string
    position?: string
    isPrimary?: number
  }>
  invoices?: Array<{
    id?: number
    invoiceTitle?: string
    taxNumber?: string
    bankName?: string
    bankAccount?: string
    address?: string
    phone?: string
    isDefault?: number
  }>
}

/** 分页查询商家列表 */
export function getMerchantPage(params: MerchantQuery) {
  return request.get<any, any>('/base/merchants', { params })
}

/** 查询商家详情 */
export function getMerchantDetail(id: number) {
  return request.get<MerchantVO, MerchantVO>(`/base/merchants/${id}`)
}

/** 新增商家 */
export function createMerchant(data: MerchantSaveDTO) {
  return request.post<number, number>('/base/merchants', data)
}

/** 编辑商家 */
export function updateMerchant(id: number, data: MerchantSaveDTO) {
  return request.put<void, void>(`/base/merchants/${id}`, data)
}

/** 删除商家 */
export function deleteMerchant(id: number) {
  return request.delete<void, void>(`/base/merchants/${id}`)
}
