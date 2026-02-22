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
