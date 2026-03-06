<template>
  <div class="change-form-page">
    <!-- 页面头部 -->
    <div class="page-header-bar">
      <div class="bar-left">
        <el-button :icon="ArrowLeft" text @click="router.back()">返回</el-button>
        <span class="bar-divider" />
        <span class="bar-title">{{ isEdit ? '编辑变更单' : '新增变更单' }}</span>
      </div>
    </div>

    <el-card shadow="never" class="form-card">
      <!-- 步骤条 -->
      <el-steps :active="activeStep" finish-status="success" style="margin-bottom:32px">
        <el-step title="选择合同" />
        <el-step title="变更类型" />
        <el-step title="填写内容" />
        <el-step title="影响预览" />
      </el-steps>

      <!-- Step 0：选择合同 -->
      <div v-if="activeStep === 0">
        <el-form :model="step0Form" :rules="step0Rules" ref="step0Ref" label-width="120px">
          <el-form-item label="合同编号/名称" prop="contractId">
            <el-select
              v-model="step0Form.contractId"
              filterable
              remote
              :remote-method="searchContracts"
              :loading="contractLoading"
              placeholder="输入合同编号或商家名称搜索"
              style="width:400px"
              @change="onContractSelect"
            >
              <el-option
                v-for="c in contractOptions"
                :key="c.id"
                :value="c.id"
                :label="`${c.contractCode} · ${c.merchantName || ''}`"
              >
                <div>
                  <span style="font-weight:600">{{ c.contractCode }}</span>
                  <span style="color:#909399;margin-left:8px;font-size:12px">{{ c.contractName }}</span>
                </div>
                <div style="font-size:12px;color:#606266">
                  商家：{{ c.merchantName }} ｜ 到期：{{ c.contractEnd }}
                </div>
              </el-option>
            </el-select>
          </el-form-item>
          <!-- 选中合同信息展示 -->
          <el-form-item v-if="selectedContract" label="合同信息">
            <el-descriptions :column="3" size="small" border>
              <el-descriptions-item label="合同编号">{{ selectedContract.contractCode }}</el-descriptions-item>
              <el-descriptions-item label="合同名称">{{ selectedContract.contractName }}</el-descriptions-item>
              <el-descriptions-item label="商家名称">{{ selectedContract.merchantName }}</el-descriptions-item>
              <el-descriptions-item label="合同开始">{{ selectedContract.contractStart }}</el-descriptions-item>
              <el-descriptions-item label="合同到期">{{ selectedContract.contractEnd }}</el-descriptions-item>
              <el-descriptions-item label="合同类型">{{ selectedContract.contractTypeName }}</el-descriptions-item>
            </el-descriptions>
          </el-form-item>
        </el-form>
      </div>

      <!-- Step 1：变更类型多选 -->
      <div v-if="activeStep === 1">
        <el-form :model="step1Form" :rules="step1Rules" ref="step1Ref" label-width="120px">
          <el-form-item label="变更类型" prop="changeTypeCodes">
            <div class="type-checkbox-group">
              <el-checkbox-group v-model="step1Form.changeTypeCodes">
                <el-checkbox
                  v-for="o in CHANGE_TYPE_OPTIONS"
                  :key="o.code"
                  :value="o.code"
                  :label="o.code"
                  border
                  class="type-checkbox"
                >
                  <div class="type-checkbox-inner">
                    <el-tag :type="typeTagType(o.code)" size="small" style="margin-right:4px">{{ o.code }}</el-tag>
                    <span>{{ o.label }}</span>
                  </div>
                </el-checkbox>
              </el-checkbox-group>
            </div>
          </el-form-item>
          <el-form-item label="变更生效日期" prop="effectiveDate">
            <el-date-picker
              v-model="step1Form.effectiveDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择生效日期"
            />
          </el-form-item>
          <el-form-item label="变更原因">
            <el-input v-model="step1Form.reason" type="textarea" :rows="3" style="width:500px" placeholder="请说明变更原因" />
          </el-form-item>
        </el-form>
      </div>

      <!-- Step 2：按变更类型渲染动态表单 -->
      <div v-if="activeStep === 2">
        <el-form :model="changeFields" ref="step2Ref" label-width="140px">

          <!-- RENT / FEE：租金/费项单价变更 -->
          <template v-if="hasType('RENT') || hasType('FEE')">
            <el-divider content-position="left">
              <el-tag type="danger" size="small">{{ hasType('RENT') ? '租金变更' : '费项单价变更' }}</el-tag>
            </el-divider>
            <el-form-item label="原租金金额（月）" prop="oldRentAmount">
              <el-input-number v-model="changeFields.oldRentAmount" :precision="2" :min="0" style="width:200px" />
              <span class="form-hint">元/月</span>
            </el-form-item>
            <el-form-item label="新租金金额（月）" prop="newRentAmount"
              :rules="[{ required: true, message: '请输入新租金金额', trigger: 'blur' }]">
              <el-input-number v-model="changeFields.newRentAmount" :precision="2" :min="0.01" style="width:200px" />
              <span class="form-hint">元/月</span>
            </el-form-item>
          </template>

          <!-- TERM：租期变更 -->
          <template v-if="hasType('TERM')">
            <el-divider content-position="left">
              <el-tag type="primary" size="small">租期变更</el-tag>
            </el-divider>
            <el-form-item label="新合同开始日期">
              <el-date-picker v-model="changeFields.newContractStart" type="date" value-format="YYYY-MM-DD" />
              <span class="form-hint">（选填，不改则留空）</span>
            </el-form-item>
            <el-form-item label="新合同到期日期" prop="newContractEnd"
              :rules="[{ required: true, message: '请选择新合同到期日期', trigger: 'change' }]">
              <el-date-picker v-model="changeFields.newContractEnd" type="date" value-format="YYYY-MM-DD" />
            </el-form-item>
          </template>

          <!-- AREA：面积变更 -->
          <template v-if="hasType('AREA')">
            <el-divider content-position="left">
              <el-tag type="success" size="small">面积变更</el-tag>
            </el-divider>
            <el-form-item label="原租赁面积（㎡）">
              <el-input-number v-model="changeFields.oldRentArea" :precision="2" :min="0" style="width:200px" />
            </el-form-item>
            <el-form-item label="新租赁面积（㎡）"
              :rules="[{ required: true, message: '请输入新租赁面积', trigger: 'blur' }]">
              <el-input-number v-model="changeFields.newRentArea" :precision="2" :min="0.01" style="width:200px" />
            </el-form-item>
          </template>

          <!-- BRAND：品牌变更 -->
          <template v-if="hasType('BRAND')">
            <el-divider content-position="left">
              <el-tag size="small">品牌变更</el-tag>
            </el-divider>
            <el-form-item label="新品牌名称"
              :rules="[{ required: true, message: '请输入新品牌名称', trigger: 'blur' }]">
              <el-input v-model="changeFields.newBrandName" style="width:280px" placeholder="请输入新品牌名称" />
            </el-form-item>
          </template>

          <!-- TENANT：租户主体变更 -->
          <template v-if="hasType('TENANT')">
            <el-divider content-position="left">
              <el-tag size="small">租户主体变更</el-tag>
            </el-divider>
            <el-form-item label="新商家名称"
              :rules="[{ required: true, message: '请输入新商家名称', trigger: 'blur' }]">
              <el-input v-model="changeFields.newMerchantName" style="width:280px" placeholder="请输入新商家名称" />
            </el-form-item>
          </template>

          <!-- COMPANY：公司名称变更 -->
          <template v-if="hasType('COMPANY')">
            <el-divider content-position="left">
              <el-tag size="small">公司名称变更</el-tag>
            </el-divider>
            <el-form-item label="新公司名称"
              :rules="[{ required: true, message: '请输入新公司名称', trigger: 'blur' }]">
              <el-input v-model="changeFields.newCompanyName" style="width:280px" placeholder="请输入新公司名称" />
            </el-form-item>
          </template>

          <!-- CLAUSE：合同条款变更 -->
          <template v-if="hasType('CLAUSE')">
            <el-divider content-position="left">
              <el-tag size="small">合同条款变更</el-tag>
            </el-divider>
            <el-form-item label="条款内容"
              :rules="[{ required: true, message: '请输入条款内容', trigger: 'blur' }]">
              <el-input v-model="changeFields.clauseContent" type="textarea" :rows="4" style="width:500px"
                placeholder="请输入变更后的合同条款内容" />
            </el-form-item>
          </template>
        </el-form>
      </div>

      <!-- Step 3：影响预览 -->
      <div v-if="activeStep === 3">
        <div v-if="impactLoading" style="text-align:center;padding:40px">
          <el-icon class="is-loading" size="32"><Loading /></el-icon>
          <div style="margin-top:12px;color:#909399">正在计算变更影响...</div>
        </div>
        <template v-else-if="impactData">
          <!-- 统计卡片 -->
          <el-row :gutter="16" style="margin-bottom:20px">
            <el-col :span="6">
              <el-card shadow="never" class="stat-card">
                <div class="stat-num">{{ impactData.affectedPlanCount }}</div>
                <div class="stat-label">受影响应收（条）</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="never" class="stat-card">
                <div class="stat-num">{{ formatMoney(impactData.originalTotalAmount) }}</div>
                <div class="stat-label">原应收合计（元）</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="never" class="stat-card">
                <div class="stat-num">{{ formatMoney(impactData.newTotalAmount) }}</div>
                <div class="stat-label">变更后合计（元）</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="never" :class="['stat-card', impactData.amountDiff >= 0 ? 'stat-up' : 'stat-down']">
                <div class="stat-num">
                  {{ impactData.amountDiff >= 0 ? '+' : '' }}{{ formatMoney(impactData.amountDiff) }}
                </div>
                <div class="stat-label">应收差额（元）</div>
              </el-card>
            </el-col>
          </el-row>

          <!-- 字段对比表 -->
          <el-table
            v-if="impactData.fieldComparisons?.length"
            :data="impactData.fieldComparisons"
            border
            size="small"
            style="margin-bottom:16px"
          >
            <el-table-column prop="label" label="变更字段" width="180" />
            <el-table-column prop="oldValue" label="变更前" min-width="200">
              <template #default="{ row }">
                <span class="old-value">{{ row.oldValue || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="newValue" label="变更后" min-width="200">
              <template #default="{ row }">
                <span class="new-value">{{ row.newValue || '—' }}</span>
              </template>
            </el-table-column>
          </el-table>

          <!-- 影响说明 -->
          <el-alert v-if="impactData.impactDesc" :title="impactData.impactDesc" type="info" :closable="false" />
        </template>
        <el-empty v-else description="暂无影响数据" />
      </div>

      <!-- 底部按钮 -->
      <div class="form-footer">
        <el-button v-if="activeStep > 0" @click="activeStep--">上一步</el-button>
        <el-button
          v-if="activeStep < 3"
          type="primary"
          :loading="saving"
          @click="handleNext"
        >下一步</el-button>
        <el-button
          v-if="activeStep === 3"
          type="primary"
          :loading="saving"
          @click="handleSubmit"
        >{{ savedChangeId ? '保存并完成' : '提交草稿' }}</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading, ArrowLeft } from '@element-plus/icons-vue'
import {
  CHANGE_TYPE_OPTIONS,
  createChange,
  updateChange,
  previewImpact,
  getChangeById,
  type ChangeDetailVO,
  type ChangeImpactVO,
} from '@/api/opr/change'
import request from '@/api/request'

const router = useRouter()
const route = useRoute()
const editId = route.query.id ? Number(route.query.id) : undefined
const isEdit = !!editId

const activeStep = ref(0)
const saving = ref(false)
const impactLoading = ref(false)
const contractLoading = ref(false)
const savedChangeId = ref<number | null>(null)
const impactData = ref<ChangeImpactVO | null>(null)

// 合同搜索
const contractOptions = ref<any[]>([])
const selectedContract = ref<any>(null)

// Step 0
const step0Form = reactive({ contractId: undefined as number | undefined })
const step0Ref = ref()
const step0Rules = { contractId: [{ required: true, message: '请选择合同', trigger: 'change' }] }

// Step 1
const step1Form = reactive({
  changeTypeCodes: [] as string[],
  effectiveDate: '',
  reason: '',
})
const step1Ref = ref()
const step1Rules = {
  changeTypeCodes: [{
    validator: (_rule: unknown, value: string[], callback: (e?: Error) => void) => {
      if (!value || value.length === 0) callback(new Error('请至少选择一种变更类型'))
      else callback()
    },
    trigger: 'change',
  }],
  effectiveDate: [{ required: true, message: '请选择变更生效日期', trigger: 'change' }],
}

// Step 2 - 动态字段
const changeFields = reactive<Record<string, any>>({
  oldRentAmount: undefined,
  newRentAmount: undefined,
  newContractStart: '',
  newContractEnd: '',
  oldRentArea: undefined,
  newRentArea: undefined,
  newBrandName: '',
  newMerchantName: '',
  newCompanyName: '',
  clauseContent: '',
})
const step2Ref = ref()

type TagType = 'success' | 'primary' | 'warning' | 'info' | 'danger' | undefined
const TYPE_TAG_MAP: Record<string, TagType> = {
  RENT: 'danger', FEE: 'warning', TERM: 'primary', AREA: 'success',
  BRAND: 'info', TENANT: 'info', COMPANY: 'info', CLAUSE: 'info',
}
function typeTagType(code: string): TagType { return TYPE_TAG_MAP[code] }
function hasType(code: string): boolean { return step1Form.changeTypeCodes.includes(code) }

function formatMoney(v?: number): string {
  if (v == null) return '—'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function searchContracts(keyword: string) {
  if (!keyword) return
  contractLoading.value = true
  try {
    const res = await request.get('/inv/contracts', { params: { keyword, pageSize: 20 } })
    contractOptions.value = res.records || []
  } catch {
    contractOptions.value = []
  } finally {
    contractLoading.value = false
  }
}

function onContractSelect(id: number) {
  selectedContract.value = contractOptions.value.find(c => c.id === id) || null
}

async function handleNext() {
  if (activeStep.value === 0) {
    await step0Ref.value.validate()
    activeStep.value = 1
  } else if (activeStep.value === 1) {
    await step1Ref.value.validate()
    await saveDraft()
    activeStep.value = 2
  } else if (activeStep.value === 2) {
    await saveDraft()
    activeStep.value = 3
    await loadImpact()
  }
}

async function saveDraft() {
  saving.value = true
  try {
    const dto = {
      contractId: step0Form.contractId!,
      changeTypeCodes: step1Form.changeTypeCodes,
      effectiveDate: step1Form.effectiveDate,
      reason: step1Form.reason,
      changeFields: { ...changeFields },
    }
    if (savedChangeId.value) {
      await updateChange(savedChangeId.value, dto)
    } else if (isEdit && editId) {
      await updateChange(editId, dto)
      savedChangeId.value = editId
    } else {
      const res = await createChange(dto)
      savedChangeId.value = res
    }
  } finally {
    saving.value = false
  }
}

async function loadImpact() {
  if (!savedChangeId.value) return
  impactLoading.value = true
  impactData.value = null
  try {
    const res = await previewImpact(savedChangeId.value)
    impactData.value = res
  } catch {
    ElMessage.warning('影响预览计算失败，可直接提交草稿')
  } finally {
    impactLoading.value = false
  }
}

async function handleSubmit() {
  ElMessage.success('变更单已保存为草稿，可在列表中提交审批')
  router.push('/opr/contract-changes')
}

async function loadEditData() {
  if (!editId) return
  try {
    const res = await getChangeById(editId)
    const d: ChangeDetailVO = res as any
    step0Form.contractId = d.contractId
    step1Form.changeTypeCodes = d.changeTypeCodes || []
    step1Form.effectiveDate = d.effectiveDate || ''
    step1Form.reason = d.reason || ''
    savedChangeId.value = editId
    for (const det of d.details || []) {
      if (det.newValue != null) changeFields[det.fieldName] = det.newValue
    }
  } catch {
    ElMessage.error('加载变更单失败')
  }
}

onMounted(() => { if (isEdit) loadEditData() })
</script>

<style scoped lang="scss">
.change-form-page { display: flex; flex-direction: column; gap: 16px; }

.page-header-bar {
  display: flex; align-items: center; justify-content: space-between;
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

.form-footer {
  margin-top: 32px; text-align: right;
  border-top: 1px solid #f1f5f9; padding-top: 20px;
  display: flex; justify-content: flex-end; gap: 8px;
}

.form-hint { margin-left: 8px; color: #909399; font-size: 12px; }
.type-checkbox-group { display: flex; flex-wrap: wrap; gap: 8px; }
.type-checkbox { margin: 0 !important; }
.type-checkbox-inner { display: flex; align-items: center; }

.stat-card {
  text-align: center;
  border-radius: 8px !important;
  border: 1px solid rgba(0, 0, 0, 0.06) !important;
  .stat-num { font-size: 22px; font-weight: 700; line-height: 1.3; }
  .stat-label { font-size: 12px; color: #64748b; margin-top: 4px; }
}
.stat-up .stat-num { color: #f56c6c; }
.stat-down .stat-num { color: #67c23a; }

.old-value { color: #f56c6c; text-decoration: line-through; }
.new-value { color: #67c23a; font-weight: 600; }
</style>
