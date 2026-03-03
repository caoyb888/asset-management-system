import request from '@/api/request'

// ── 报表编码常量 ──────────────────────────────────────────────────────────────

export const REPORT_CODES = {
  // 财务类
  FIN_RECEIVABLE_SUMMARY: 'FIN_RECEIVABLE_SUMMARY',
  FIN_RECEIPT_SUMMARY: 'FIN_RECEIPT_SUMMARY',
  FIN_OUTSTANDING_SUMMARY: 'FIN_OUTSTANDING_SUMMARY',
  FIN_AGING_ANALYSIS: 'FIN_AGING_ANALYSIS',
  FIN_OVERDUE_RATE: 'FIN_OVERDUE_RATE',
  FIN_COLLECTION_RATE: 'FIN_COLLECTION_RATE',
  // 资产类
  ASSET_SHOP_RENTAL: 'ASSET_SHOP_RENTAL',
  ASSET_VACANCY_RATE: 'ASSET_VACANCY_RATE',
  ASSET_BRAND_DIST: 'ASSET_BRAND_DIST',
  // 营运类
  OPR_REVENUE_SUMMARY: 'OPR_REVENUE_SUMMARY',
  OPR_CONTRACT_CHANGES: 'OPR_CONTRACT_CHANGES',
  // 招商类
  INV_INTENTION_STATS: 'INV_INTENTION_STATS',
  INV_PERFORMANCE: 'INV_PERFORMANCE',
} as const

export type ReportCode = (typeof REPORT_CODES)[keyof typeof REPORT_CODES]

// ── 类型定义 ──────────────────────────────────────────────────────────────────

export interface ExportTaskDTO {
  reportCode: ReportCode | string
  format?: 'EXCEL' | 'PDF'
  params?: Record<string, any>
}

export interface ExportTaskStatusVO {
  logCode: string
  /** 0=失败, 1=成功, 2=进行中 */
  status: 0 | 1 | 2
  statusName: string
  fileName?: string
  dataCount?: number
  durationMs?: number
  errorMsg?: string
  downloadUrl?: string
}

export interface RptGenerationLog {
  id: number
  logCode: string
  reportId: number
  generationType: string
  triggeredBy: number
  fileFormat: string
  fileName: string
  filePath: string
  fileSize: number
  filterParams: string
  dataCount: number
  status: 0 | 1 | 2
  errorMsg?: string
  durationMs: number
  createdAt: string
  updatedAt: string
}

// ── API 方法 ──────────────────────────────────────────────────────────────────

/**
 * 提交导出任务
 * @returns logCode 任务流水号
 */
export function submitExport(dto: ExportTaskDTO): Promise<{ data: string }> {
  return request.post('/rpt/common/export', dto)
}

/**
 * 查询导出任务状态（前端轮询用）
 */
export function queryExportStatus(logCode: string): Promise<{ data: ExportTaskStatusVO }> {
  return request.get(`/rpt/common/export/${logCode}/status`)
}

/**
 * 获取文件下载 URL（status=1 时有效）
 */
export function getDownloadUrl(logCode: string): string {
  return `/api/rpt/common/export/${logCode}/download`
}

/**
 * 查询当前用户最近导出记录
 */
export function myExportLogs(): Promise<{ data: RptGenerationLog[] }> {
  return request.get('/rpt/common/export/my-logs')
}
