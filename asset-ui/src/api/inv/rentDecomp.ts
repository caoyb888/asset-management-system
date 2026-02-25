import request from '@/api/request'
import type { PageResult } from '@/api/base/project'

// ───────────────── 主实体类型 ─────────────────

export interface RentDecompVO {
  id: number
  decompCode: string
  projectId: number
  projectName?: string      // 后端 JOIN 补充
  policyId: number
  policyName?: string       // 后端 JOIN 补充（policy_code）
  businessYear?: string     // 业务年度（从 created_at 提取）
  totalAnnualRent: number
  totalAnnualFee: number
  status: number    // 0草稿 1审批中 2通过 3驳回
  approvalId: string
  createdAt: string
  policySnapshot?: Record<string, unknown>
}

export interface RentDecompQuery {
  page?: number
  size?: number
  pageNum?: number
  pageSize?: number
  projectId?: number
  projectName?: string
  businessYear?: string
  status?: number
}

export interface RentDecompSaveDTO {
  projectId?: number
  policyId?: number
}

// ───────────────── 明细类型 ─────────────────

export interface RentDecompDetailVO {
  id?: number
  decompId?: number
  shopCategory: number    // 1主力 2次主力 3一般
  formatType?: string
  rentUnitPrice: number
  propertyUnitPrice: number
  area: number
  annualRent?: number
  annualFee?: number
  remark?: string
}

export interface CalculateResult {
  totalAnnualRent: number
  totalAnnualFee: number
  detailCount: number
}

// ───────────────── API ─────────────────

/** 分页查询 */
export function getRentDecompPage(params: RentDecompQuery) {
  return request.get<PageResult<RentDecompVO>, PageResult<RentDecompVO>>(
    '/inv/rent-decomps', { params })
}

/** 查询详情 */
export function getRentDecompDetail(id: number) {
  return request.get<RentDecompVO, RentDecompVO>(`/inv/rent-decomps/${id}`)
}

/** 新增 */
export function createRentDecomp(data: RentDecompSaveDTO) {
  return request.post<number, number>('/inv/rent-decomps', data)
}

/** 编辑 */
export function updateRentDecomp(id: number, data: RentDecompSaveDTO) {
  return request.put<void, void>(`/inv/rent-decomps/${id}`, data)
}

/** 删除 */
export function deleteRentDecomp(id: number) {
  return request.delete<void, void>(`/inv/rent-decomps/${id}`)
}

/** 查询明细列表 */
export function getRentDecompDetails(id: number) {
  return request.get<RentDecompDetailVO[], RentDecompDetailVO[]>(
    `/inv/rent-decomps/${id}/details`)
}

/** 批量保存明细（全量替换） */
export function saveRentDecompDetails(id: number, data: RentDecompDetailVO[]) {
  return request.post<void, void>(`/inv/rent-decomps/${id}/details`, data)
}

/** 自动汇总计算 */
export function calculateRentDecomp(id: number) {
  return request.post<CalculateResult, CalculateResult>(`/inv/rent-decomps/${id}/calculate`)
}

/** Excel 导入明细（覆盖写入） */
export function importRentDecompDetails(id: number, file: File) {
  const fd = new FormData()
  fd.append('file', file)
  return request.post<{ successCount: number; errorCount: number; errors: string[] },
    { successCount: number; errorCount: number; errors: string[] }>(
    `/inv/rent-decomps/${id}/import`, fd,
    { headers: { 'Content-Type': 'multipart/form-data' } }
  )
}

/** 导出明细 Excel（触发浏览器下载） */
export function exportRentDecompDetails(id: number, decompCode: string) {
  return request.get(`/inv/rent-decomps/${id}/export`, {
    responseType: 'blob',
  }).then((blob: unknown) => {
    const url = URL.createObjectURL(blob as Blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${decompCode}_明细.xlsx`
    a.click()
    URL.revokeObjectURL(url)
  })
}

/** 重新关联租决政策（人工触发，更新快照） */
export function reLinkRentDecompPolicy(id: number, policyId: number) {
  return request.put<void, void>(`/inv/rent-decomps/${id}/re-link-policy`, { policyId })
}

/** 提交审批（草稿/驳回 → 审批中） */
export function submitRentDecompApproval(id: number) {
  return request.post<void, void>(`/inv/rent-decomps/${id}/submit-approval`)
}

/** 审批回调（Mock 测试用） */
export function rentDecompApprovalCallback(id: number, approved: boolean) {
  return request.post<void, void>(`/inv/rent-decomps/${id}/approval-callback`, { approved })
}
