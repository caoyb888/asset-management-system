import request from '@/api/request'

export interface SysCodeRule {
  id: number
  ruleKey: string
  ruleName: string
  prefix: string
  dateFormat: string
  sep: string
  seqLength: number
  resetType: number
  currentSeq: number
  currentPeriod: string
  status: number
  remark?: string
  createdAt?: string
}

export interface CodeRuleCreateDTO {
  id?: number
  ruleKey: string
  ruleName: string
  prefix?: string
  dateFormat?: string
  sep?: string
  seqLength: number
  resetType: number
  status?: number
  remark?: string
}

export const codeRuleApi = {
  page: (params?: object) => request.get('/sys/code-rules', { params }),
  create: (data: CodeRuleCreateDTO) => request.post('/sys/code-rules', data),
  update: (id: number, data: CodeRuleCreateDTO) => request.put(`/sys/code-rules/${id}`, data),
  delete: (id: number) => request.delete(`/sys/code-rules/${id}`),
  changeStatus: (id: number, status: number) => request.put(`/sys/code-rules/${id}/status`, { status }),
  resetSeq: (id: number) => request.put(`/sys/code-rules/${id}/reset-seq`),
  generate: (ruleKey: string) => request.get(`/sys/code-rules/generate/${ruleKey}`),
}
