import request from '@/api/request'

/** 变量定义（JSON variables 字段中的元素） */
export interface AlgoVariable {
  key: string        // 变量标识，如 unit_price
  label: string      // 显示名称，如 租金单价
  unit?: string      // 单位，如 元/㎡/天
  defaultValue?: string
}

/** 租费算法 */
export interface SysFeeAlgorithm {
  id: number
  algoCode: string
  algoName: string
  algoType: number   // 1租金 2保证金 3服务费 4其他
  calcMode: number   // 1固定金额 2比率计算 3阶梯计算 4自定义公式
  formula: string
  variables?: AlgoVariable[]
  params?: Record<string, string | number>
  description?: string
  status: number
  createdAt?: string
}

export interface FeeAlgorithmCreateDTO {
  id?: number
  algoCode: string
  algoName: string
  algoType: number
  calcMode: number
  formula: string
  variables?: AlgoVariable[]
  params?: Record<string, string | number>
  description?: string
  status?: number
}

export interface CalcTestDTO {
  algoId: number
  inputs: Record<string, string>
}

export interface CalcTestResultVO {
  result: string
  expandedFormula: string
  detail: string
}

export const algorithmApi = {
  page: (params?: object) => request.get('/sys/fee-algorithms', { params }),
  listEnabled: () => request.get('/sys/fee-algorithms/enabled'),
  create: (data: FeeAlgorithmCreateDTO) => request.post('/sys/fee-algorithms', data),
  update: (id: number, data: FeeAlgorithmCreateDTO) => request.put(`/sys/fee-algorithms/${id}`, data),
  delete: (id: number) => request.delete(`/sys/fee-algorithms/${id}`),
  changeStatus: (id: number, status: number) => request.put(`/sys/fee-algorithms/${id}/status`, { status }),
  testCalc: (data: CalcTestDTO) => request.post('/sys/fee-algorithms/test-calc', data),
}
