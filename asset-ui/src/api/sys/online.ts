import request from '@/api/request'

/** 在线用户信息 */
export interface OnlineUserVO {
  userId: number
  username: string
  loginIp: string
  /** ISO 时间字符串 */
  loginTime: string
}

export const onlineApi = {
  /** 查询在线用户列表 */
  list: () => request.get<OnlineUserVO[]>('/sys/online-users'),

  /** 强制下线指定用户 */
  kick: (userId: number) => request.delete(`/sys/online-users/${userId}`),
}
