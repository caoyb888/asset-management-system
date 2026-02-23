import request from '@/api/request'

// ─────────────────────────── 类型定义 ───────────────────────────

export interface ImageUrl {
  url: string
  name: string
  sort: number
}

export interface ProjectVO {
  id: number
  projectCode: string
  projectName: string
  companyId: number
  companyName: string
  province: string
  city: string
  address: string
  propertyType: number
  propertyTypeName: string
  businessType: number
  businessTypeName: string
  buildingArea: number
  operatingArea: number
  operationStatus: number
  operationStatusName: string
  openingDate: string
  managerId: number
  managerName: string
  imageUrls: ImageUrl[]
  createdAt: string
  updatedAt: string
}

export interface ProjectQuery {
  pageNum?: number
  pageSize?: number
  projectName?: string
  projectCode?: string
  operationStatus?: number | ''
  province?: string
  city?: string
}

export interface ProjectSaveDTO {
  id?: number
  projectCode: string
  projectName: string
  companyId: number | null
  province?: string
  city?: string
  address?: string
  propertyType?: number | null
  businessType?: number | null
  buildingArea?: number | null
  operatingArea?: number | null
  operationStatus?: number | null
  openingDate?: string
  managerId?: number | null
  imageUrls?: ImageUrl[]
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// ─────────────────────────── API ───────────────────────────

/** 获取项目下拉列表（用于下拉选择） */
export function getProjectList() {
  return request.get<Array<{ id: number; projectName: string; projectCode: string }>, Array<{ id: number; projectName: string; projectCode: string }>>('/base/projects/list')
}

/** 分页查询项目列表 */
export function getProjectPage(params: ProjectQuery) {
  return request.get<PageResult<ProjectVO>, PageResult<ProjectVO>>(
    '/base/projects',
    { params },
  )
}

/** 查询项目详情 */
export function getProjectById(id: number) {
  return request.get<ProjectVO, ProjectVO>(`/base/projects/${id}`)
}

/** 新增项目 */
export function createProject(data: ProjectSaveDTO) {
  return request.post<number, number>('/base/projects', data)
}

/** 编辑项目 */
export function updateProject(id: number, data: ProjectSaveDTO) {
  return request.put<void, void>(`/base/projects/${id}`, data)
}

/** 删除项目 */
export function deleteProject(id: number) {
  return request.delete<void, void>(`/base/projects/${id}`)
}

// ─────────────────────────── 合同甲方 ───────────────────────────

export interface ProjectContractVO {
  id?: number
  partyAName?: string
  partyAAbbr?: string
  partyAAddress?: string
  partyAPhone?: string
  businessLicense?: string
  legalRepresentative?: string
  email?: string
}

export function getProjectContract(id: number) {
  return request.get<ProjectContractVO, ProjectContractVO>(`/base/projects/${id}/contracts`)
}

export function saveProjectContract(id: number, data: ProjectContractVO) {
  return request.put<void, void>(`/base/projects/${id}/contracts`, data)
}

// ─────────────────────────── 财务联系人 ───────────────────────────

export interface ProjectFinanceContactVO {
  id: number
  contactName: string
  phone?: string
  email?: string
  creditCode?: string
  sealType?: string
  sealDesc?: string
}

export function getProjectFinanceContacts(id: number) {
  return request.get<ProjectFinanceContactVO[], ProjectFinanceContactVO[]>(`/base/projects/${id}/finance-contacts`)
}

export function addProjectFinanceContact(id: number, data: Omit<ProjectFinanceContactVO, 'id'>) {
  return request.post<number, number>(`/base/projects/${id}/finance-contacts`, data)
}

export function updateProjectFinanceContact(id: number, cid: number, data: Omit<ProjectFinanceContactVO, 'id'>) {
  return request.put<void, void>(`/base/projects/${id}/finance-contacts/${cid}`, data)
}

export function deleteProjectFinanceContact(id: number, cid: number) {
  return request.delete<void, void>(`/base/projects/${id}/finance-contacts/${cid}`)
}

// ─────────────────────────── 银行账号 ───────────────────────────

export interface ProjectBankVO {
  id: number
  bankName: string
  bankAccount: string
  accountName: string
  isDefault: number
}

export function getProjectBanks(id: number) {
  return request.get<ProjectBankVO[], ProjectBankVO[]>(`/base/projects/${id}/banks`)
}

export function addProjectBank(id: number, data: Omit<ProjectBankVO, 'id'>) {
  return request.post<number, number>(`/base/projects/${id}/banks`, data)
}

export function updateProjectBank(id: number, bid: number, data: Omit<ProjectBankVO, 'id'>) {
  return request.put<void, void>(`/base/projects/${id}/banks/${bid}`, data)
}

export function deleteProjectBank(id: number, bid: number) {
  return request.delete<void, void>(`/base/projects/${id}/banks/${bid}`)
}
