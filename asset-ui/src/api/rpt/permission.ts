import request from '@/api/request'

export interface UserPermissionVO {
  userId: number
  username: string
  admin: boolean
  hasFinViewPerm: boolean
  accessibleModules: string[]   // 'ASSET' | 'INV' | 'OPR' | 'FIN'
  permittedProjectIds: number[] | null  // null = 管理员，可见全部
}

/**
 * 查询当前用户的报表权限信息
 * 用于初始化时控制菜单可见性和数据展示方式
 */
export function getUserPermissions() {
  return request.get<UserPermissionVO>('/rpt/common/user-permissions')
}
