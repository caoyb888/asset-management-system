import request from '@/api/request'

export interface UserOption {
  id: number
  username: string
  realName: string
}

/** 获取启用状态的用户列表（用于下拉选择） */
export function getUserList() {
  return request.get<UserOption[], UserOption[]>('/base/users/list')
}
