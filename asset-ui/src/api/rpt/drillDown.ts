import request from '@/api/request'

// ── 类型定义 ──────────────────────────────────────────────────────────────────

export interface DrillDownRequestDTO {
  reportCode: string
  /** 当前层级（1=项目, 2=楼栋/费项, 3=楼层, 4=商铺） */
  fromLevel: number
  /** 当前层级维度ID（项目ID/楼栋ID/楼层ID） */
  dimensionId: number | null
  /** 统计日期（资产域，yyyy-MM-dd） */
  statDate?: string
  /** 起始月份（财务域，yyyy-MM） */
  startMonth?: string
  /** 结束月份（财务域，yyyy-MM） */
  endMonth?: string
  /** 费项类型（财务域费项→明细层时传） */
  feeItemType?: string
  /** 扩展参数 */
  extra?: Record<string, any>
}

export interface DrillColumnVO {
  prop: string
  label: string
  drillable: boolean
  /** 下钻时从行数据中取 dimensionId 的字段名 */
  drillIdField?: string
  align: 'left' | 'center' | 'right'
  width: number
}

export interface DrillDownResultVO {
  currentLevel: number
  levelName: string
  nextLevelName: string | null
  canDrillDown: boolean
  parentId: number | null
  parentName: string
  columns: DrillColumnVO[]
  rows: Record<string, any>[]
  total: number
}

/** 面包屑节点 */
export interface DrillCrumb {
  level: number
  levelName: string
  dimensionId: number | null
  label: string
  /** 快照请求参数（用于回退时复现数据） */
  requestSnapshot: DrillDownRequestDTO
  /** 快照结果（用于回退时恢复，不重新请求） */
  resultSnapshot?: DrillDownResultVO
}

// ── API 方法 ──────────────────────────────────────────────────────────────────

export function drillDown(dto: DrillDownRequestDTO): Promise<{ data: DrillDownResultVO }> {
  return request.post('/rpt/common/drill-down', dto)
}
