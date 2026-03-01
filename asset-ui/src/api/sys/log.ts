import request from '@/api/request'

/** 操作日志 */
export interface SysOperLog {
  id: number
  module: string
  bizType: string
  action?: string
  method: string
  requestMethod: string
  requestUrl: string
  requestParam?: string
  operUser: string
  operIp: string
  /** 0=成功 1=失败 */
  status: number
  errorMsg?: string
  costTime: number
  operTime: string
}

export interface OperLogQueryDTO {
  pageNum?: number
  pageSize?: number
  module?: string
  operUser?: string
  status?: number
  timeFrom?: string
  timeTo?: string
}

/** 登录日志 */
export interface SysLoginLog {
  id: number
  username: string
  ipAddr: string
  browser?: string
  os?: string
  /** 0=成功 1=失败 */
  status: number
  msg?: string
  loginTime: string
}

export interface LoginLogQueryDTO {
  pageNum?: number
  pageSize?: number
  username?: string
  ipAddr?: string
  status?: number
  timeFrom?: string
  timeTo?: string
}

export const logApi = {
  page: (params: OperLogQueryDTO) => request.get('/sys/logs', { params }),
  clearAll: () => request.delete('/sys/logs/clear'),
}

export const loginLogApi = {
  page: (params: LoginLogQueryDTO) => request.get('/sys/login-logs', { params }),
  clearAll: () => request.delete('/sys/login-logs/clear'),
}
