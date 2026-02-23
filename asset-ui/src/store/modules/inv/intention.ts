import { defineStore } from 'pinia'
import type { IntentionSaveDTO } from '@/api/inv/intention'

/**
 * 意向协议向导表单状态管理
 * 缓存7个步骤的数据，支持步骤间回退，上游变更时下游需重算
 */

interface IntentionFormState {
  /** 当前编辑的意向协议ID（新增为null） */
  intentionId: number | null
  /** 当前步骤（0-6） */
  currentStep: number
  /** 步骤1: 计租方案 */
  rentScheme: {
    schemeId: number | null
    schemeName: string
    chargeType: number | null
  }
  /** 步骤2: 基础信息 */
  basicInfo: {
    projectId: number | null
    projectName: string
    intentionName: string
    partyBName: string
    merchantId: number | null
    brandId: number | null
  }
  /** 步骤3: 商务信息 */
  businessInfo: {
    shopIds: number[]
    contractStart: string
    contractEnd: string
    deliveryDate: string
    decorationStart: string
    decorationEnd: string
    openingDate: string
    paymentCycle: number | null
    billingMode: number | null
  }
  /** 步骤4: 费项配置 */
  fees: Array<{
    feeItemId: number
    feeItemName: string
    chargeType: number
    unitPrice: number | null
    commissionRate: number | null
    minCommissionAmount: number | null
    formulaParams: Record<string, unknown>
  }>
  /** 步骤5: 分铺计租阶段 */
  feeStages: Array<{
    shopId: number
    stageIndex: number
    stageStart: string
    stageEnd: string
    unitPrice: number | null
    minCommissionAmount: number | null
  }>
  /** 步骤6: 费用生成结果 */
  costResult: {
    totalAmount: number | null
    generated: boolean
  }
  /** 步骤7: 账期列表 */
  billingList: unknown[]
}

const defaultState = (): IntentionFormState => ({
  intentionId: null,
  currentStep: 0,
  rentScheme: { schemeId: null, schemeName: '', chargeType: null },
  basicInfo: { projectId: null, projectName: '', intentionName: '', partyBName: '', merchantId: null, brandId: null },
  businessInfo: {
    shopIds: [], contractStart: '', contractEnd: '',
    deliveryDate: '', decorationStart: '', decorationEnd: '',
    openingDate: '', paymentCycle: null, billingMode: null,
  },
  fees: [],
  feeStages: [],
  costResult: { totalAmount: null, generated: false },
  billingList: [],
})

export const useIntentionStore = defineStore('inv-intention', {
  state: defaultState,

  getters: {
    /** 是否已选定计租方案 */
    hasScheme: (state) => !!state.rentScheme.schemeId,
    /** 当前向导是否编辑状态 */
    isEdit: (state) => !!state.intentionId,
    /** 费用是否已生成 */
    isCostGenerated: (state) => state.costResult.generated,
  },

  actions: {
    /** 初始化新增向导 */
    initCreate() {
      this.$patch((s) => Object.assign(s, defaultState()))
    },

    /** 初始化编辑向导（从已有意向加载） */
    initEdit(id: number) {
      this.$patch((s) => { Object.assign(s, defaultState()); s.intentionId = id })
    },

    /** 步骤跳转（向前或向后） */
    goToStep(step: number, confirmDownstream = false) {
      if (step < this.currentStep || confirmDownstream) {
        // 回退到上游步骤时，如果费用已生成则需要重置
        if (step < 5 && this.costResult.generated) {
          this.costResult = { totalAmount: null, generated: false }
          this.billingList = []
        }
      }
      this.currentStep = step
    },

    /** 更新计租方案（上游变更，清除下游数据） */
    setRentScheme(scheme: IntentionFormState['rentScheme']) {
      this.rentScheme = scheme
      this.fees = []
      this.feeStages = []
      this.costResult = { totalAmount: null, generated: false }
      this.billingList = []
    },

    /** 更新商务信息（商铺变更，清除分铺计租） */
    setBusinessInfo(info: IntentionFormState['businessInfo']) {
      this.businessInfo = info
      this.feeStages = []
      this.costResult = { totalAmount: null, generated: false }
      this.billingList = []
    },

    /** 设置费用生成结果 */
    setCostResult(totalAmount: number) {
      this.costResult = { totalAmount, generated: true }
      this.billingList = []
    },

    /** 重置全部状态 */
    reset() {
      this.$patch((s) => Object.assign(s, defaultState()))
    },
  },
})
