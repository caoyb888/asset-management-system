import request from '@/api/request'

// ─────────────────────────── DTO / VO ───────────────────────────

export interface FavoriteAddDTO {
  reportCode: string
  reportName: string
  routePath: string
  /** 1=资产 2=招商 3=营运 4=财务 */
  category: 1 | 2 | 3 | 4
  quickAccess?: boolean
}

export interface FavoriteVO {
  id: number
  reportCode: string
  reportName: string
  routePath: string
  category: 1 | 2 | 3 | 4
  sortOrder: number
  quickAccess: boolean
  createdAt: string
}

// ─────────────────────────── API ───────────────────────────

/** 获取当前用户的收藏列表 */
export function listFavorites() {
  return request.get<FavoriteVO[]>('/rpt/common/favorites')
}

/** 收藏报表（重复收藏返回已有 ID） */
export function addFavorite(dto: FavoriteAddDTO) {
  return request.post<number>('/rpt/common/favorites', dto)
}

/** 取消收藏 */
export function removeFavorite(id: number) {
  return request.delete<void>(`/rpt/common/favorites/${id}`)
}

/** 拖拽排序（提交新顺序的 id 列表） */
export function updateFavoriteSort(ids: number[]) {
  return request.put<void>('/rpt/common/favorites/sort', { ids })
}
