import request from '@/api/request'

export interface SysOperLog {
  id: number
  module: string
  bizType: string
  method: string
  requestMethod: string
  requestUrl: string
  requestParam?: string
  responseResult?: string
  operUser: string
  operIp: string
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

export const logApi = {
  page: (params: OperLogQueryDTO) => request.get('/sys/logs', { params }),
  clearAll: () => request.delete('/sys/logs/clear'),
}
