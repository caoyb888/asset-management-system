import request from '@/api/request'

// ─── 类型定义 ───────────────────────────────────────────────────────────────

export interface UserDetailVO {
  id: number
  username: string
  realName: string
  deptId?: number
  deptName?: string
  phone?: string
  email?: string
  avatar?: string
  status: number
  statusName: string
  loginIp?: string
  loginTime?: string
  createdAt?: string
  roleIds?: number[]
  roleNames?: string[]
  postIds?: number[]
  postNames?: string[]
}

export interface UserCreateDTO {
  id?: number
  username: string
  password?: string
  realName?: string
  deptId?: number
  phone?: string
  email?: string
  avatar?: string
  status?: number
  roleIds?: number[]
  postIds?: number[]
}

export interface UserQueryDTO {
  pageNum?: number
  pageSize?: number
  username?: string
  realName?: string
  phone?: string
  status?: number
  deptId?: number
  roleId?: number
}

export interface ResetPwdDTO {
  userId: number
  newPassword: string
}

// ─── API ────────────────────────────────────────────────────────────────────

export const userApi = {
  /** 分页查询用户列表 */
  page: (params: UserQueryDTO) => request.get('/sys/users', { params }),
  /** 用户详情 */
  getById: (id: number) => request.get(`/sys/users/${id}`),
  /** 新增用户 */
  create: (data: UserCreateDTO) => request.post('/sys/users', data),
  /** 更新用户 */
  update: (id: number, data: UserCreateDTO) => request.put(`/sys/users/${id}`, data),
  /** 删除用户 */
  delete: (id: number) => request.delete(`/sys/users/${id}`),
  /** 重置密码 */
  resetPassword: (data: ResetPwdDTO) => request.put('/sys/users/reset-password', data),
  /** 修改用户状态 */
  changeStatus: (id: number, status: number) => request.put(`/sys/users/${id}/status`, { status }),
}
