import request from '@/api/request'
import type { PageResult } from './project'

// ─────────────────────────── 类型定义 ───────────────────────────

export interface FloorVO {
  id: number
  projectId: number
  buildingId: number
  buildingName: string
  floorCode: string
  floorName: string
  status: number
  statusName: string
  buildingArea: number
  operatingArea: number
  remark: string
  imageUrl: string
  createdAt: string
  updatedAt: string
}

export interface FloorQuery {
  pageNum?: number
  pageSize?: number
  projectId?: number | null
  buildingId?: number | null
  floorName?: string
  floorCode?: string
  status?: number | ''
}

export interface FloorSaveDTO {
  id?: number
  projectId: number | null
  buildingId: number | null
  floorCode?: string
  floorName: string
  status?: number | null
  buildingArea?: number | null
  operatingArea?: number | null
  remark?: string
  imageUrl?: string
}

// ─────────────────────────── API ───────────────────────────

/** 分页查询楼层列表 */
export function getFloorPage(params: FloorQuery) {
  return request.get<PageResult<FloorVO>, PageResult<FloorVO>>(
    '/base/floors',
    { params },
  )
}

/** 查询楼层详情 */
export function getFloorById(id: number) {
  return request.get<FloorVO, FloorVO>(`/base/floors/${id}`)
}

/** 新增楼层 */
export function createFloor(data: FloorSaveDTO) {
  return request.post<number, number>('/base/floors', data)
}

/** 编辑楼层 */
export function updateFloor(id: number, data: FloorSaveDTO) {
  return request.put<void, void>(`/base/floors/${id}`, data)
}

/** 删除楼层 */
export function deleteFloor(id: number) {
  return request.delete<void, void>(`/base/floors/${id}`)
}
