import request from '@/api/request'

export interface ExtFieldDef {
  id: number
  moduleCode: string
  fieldKey: string
  fieldLabel: string
  fieldType: 'text' | 'textarea' | 'number' | 'date' | 'select' | 'radio' | 'checkbox'
  optionsJson?: { label: string; value: string }[]
  required: boolean
  sortOrder: number
  showInList: boolean
  showInForm: boolean
  defaultVal?: string
  placeholder?: string
  maxLength?: number
  minVal?: number
  maxVal?: number
  createdAt?: string
  updatedAt?: string
}

export interface ExtFieldCreateDTO {
  moduleCode?: string
  fieldKey?: string
  fieldLabel?: string
  fieldType?: string
  optionsJson?: { label: string; value: string }[]
  required?: boolean
  sortOrder?: number
  showInList?: boolean
  showInForm?: boolean
  defaultVal?: string
  placeholder?: string
  maxLength?: number
  minVal?: number
  maxVal?: number
}

export interface ExtFieldSortItem {
  id: number
  sortOrder: number
}

export const extFieldApi = {
  /** 查询模块字段定义列表 */
  list: (moduleCode: string) =>
    request.get<any, ExtFieldDef[]>('/sys/ext-fields', { params: { moduleCode } }),

  /** 查询单个字段详情 */
  getById: (id: number) =>
    request.get<any, ExtFieldDef>(`/sys/ext-fields/${id}`),

  /** 新增扩展字段 */
  create: (data: ExtFieldCreateDTO) =>
    request.post<any, number>('/sys/ext-fields', data),

  /** 修改扩展字段（fieldKey 不可修改） */
  update: (id: number, data: ExtFieldCreateDTO) =>
    request.put(`/sys/ext-fields/${id}`, data),

  /** 删除扩展字段 */
  delete: (id: number) =>
    request.delete(`/sys/ext-fields/${id}`),

  /** 批量更新排序 */
  updateSort: (items: ExtFieldSortItem[]) =>
    request.put('/sys/ext-fields/sort', items),
}
