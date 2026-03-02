import request from '@/api/request'
import type { PageResult } from './project'

// ─────────────────────────── 类型定义 ───────────────────────────

export interface BuildingVO {
  id: number
  projectId: number
  projectName: string
  buildingCode: string
  buildingName: string
  status: number
  statusName: string
  buildingArea: number
  operatingArea: number
  aboveFloors: number
  belowFloors: number
  imageUrl: string
  createdAt: string
  updatedAt: string
}

export interface BuildingQuery {
  pageNum?: number
  pageSize?: number
  projectId?: number | null
  buildingName?: string
  buildingCode?: string
  status?: number | ''
}

export interface BuildingSaveDTO {
  id?: number
  projectId: number | null
  buildingCode?: string
  buildingName: string
  status?: number | null
  buildingArea?: number | null
  operatingArea?: number | null
  aboveFloors?: number | null
  belowFloors?: number | null
  imageUrl?: string
}

// ─────────────────────────── API ───────────────────────────

/** 获取楼栋下拉列表（按项目过滤，内部复用分页接口取 200 条） */
export function getBuildingList(projectId?: number | null) {
  return getBuildingPage({ projectId: projectId ?? undefined, pageSize: 200 })
    .then(result => result.records.map(b => ({ id: b.id, buildingName: b.buildingName })))
}

/** 分页查询楼栋列表 */
export function getBuildingPage(params: BuildingQuery) {
  return request.get<PageResult<BuildingVO>, PageResult<BuildingVO>>(
    '/base/buildings',
    { params },
  )
}

/** 查询楼栋详情 */
export function getBuildingById(id: number) {
  return request.get<BuildingVO, BuildingVO>(`/base/buildings/${id}`)
}

/** 新增楼栋 */
export function createBuilding(data: BuildingSaveDTO) {
  return request.post<number, number>('/base/buildings', data)
}

/** 编辑楼栋 */
export function updateBuilding(id: number, data: BuildingSaveDTO) {
  return request.put<void, void>(`/base/buildings/${id}`, data)
}

/** 删除楼栋 */
export function deleteBuilding(id: number) {
  return request.delete<void, void>(`/base/buildings/${id}`)
}
