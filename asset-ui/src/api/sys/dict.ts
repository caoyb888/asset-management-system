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

  // 字典数据
  listData: (dictType: string) => request.get(`/sys/dict/data/${dictType}`),
  createData: (data: DictDataCreateDTO) => request.post('/sys/dict/data', data),
  updateData: (id: number, data: DictDataCreateDTO) => request.put(`/sys/dict/data/${id}`, data),
  deleteData: (id: number) => request.delete(`/sys/dict/data/${id}`),
}
