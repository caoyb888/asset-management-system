import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 合同台账 Pinia Store
 * 阶段一补充：缓存台账列表查询条件、当前台账详情、应收计划列表
 */
export const useLedgerStore = defineStore('opr-ledger', () => {
  /** 当前台账详情（详情页使用，避免重复请求） */
  const currentLedger = ref<Record<string, unknown> | null>(null)

  /** 列表查询条件（用于分页查询后恢复筛选状态） */
  const queryParams = ref({
    projectId: undefined as number | undefined,
    contractType: undefined as number | undefined,
    status: undefined as number | undefined,
    pageNum: 1,
    pageSize: 20,
  })

  function resetQuery() {
    queryParams.value = { projectId: undefined, contractType: undefined, status: undefined, pageNum: 1, pageSize: 20 }
  }

  function setCurrentLedger(ledger: Record<string, unknown> | null) {
    currentLedger.value = ledger
  }

  return { currentLedger, queryParams, resetQuery, setCurrentLedger }
})
