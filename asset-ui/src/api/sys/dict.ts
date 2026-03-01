import request from '@/api/request'

export interface SysDictType {
  id: number
  dictName: string
  dictType: string
  status: number
  remark?: string
  createdAt?: string
}

export interface SysDictData {
  id: number
  dictType: string
  dictLabel: string
  dictValue: string
  cssClass?: string
  sortOrder?: number
  status: number
  remark?: string
}

export interface DictTypeCreateDTO {
  id?: number
  dictName: string
  dictType: string
  status?: number
  remark?: string
}

export interface DictDataCreateDTO {
  id?: number
  dictType: string
  dictLabel: string
  dictValue: string
  cssClass?: string
  sortOrder?: number
  status?: number
  remark?: string
}

export const dictApi = {
  // 字典类型
  pageType: (params?: object) => request.get('/sys/dict/types', { params }),
  createType: (data: DictTypeCreateDTO) => request.post('/sys/dict/types', data),
  updateType: (id: number, data: DictTypeCreateDTO) => request.put(`/sys/dict/types/${id}`, data),
  deleteType: (id: number) => request.delete(`/sys/dict/types/${id}`),
  changeStatusType: (id: number, status: number) => request.put(`/sys/dict/types/${id}/status`, { status }),

  // 字典数据
  listData: (dictType: string) => request.get(`/sys/dict/data/${dictType}`),
  listAllData: (dictType: string) => request.get(`/sys/dict/data/${dictType}/all`),
  createData: (data: DictDataCreateDTO) => request.post('/sys/dict/data', data),
  updateData: (id: number, data: DictDataCreateDTO) => request.put(`/sys/dict/data/${id}`, data),
  deleteData: (id: number) => request.delete(`/sys/dict/data/${id}`),
  changeStatusData: (id: number, status: number) => request.put(`/sys/dict/data/${id}/status`, { status }),

  // 缓存管理
  refreshCache: (dictType: string) => request.delete(`/sys/dict/cache/${dictType}`),
}
