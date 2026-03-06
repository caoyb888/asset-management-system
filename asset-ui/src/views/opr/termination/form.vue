<template>
  <div class="termination-form-page">
    <!-- 页面头部 -->
    <div class="page-header-bar">
      <div class="bar-left">
        <el-button :icon="ArrowLeft" text @click="$router.back()">返回</el-button>
        <span class="bar-divider" />
        <span class="bar-title">{{ isEdit ? '编辑解约单' : '新增解约单' }}</span>
      </div>
    </div>

    <el-card shadow="never" class="form-card">
      <!-- 步骤条 -->
      <el-steps :active="step" finish-status="success" style="margin-bottom:32px">
        <el-step title="选择合同" />
        <el-step title="解约信息" />
        <el-step title="清算确认" />
      </el-steps>

      <!-- Step 1：选择合同 -->
      <div v-if="step === 0">
        <el-form :model="step1" label-width="100px" style="max-width:600px">
          <el-form-item label="合同编号" required>
            <el-input v-model="step1.contractCode" placeholder="输入合同编号后回车搜索" clearable
              @keyup.enter="searchContract" @clear="clearContract">
              <template #append>
                <el-button :icon="Search" @click="searchContract">搜索</el-button>
              </template>
            </el-input>
          </el-form-item>
        </el-form>

        <!-- 合同搜索结果 -->
        <el-table v-if="contractList.length" :data="contractList" border style="max-width:900px;margin-top:16px"
          highlight-current-row @current-change="selectContract">
          <el-table-column label="合同编号" prop="contractCode" width="160" />
          <el-table-column label="合同名称" prop="contractName" min-width="200" show-overflow-tooltip />
          <el-table-column label="商家名称" prop="merchantName" width="140" />
          <el-table-column label="合同期限" width="220">
            <template #default="{ row }">
              {{ row.contractStart }} ~ {{ row.contractEnd }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 3 ? 'success' : 'info'">
                {{ contractStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>

        <!-- 已选合同展示 -->
        <el-alert v-if="selectedContract" type="success" :closable="false" style="max-width:900px;margin-top:16px">
          <template #title>
            已选合同：{{ selectedContract.contractCode }} - {{ selectedContract.contractName }}
            <el-button link type="primary" style="margin-left:8px" @click="clearContract">重新选择</el-button>
          </template>
        </el-alert>

        <div class="step-footer">
          <el-button type="primary" :disabled="!selectedContract" @click="step = 1">
            下一步
          </el-button>
        </div>
      </div>

      <!-- Step 2：解约信息 -->
      <div v-if="step === 1">
        <el-form ref="step2FormRef" :model="step2" :rules="step2Rules" label-width="120px" style="max-width:600px">
          <el-form-item label="解约类型" prop="terminationType">
            <el-radio-group v-model="step2.terminationType">
              <el-radio :value="1">到期终止</el-radio>
              <el-radio :value="2">提前解约</el-radio>
              <el-radio :value="3">重签解约</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="解约日期" prop="terminationDate">
            <el-date-picker v-model="step2.terminationDate" type="date" value-format="YYYY-MM-DD"
              placeholder="选择解约日期" style="width:100%" />
          </el-form-item>
          <el-form-item v-if="step2.terminationType === 2" label="违约金比例">
            <el-input-number v-model="step2.penaltyRate" :min="0" :max="1" :step="0.05"
              :precision="2" placeholder="0.30" style="width:160px" />
            <span style="margin-left:8px;color:#909399;font-size:13px">（0~1，默认0.3即30%）</span>
          </el-form-item>
          <el-form-item v-if="step2.terminationType === 3" label="新合同ID" prop="newContractId">
            <el-input-number v-model="step2.newContractId" :min="1" placeholder="填写新合同ID" style="width:200px" />
          </el-form-item>
          <el-form-item label="解约原因">
            <el-input v-model="step2.reason" type="textarea" :rows="3" placeholder="选填" />
          </el-form-item>
        </el-form>

        <div class="step-footer">
          <el-button @click="step = 0">上一步</el-button>
          <el-button type="primary" @click="nextToStep3">下一步（预保存并计算清算）</el-button>
        </div>
      </div>

      <!-- Step 3：清算确认 -->
      <div v-if="step === 2">
        <el-descriptions :column="2" border style="max-width:800px;margin-bottom:24px">
          <el-descriptions-item label="解约单号">{{ savedTermination?.terminationCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="合同编号">{{ selectedContract?.contractCode }}</el-descriptions-item>
          <el-descriptions-item label="合同名称">{{ selectedContract?.contractName }}</el-descriptions-item>
          <el-descriptions-item label="解约类型">{{ typeLabel(step2.terminationType) }}</el-descriptions-item>
          <el-descriptions-item label="解约日期">{{ step2.terminationDate }}</el-descriptions-item>
          <el-descriptions-item label="清算总额">
            <span v-if="savedTermination?.settlementAmount != null"
              :class="savedTermination.settlementAmount >= 0 ? 'text-danger' : 'text-success'" style="font-size:16px;font-weight:600">
              ¥ {{ formatMoney(savedTermination.settlementAmount) }}
            </span>
            <span v-else class="text-gray">计算中...</span>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 清算明细 -->
        <div style="max-width:800px">
          <div class="section-title">清算明细</div>
          <el-table :data="settlements" border>
            <el-table-column label="明细类型" prop="itemType" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="itemTypeColor(row.itemType)" size="small">
                  {{ itemTypeLabel(row.itemType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="明细名称" prop="itemName" min-width="160" />
            <el-table-column label="金额(元)" prop="amount" width="140" align="right">
              <template #default="{ row }">
                <span :class="row.amount >= 0 ? 'text-danger' : 'text-success'">
                  {{ formatMoney(row.amount) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="备注" prop="remark" min-width="180" show-overflow-tooltip />
          </el-table>
          <el-empty v-if="!settlements.length" description="暂无清算明细" style="padding:20px 0" />
        </div>

        <el-alert v-if="settlements.length" style="margin-top:16px" type="info" :closable="false">
          正数金额表示「应收（租方欠款）」，负数金额表示「应退（平台退款）」
        </el-alert>

        <div class="step-footer">
          <el-button @click="step = 1">上一步</el-button>
          <el-button type="warning" :loading="calcLoading" @click="recalculate">重新计算</el-button>
          <el-button type="primary" :loading="submitLoading"
            :disabled="!savedTermination?.settlementAmount && savedTermination?.settlementAmount !== 0"
            @click="doSubmitApproval">提交审批</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Search } from '@element-plus/icons-vue'
import {
  createTermination,
  updateTermination,
  getTerminationById,
  calculateSettlement,
  submitTerminationApproval,
  type TerminationDetailVO,
  type TerminationSettlementItem
} from '@/api/opr/termination'
import request from '@/api/request'

const router = useRouter()
const route = useRoute()

const isEdit = computed(() => !!route.query.id)
const editId = computed(() => Number(route.query.id) || null)

const step = ref(0)

// Step1 状态
const step1 = reactive({ contractCode: '' })
const contractList = ref<any[]>([])
const selectedContract = ref<any>(null)

// Step2 状态
const step2 = reactive({
  terminationType: 1,
  terminationDate: '',
  reason: '',
  penaltyRate: 0.3,
  newContractId: null as number | null
})
const step2FormRef = ref()
const step2Rules = {
  terminationType: [{ required: true, message: '请选择解约类型', trigger: 'change' }],
  terminationDate: [{ required: true, message: '请选择解约日期', trigger: 'change' }],
  newContractId: [{ required: true, message: '重签解约必须填写新合同ID', trigger: 'blur' }]
}

// Step3 状态
const savedTermination = ref<TerminationDetailVO | null>(null)
const settlements = ref<TerminationSettlementItem[]>([])
const calcLoading = ref(false)
const submitLoading = ref(false)

// 搜索合同
async function searchContract() {
  if (!step1.contractCode.trim()) return
  try {
    const res = await request.get('/inv/contracts', {
      params: { contractCode: step1.contractCode, pageSize: 10 }
    })
    contractList.value = res.records || []
    if (!contractList.value.length) ElMessage.info('未找到匹配合同')
  } catch (e: any) {
    ElMessage.error('搜索失败')
  }
}

function selectContract(row: any) {
  if (!row) return
  selectedContract.value = row
  contractList.value = []
}

function clearContract() {
  selectedContract.value = null
  step1.contractCode = ''
  contractList.value = []
}

// Step2 → Step3：保存草稿 + 触发清算
async function nextToStep3() {
  const valid = await step2FormRef.value?.validate().catch(() => false)
  if (!valid) return

  calcLoading.value = true
  try {
    const dto = {
      contractId: selectedContract.value.id,
      terminationType: step2.terminationType,
      terminationDate: step2.terminationDate,
      reason: step2.reason,
      newContractId: step2.newContractId || undefined,
      penaltyRate: step2.terminationType === 2 ? step2.penaltyRate : undefined
    }

    let id: number
    if (isEdit.value && editId.value) {
      await updateTermination(editId.value, dto)
      id = editId.value
    } else if (savedTermination.value?.id) {
      await updateTermination(savedTermination.value.id, dto)
      id = savedTermination.value.id
    } else {
      const res = await createTermination(dto)
      id = res
    }

    // 触发清算计算
    await calculateSettlement(id)

    // 加载详情（含清算明细）
    const detailRes = await getTerminationById(id)
    savedTermination.value = detailRes.data
    settlements.value = detailRes.data?.settlements || []

    step.value = 2
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '操作失败')
  } finally {
    calcLoading.value = false
  }
}

// 重新计算清算
async function recalculate() {
  if (!savedTermination.value?.id) return
  calcLoading.value = true
  try {
    await calculateSettlement(savedTermination.value.id)
    const res = await getTerminationById(savedTermination.value.id)
    savedTermination.value = res
    settlements.value = res.settlements || []
    ElMessage.success('重新计算完成')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '计算失败')
  } finally {
    calcLoading.value = false
  }
}

// 提交审批
async function doSubmitApproval() {
  if (!savedTermination.value?.id) return
  submitLoading.value = true
  try {
    await submitTerminationApproval(savedTermination.value.id)
    ElMessage.success('已提交审批')
    router.push('/opr/terminations')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '提交失败')
  } finally {
    submitLoading.value = false
  }
}

// 编辑模式：回填数据
async function loadEditData() {
  if (!editId.value) return
  try {
    const res = await getTerminationById(editId.value)
    const data = res as TerminationDetailVO
    savedTermination.value = data
    settlements.value = data.settlements || []

    step2.terminationType = data.terminationType
    step2.terminationDate = data.terminationDate
    step2.reason = data.reason || ''
    step2.newContractId = data.newContractId || null
    step2.penaltyRate = data.penaltyAmount || 0.3

    selectedContract.value = {
      id: data.contractId,
      contractCode: data.contractCode,
      contractName: data.contractName
    }
    step.value = 0
  } catch (e) {
    ElMessage.error('加载失败')
  }
}

// 工具函数
function typeLabel(t: number) {
  return { 1: '到期终止', 2: '提前解约', 3: '重签解约' }[t] ?? ''
}
function contractStatusLabel(s: number) {
  return { 1: '草稿', 2: '审批中', 3: '执行中', 4: '已完成', 5: '已解约' }[s] ?? ''
}
function itemTypeLabel(t: number) {
  return { 1: '未收租费', 2: '违约金', 3: '保证金退还', 4: '其他' }[t] ?? ''
}
function itemTypeColor(t: number): 'success' | 'warning' | 'info' | 'danger' | undefined {
  const m: Record<number, 'success' | 'warning' | 'info' | 'danger'> = {
    1: 'danger', 2: 'warning', 3: 'success', 4: 'info'
  }
  return m[t]
}
function formatMoney(val: number) {
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

onMounted(() => {
  if (isEdit.value) loadEditData()
})
</script>

<style scoped lang="scss">
.termination-form-page { display: flex; flex-direction: column; gap: 16px; }

.page-header-bar {
  display: flex; align-items: center;
  padding: 12px 20px; background: #fff;
  border-radius: 12px; border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  .bar-left { display: flex; align-items: center; gap: 12px; }
  .bar-divider { width: 1px; height: 16px; background: #e2e8f0; }
  .bar-title { font-size: 16px; font-weight: 600; color: #1e293b; }
}

.form-card {
  border-radius: 12px !important;
  border: 1px solid rgba(0, 0, 0, 0.06) !important;
  :deep(.el-card__body) { padding: 24px 32px; }
}

.step-footer {
  margin-top: 32px; padding-top: 16px;
  border-top: 1px solid #f1f5f9;
  display: flex; gap: 12px;
}

.section-title {
  font-size: 14px; font-weight: 600; color: #1e293b;
  margin-bottom: 12px; padding-left: 8px;
  border-left: 3px solid #3b82f6;
}

.text-danger { color: #f56c6c; }
.text-success { color: #67c23a; }
.text-gray { color: #909399; font-size: 13px; }
</style>
