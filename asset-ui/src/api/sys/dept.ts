import request from '@/api/request'

export interface DeptTreeVO {
  id: number
  parentId: number
  deptName: string
  deptCode?: string
  sortOrder?: number
  leader?: string
  status: number
  children?: DeptTreeVO[]
}

export interface DeptCreateDTO {
  id?: number
  parentId?: number
  deptName: string
  deptCode?: string
  sortOrder?: number
  leader?: string
  phone?: string
  email?: string
  status?: number
}

export const deptApi = {
  /** 部门树列表 */
  tree: (status?: number) => request.get('/sys/depts', { params: { status } }),
  /** 部门详情 */
  getById: (id: number) => request.get(`/sys/depts/${id}`),
  /** 新增部门 */
  create: (data: DeptCreateDTO) => request.post('/sys/depts', data),
  /** 更新部门 */
  update: (id: number, data: DeptCreateDTO) => request.put(`/sys/depts/${id}`, data),
  /** 删除部门 */
  delete: (id: number) => request.delete(`/sys/depts/${id}`),
  /** 修改状态 */
  changeStatus: (id: number, status: number) => request.put(`/sys/depts/${id}/status`, { status }),
  /** 移动子树（变更上级） */
  move: (id: number, targetParentId: number) => request.put(`/sys/depts/${id}/move`, { targetParentId }),
  /** 查询机构下用户 */
  getUsers: (id: number, includeChildren?: boolean) =>
    request.get(`/sys/depts/${id}/users`, { params: { includeChildren: includeChildren ?? false } }),
  /** 清除缓存 */
  clearCache: () => request.delete('/sys/depts/cache'),
}
