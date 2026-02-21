import request from '@/api/request'

export interface CompanyOption {
  id: number
  companyCode: string
  companyName: string
}

/** 获取启用状态的公司列表（用于下拉选择） */
export function getCompanyList() {
  return request.get<CompanyOption[], CompanyOption[]>('/base/companies/list')
}
