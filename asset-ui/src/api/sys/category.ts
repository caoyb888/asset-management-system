import request from '@/api/request'

export interface CategoryTreeVO {
  id: number
  categoryType: string
  parentId: number
  categoryCode: string
  categoryName: string
  level: number
  sortOrder: number
  status: number
  remark?: string
  children?: CategoryTreeVO[]
}

export interface CategoryCreateDTO {
  id?: number
  categoryType: string
  parentId?: number
  categoryCode: string
  categoryName: string
  sortOrder?: number
  status?: number
  remark?: string
}

export const categoryApi = {
  tree: (params?: object) => request.get('/sys/categories/tree', { params }),
  listTypes: () => request.get('/sys/categories/types'),
  create: (data: CategoryCreateDTO) => request.post('/sys/categories', data),
  update: (id: number, data: CategoryCreateDTO) => request.put(`/sys/categories/${id}`, data),
  delete: (id: number) => request.delete(`/sys/categories/${id}`),
  changeStatus: (id: number, status: number) => request.put(`/sys/categories/${id}/status`, { status }),
}
