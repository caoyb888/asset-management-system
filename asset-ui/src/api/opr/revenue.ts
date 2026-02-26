import request from '@/api/request'

// ── 类型定义 ────────────────────────────────────────────────────

export interface RevenueReportQueryDTO {
  projectId?: number
  contractId?: number
  merchantId?: number
  shopId?: number
  reportMonth?: string
  reportDateFrom?: string
  reportDateTo?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface OprRevenueReport {
  id: number
  projectId: number
  contractId: number
  shopId?: number
  merchantId?: number
  reportDate: string
  reportMonth: string
  revenueAmount: number
  status: number        // 0待确认 1已确认
  createdAt?: string
}

export interface RevenueReportCreateDTO {
  contractId: number
  projectId?: number
  shopId?: number
  merchantId?: number
  reportDate: string    // YYYY-MM-DD
  revenueAmount: number
}

export interface OprFloatingRent {
  id: number
  contractId: number
  shopId?: number
  calcMonth: string
  monthlyRevenue: number
  fixedRent?: number
  commissionRate?: number
  commissionAmount?: number
  floatingRent: number
  calcFormula?: string
  receivableId?: number
}

export interface TierDetailVO {
  tierNo: number
  revenueFrom?: number
  revenueTo?: number
  rate: number
  tierAmount: number
}

export interface FloatingRentDetailVO extends OprFloatingRent {
  chargeType?: number
  chargeTypeName?: string
  tiers: TierDetailVO[]
}

export interface RevenueStatisticsVO {
  reportMonth: string
  totalRevenue: number
  reportedContractCount: number
  totalDays: number
  details: ContractMonthlyVO[]
}

export interface ContractMonthlyVO {
  contractId: number
  contractCode: string
  shopId?: number
  shopCode?: string
  merchantName?: string
  monthlyRevenue: number
  reportDays: number
  totalDays: number
  complete: boolean
}

export interface ImportResultVO {
  successCount: number
  errorList: string[]
}

// ── 营收填报 API ─────────────────────────────────────────────────

export const revenueReportApi = {
  /** 分页列表 */
  page: (params: RevenueReportQueryDTO) =>
    request.get<any>('/api/opr/revenue-reports', { params }),

  /** 新增单日营收 */
  create: (data: RevenueReportCreateDTO) =>
    request.post<OprRevenueReport>('/api/opr/revenue-reports', data),

  /** 修改（仅待确认） */
  update: (id: number, data: Partial<RevenueReportCreateDTO>) =>
    request.put('/api/opr/revenue-reports/' + id, data),

  /** 批量导入 Excel */
  importExcel: (file: File) => {
    const form = new FormData()
    form.append('file', file)
    return request.post<ImportResultVO>('/api/opr/revenue-reports/import', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  /** 导出 Excel（blob） */
  exportExcel: (params: RevenueReportQueryDTO) =>
    request.get('/api/opr/revenue-reports/export', { params, responseType: 'blob' }),

  /** 下载导入模板 */
  downloadTemplate: () =>
    request.get('/api/opr/revenue-reports/template', { responseType: 'blob' }),

  /** 查询指定合同月份每日明细（日历着色用） */
  dailyDetail: (contractId: number, reportMonth: string) =>
    request.get<Record<string, number>>('/api/opr/revenue-reports/daily-detail', {
      params: { contractId, reportMonth },
    }),

  /** 月度汇总统计 */
  statistics: (params: { reportMonth: string; projectId?: number; contractId?: number }) =>
    request.get<RevenueStatisticsVO>('/api/opr/revenue-reports/statistics', { params }),
}

// ── 浮动租金 API ─────────────────────────────────────────────────

export const floatingRentApi = {
  /** 触发浮动租金计算 */
  generate: (data: { contractId: number; calcMonth: string }) =>
    request.post<number>('/api/opr/revenue-reports/generate-floating-rent', data),

  /** 分页列表 */
  page: (params: { contractId?: number; calcMonth?: string; pageNum?: number; pageSize?: number }) =>
    request.get<any>('/api/opr/floating-rents', { params }),

  /** 详情（含阶梯） */
  detail: (id: number) =>
    request.get<FloatingRentDetailVO>('/api/opr/floating-rents/' + id),

  /** 手动生成应收 */
  generateReceivable: (id: number) =>
    request.post<number>('/api/opr/floating-rents/' + id + '/generate-receivable'),
}
