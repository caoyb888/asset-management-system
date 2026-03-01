import request from '@/api/request'

export interface MenuTreeVO {
  id: number
  parentId: number
  menuName: string
  menuType: string
  path?: string
  component?: string
  perms?: string
  icon?: string
  sortOrder?: number
  visible?: number
  status?: number
  remark?: string
  children?: MenuTreeVO[]
}

export interface MenuCreateDTO {
  id?: number
  parentId?: number
  menuName: string
  menuType: string
  path?: string
  component?: string
  perms?: string
  icon?: string
  sortOrder?: number
  visible?: number
  status?: number
  remark?: string
}

export const menuApi = {
  /** 菜单树（全量） */
  tree: () => request.get('/sys/menus'),
  /** 菜单详情 */
  getById: (id: number) => request.get(`/sys/menus/${id}`),
  /** 用户路由树 */
  routes: (userId: number) => request.get('/sys/menus/routes', { params: { userId } }),
  create: (data: MenuCreateDTO) => request.post('/sys/menus', data),
  update: (id: number, data: MenuCreateDTO) => request.put(`/sys/menus/${id}`, data),
  delete: (id: number) => request.delete(`/sys/menus/${id}`),
  changeStatus: (id: number, status: number) => request.put(`/sys/menus/${id}/status`, { status }),
  changeVisible: (id: number, visible: number) => request.put(`/sys/menus/${id}/visible`, { visible }),
}
