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

// ─────────────────────────── 子资源类型定义 ───────────────────────────

export interface MerchantCreditVO {
  id: number
  merchantId: number
  recordType: number
  recordTypeName: string
  content: string
  recordDate: string
  attachmentUrl?: string
  createdAt: string
}

export interface AttachmentVO {
  id: number
  merchantId: number
  fileName: string
  fileUrl: string
  fileType?: string
  fileSize?: number
  uploadBy?: number
  createdAt: string
}

// ─────────────────────────── 商家联系人 ───────────────────────────

export interface MerchantContactDTO {
  contactName: string
  phone?: string
  email?: string
  position?: string
  isPrimary?: number
}

export function getMerchantContacts(id: number) {
  return request.get<MerchantContactVO[], MerchantContactVO[]>(`/base/merchants/${id}/contacts`)
}

export function addMerchantContact(id: number, data: MerchantContactDTO) {
  return request.post<number, number>(`/base/merchants/${id}/contacts`, data)
}

export function updateMerchantContact(id: number, cid: number, data: MerchantContactDTO) {
  return request.put<void, void>(`/base/merchants/${id}/contacts/${cid}`, data)
}

export function deleteMerchantContact(id: number, cid: number) {
  return request.delete<void, void>(`/base/merchants/${id}/contacts/${cid}`)
}

// ─────────────────────────── 商家诚信记录 ───────────────────────────

export interface CreditSaveDTO {
  recordType: number
  content: string
  recordDate: string
  attachmentUrl?: string
}

export function getMerchantCredits(id: number) {
  return request.get<MerchantCreditVO[], MerchantCreditVO[]>(`/base/merchants/${id}/credits`)
}

export function addMerchantCredit(id: number, data: CreditSaveDTO) {
  return request.post<number, number>(`/base/merchants/${id}/credits`, data)
}

export function deleteMerchantCredit(id: number, rid: number) {
  return request.delete<void, void>(`/base/merchants/${id}/credits/${rid}`)
}

// ─────────────────────────── 商家开票信息 ───────────────────────────

export interface InvoiceSaveDTO {
  invoiceTitle: string
  taxNumber?: string
  bankName?: string
  bankAccount?: string
  address?: string
  phone?: string
  isDefault?: number
}

export function getMerchantInvoices(id: number) {
  return request.get<MerchantInvoiceVO[], MerchantInvoiceVO[]>(`/base/merchants/${id}/invoices`)
}

export function addMerchantInvoice(id: number, data: InvoiceSaveDTO) {
  return request.post<number, number>(`/base/merchants/${id}/invoices`, data)
}

export function updateMerchantInvoice(id: number, iid: number, data: InvoiceSaveDTO) {
  return request.put<void, void>(`/base/merchants/${id}/invoices/${iid}`, data)
}

export function deleteMerchantInvoice(id: number, iid: number) {
  return request.delete<void, void>(`/base/merchants/${id}/invoices/${iid}`)
}

// ─────────────────────────── 商家附件 ───────────────────────────

export interface AttachmentSaveDTO {
  fileName: string
  fileUrl: string
  fileType?: string
  fileSize?: number
}

export function getMerchantAttachments(id: number) {
  return request.get<AttachmentVO[], AttachmentVO[]>(`/base/merchants/${id}/attachments`)
}

export function addMerchantAttachment(id: number, data: AttachmentSaveDTO) {
  return request.post<number, number>(`/base/merchants/${id}/attachments`, data)
}

export function deleteMerchantAttachment(id: number, aid: number) {
  return request.delete<void, void>(`/base/merchants/${id}/attachments/${aid}`)
}

/** 商家审核 */
export function auditMerchant(id: number, auditStatus: number) {
  return request.put<void, void>(`/base/merchants/${id}/audit`, { auditStatus })
}

// ─────────────────────────── Excel 导入 ───────────────────────────

export function importMerchants(file: File, projectId: number) {
  const fd = new FormData()
  fd.append('file', file)
  fd.append('projectId', String(projectId))
  return request.post<any, any>('/base/merchants/import', fd)
}

export function downloadMerchantTemplate() {
  return request.get('/base/merchants/template', { responseType: 'blob' })
}
