import request from '@/api/request'

/** 客流查询参数 */
export interface PassengerFlowQueryDTO {
  projectId?: number
  buildingId?: number
  floorId?: number
  startDate?: string
  endDate?: string
  sourceType?: number
  pageNum?: number
  pageSize?: number
}

/** 新增/编辑客流参数 */
export interface PassengerFlowCreateDTO {
  projectId: number
  buildingId?: number
  floorId?: number
  reportDate: string
  flowCount: number
  sourceType?: number
}

/** 客流记录 */
export interface OprPassengerFlow {
  id: number
  projectId: number
  buildingId?: number
  floorId?: number
  reportDate: string
  flowCount: number
  sourceType: number
}

/** 统计趋势点 */
export interface DailyPoint {
  date: string
  flowCount: number
}

/** 客流统计 VO */
export interface PassengerFlowStatisticsVO {
  todayFlow: number
  yesterdayFlow: number
  dayOverDayRate: number | null
  thisWeekFlow: number
  lastWeekFlow: number
  weekOverWeekRate: number | null
  last30DaysFlow: number
  trendPoints: DailyPoint[]
}

/** 分页查询客流列表 */
export function getFlowPage(params: PassengerFlowQueryDTO) {
  return request.get('/opr/passenger-flows', { params })
}

/** 新增客流 */
export function createFlow(data: PassengerFlowCreateDTO) {
  return request.post('/opr/passenger-flows', data)
}

/** 编辑客流 */
export function updateFlow(id: number, data: PassengerFlowCreateDTO) {
  return request.put(`/opr/passenger-flows/${id}`, data)
}

/** 删除客流 */
export function deleteFlow(id: number) {
  return request.delete(`/opr/passenger-flows/${id}`)
}

/** 导入 Excel */
export function importFlowExcel(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/opr/passenger-flows/import', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 导出 Excel */
export function exportFlowExcel(params: PassengerFlowQueryDTO) {
  return request.get('/opr/passenger-flows/export', {
    params,
    responseType: 'blob'
  })
}

/** 统计分析（日/周环比+趋势） */
export function getFlowStatistics(params: { projectId?: number; buildingId?: number; floorId?: number }) {
  return request.get('/opr/passenger-flows/statistics', { params })
}
