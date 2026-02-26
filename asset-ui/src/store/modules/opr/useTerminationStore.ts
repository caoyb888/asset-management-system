import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 合同解约 Pinia Store
 * 阶段五补充：缓存解约表单三步数据（合同选择→解约信息→清算确认）
 */
export const useTerminationStore = defineStore('opr-termination', () => {
  /** 当前表单步骤（0-2） */
  const currentStep = ref(0)

  /** 步骤1：已选合同信息 */
  const selectedContract = ref<Record<string, unknown> | null>(null)

  /** 步骤2：解约基本信息（类型/日期/原因） */
  const terminationInfo = ref<{
    terminationType?: number
    terminationDate?: string
    reason?: string
  }>({})

  /** 步骤3：清算结果（引擎计算后回填） */
  const settlementResult = ref<{
    settlementAmount?: number
    details?: unknown[]
  } | null>(null)

  function reset() {
    currentStep.value = 0
    selectedContract.value = null
    terminationInfo.value = {}
    settlementResult.value = null
  }

  return { currentStep, selectedContract, terminationInfo, settlementResult, reset }
})
