import { defineStore } from 'pinia'

/**
 * 招商合同表单状态管理
 * 支持直接新增和意向转合同两种入口
 */

interface ContractFormState {
  /** 当前编辑的合同ID */
  contractId: number | null
  /** 来源意向ID（意向转合同时有值） */
  fromIntentionId: number | null
  /** 激活的Tab */
  activeTab: 'basic' | 'fees' | 'billing'
  /** 基本信息 */
  basicInfo: {
    contractName: string
    contractType: number | null   // 1标准 2临时 3补充协议
    projectId: number | null
    merchantId: number | null
    brandId: number | null
    rentSchemeId: number | null
    paymentCycle: number | null
    billingMode: number | null
    contractStart: string
    contractEnd: string
    deliveryDate: string
    decorationStart: string
    decorationEnd: string
    openingDate: string
  }
  /** 费项列表（继承自意向或手动录入） */
  fees: Array<{
    feeItemId: number
    feeItemName: string
    chargeType: number
    unitPrice: number | null
    commissionRate: number | null
    minCommissionAmount: number | null
  }>
  /** 账期列表 */
  billingList: Array<unknown>
}

const defaultState = (): ContractFormState => ({
  contractId: null,
  fromIntentionId: null,
  activeTab: 'basic',
  basicInfo: {
    contractName: '',
    contractType: null,
    projectId: null,
    merchantId: null,
    brandId: null,
    rentSchemeId: null,
    paymentCycle: null,
    billingMode: null,
    contractStart: '',
    contractEnd: '',
    deliveryDate: '',
    decorationStart: '',
    decorationEnd: '',
    openingDate: '',
  },
  fees: [],
  billingList: [] as Array<unknown>,
})

export const useContractStore = defineStore('inv-contract', {
  state: defaultState,

  getters: {
    isEdit: (state) => !!state.contractId,
    isFromIntention: (state) => !!state.fromIntentionId,
  },

  actions: {
    /** 初始化新增合同 */
    initCreate() {
      this.$patch((s) => Object.assign(s, defaultState()))
    },

    /** 初始化意向转合同（预填意向数据） */
    initFromIntention(intentionId: number) {
      this.$patch((s) => { Object.assign(s, defaultState()); s.fromIntentionId = intentionId })
    },

    /** 初始化编辑合同 */
    initEdit(id: number) {
      this.$patch((s) => { Object.assign(s, defaultState()); s.contractId = id })
    },

    /** 重置 */
    reset() {
      this.$patch((s) => Object.assign(s, defaultState()))
    },
  },
})
