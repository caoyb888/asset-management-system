import request from '@/api/request'

/** 系统参数配置 */
export interface SysConfig {
  id: number
  configKey: string
  configName: string
  configValue: string
  configGroup: string
  description?: string
  isBuiltIn: number
  createdAt?: string
  updatedAt?: string
}

export interface SysConfigCreateDTO {
  id?: number
  configKey: string
  configName: string
  configValue: string
  configGroup?: string
  description?: string
  isBuiltIn?: number
}

export const configApi = {
  page: (params?: object) => request.get('/sys/configs', { params }),
  listByGroup: (group: string) => request.get(`/sys/configs/group/${group}`),
  getByKey: (key: string) => request.get(`/sys/configs/key/${key}`),
  create: (data: SysConfigCreateDTO) => request.post('/sys/configs', data),
  update: (id: number, data: SysConfigCreateDTO) => request.put(`/sys/configs/${id}`, data),
  delete: (id: number) => request.delete(`/sys/configs/${id}`),
  refresh: () => request.post('/sys/configs/refresh'),
}
