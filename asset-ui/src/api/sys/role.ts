import request from '@/api/request'

export interface SysRole {
  id: number
  roleName: string
  roleCode: string
  dataScope?: number
  dataScopeName?: string
  sortOrder?: number
  status: number
  statusName?: string
  remark?: string
}

export interface RoleDetailVO extends SysRole {
  menuIds?: number[]
  createdAt?: string
}

export interface RoleCreateDTO {
  id?: number
  roleName: string
  roleCode: string
  dataScope?: number
  sortOrder?: number
  status?: number
  remark?: string
  menuIds?: number[]
}

export const roleApi = {
  page: (params?: object) => request.get('/sys/roles', { params }),
  list: () => request.get('/sys/roles/list'),
  getById: (id: number) => request.get(`/sys/roles/${id}`),
  create: (data: RoleCreateDTO) => request.post('/sys/roles', data),
  update: (id: number, data: RoleCreateDTO) => request.put(`/sys/roles/${id}`, data),
  delete: (id: number) => request.delete(`/sys/roles/${id}`),
  changeStatus: (id: number, status: number) => request.put(`/sys/roles/${id}/status`, { status }),
  grantMenus: (id: number, menuIds: number[]) => request.put(`/sys/roles/${id}/menus`, { menuIds }),
}
