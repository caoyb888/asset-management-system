import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 合同变更 Pinia Store
 * 阶段二补充：缓存变更表单四步数据（合同选择→变更类型→动态表单→影响预览）
 */
export const useChangeStore = defineStore('opr-change', () => {
  /** 当前表单步骤（0-3） */
  const currentStep = ref(0)

  /** 步骤1：已选合同信息 */
  const selectedContract = ref<Record<string, unknown> | null>(null)

  /** 步骤2：已选变更类型编码列表（RENT/BRAND/TENANT/FEE/CLAUSE/TERM/AREA/COMPANY） */
  const changeTypeCodes = ref<string[]>([])

  /** 步骤3：变更字段值（key=fieldName, value=newValue） */
  const changeFields = ref<Record<string, unknown>>({})

  /** 步骤4：影响预览结果（应收差异汇总） */
  const impactPreview = ref<Record<string, unknown> | null>(null)

  function reset() {
    currentStep.value = 0
    selectedContract.value = null
    changeTypeCodes.value = []
    changeFields.value = {}
    impactPreview.value = null
  }

  return { currentStep, selectedContract, changeTypeCodes, changeFields, impactPreview, reset }
})
