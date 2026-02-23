<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>计租方案管理</span>
          <el-button type="primary" :icon="Plus" @click="handleCreate">新增方案</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="schemeCode" label="方案编码" width="120" />
        <el-table-column prop="schemeName" label="方案名称" min-width="140" />
        <el-table-column prop="chargeType" label="收费方式" width="120">
          <template #default="{ row }">
            <el-tag :type="chargeTypeColor(row.chargeType)" size="small">
              {{ CHARGE_TYPE_MAP[row.chargeType] ?? '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="paymentCycle" label="支付周期" width="110">
          <template #default="{ row }">{{ PAYMENT_CYCLE_MAP[row.paymentCycle] ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="billingMode" label="账期模式" width="100">
          <template #default="{ row }">{{ BILLING_MODE_MAP[row.billingMode] ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="strategyBeanName" label="策略Bean" width="180" />
        <el-table-column prop="description" label="说明" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              @change="(val) => handleToggleStatus(row, val as boolean)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editId ? '编辑计租方案' : '新增计租方案'"
      width="640px"
      :close-on-click-modal="false"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="方案编码" prop="schemeCode">
              <el-input v-model="form.schemeCode" placeholder="如 RS006" :disabled="!!editId" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="方案名称" prop="schemeName">
              <el-input v-model="form.schemeName" placeholder="请输入方案名称" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="收费方式" prop="chargeType">
              <el-select v-model="form.chargeType" placeholder="请选择" style="width: 100%" @change="onChargeTypeChange">
                <el-option v-for="(label, val) in CHARGE_TYPE_MAP" :key="val" :label="label" :value="Number(val)" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="支付周期" prop="paymentCycle">
              <el-select v-model="form.paymentCycle" placeholder="请选择" style="width: 100%">
                <el-option v-for="(label, val) in PAYMENT_CYCLE_MAP" :key="val" :label="label" :value="Number(val)" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="账期模式" prop="billingMode">
              <el-select v-model="form.billingMode" placeholder="请选择" style="width: 100%">
                <el-option v-for="(label, val) in BILLING_MODE_MAP" :key="val" :label="label" :value="Number(val)" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="策略Bean" prop="strategyBeanName">
              <el-select v-model="form.strategyBeanName" placeholder="请选择策略" style="width: 100%">
                <el-option v-for="b in STRATEGY_BEANS" :key="b" :label="b" :value="b" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="状态">
          <el-switch v-model="form.statusBool" active-text="启用" inactive-text="停用" />
        </el-form-item>

        <el-form-item label="方案说明">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入说明" />
        </el-form-item>

        <!-- formula_json 参数提示 -->
        <el-form-item label="公式参数">
          <el-alert :title="formulaParamHint" type="info" :closable="false" show-icon />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getRentSchemeAllList,
  createRentScheme,
  updateRentScheme,
  deleteRentScheme,
  toggleRentSchemeStatus,
  type RentSchemeVO,
  type RentSchemeSaveDTO,
} from '@/api/inv/config'

// ─── 字典 ───
const CHARGE_TYPE_MAP: Record<number, string> = {
  1: '固定租金', 2: '固定提成', 3: '阶梯提成', 4: '两者取高', 5: '一次性',
}
const PAYMENT_CYCLE_MAP: Record<number, string> = {
  1: '月付', 2: '两月付', 3: '季付', 4: '四月付', 5: '半年付', 6: '年付',
}
const BILLING_MODE_MAP: Record<number, string> = {
  1: '预付', 2: '当期', 3: '后付',
}
const STRATEGY_BEANS = [
  'fixedRentStrategy', 'fixedCommissionStrategy',
  'stepCommissionStrategy', 'higherOfStrategy', 'oneTimeStrategy',
]
// chargeType -> 推荐 strategyBean
const CHARGE_STRATEGY_MAP: Record<number, string> = {
  1: 'fixedRentStrategy', 2: 'fixedCommissionStrategy',
  3: 'stepCommissionStrategy', 4: 'higherOfStrategy', 5: 'oneTimeStrategy',
}
// chargeType -> formula_json params 提示
const FORMULA_HINT_MAP: Record<number, string> = {
  1: '参数: unit_price（单价）, area（面积）, months（月数）',
  2: '参数: commission_rate（提成率）, revenue（营业额）, min_commission_amount（最低保底）',
  3: '参数: stages（阶段数组）, commission_rate, min_commission_amount',
  4: '参数: fixed_amount（固定金额）, commission_rate, revenue, min_commission_amount',
  5: '参数: amount（一次性总额）',
}

type TagType = 'primary' | 'success' | 'warning' | 'danger' | 'info' | undefined
const chargeTypeColor = (t: number): TagType => {
  const map: Record<number, TagType> = { 1: undefined, 2: 'success', 3: 'warning', 4: 'danger', 5: 'info' }
  return map[t]
}

// ─── 数据 ───
const loading = ref(false)
const list = ref<RentSchemeVO[]>([])

async function fetchList() {
  loading.value = true
  try { list.value = await getRentSchemeAllList() }
  finally { loading.value = false }
}

// ─── 弹窗 ───
const dialogVisible = ref(false)
const saving = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<FormInstance>()

interface FormModel {
  schemeCode: string
  schemeName: string
  chargeType: number | null
  paymentCycle: number | null
  billingMode: number | null
  strategyBeanName: string
  statusBool: boolean
  description: string
}

const defaultForm = (): FormModel => ({
  schemeCode: '', schemeName: '',
  chargeType: null, paymentCycle: null, billingMode: null,
  strategyBeanName: '', statusBool: true, description: '',
})

const form = ref<FormModel>(defaultForm())

const rules: FormRules = {
  schemeCode: [{ required: true, message: '请输入方案编码', trigger: 'blur' }],
  schemeName: [{ required: true, message: '请输入方案名称', trigger: 'blur' }],
  chargeType: [{ required: true, message: '请选择收费方式', trigger: 'change' }],
  paymentCycle: [{ required: true, message: '请选择支付周期', trigger: 'change' }],
  billingMode: [{ required: true, message: '请选择账期模式', trigger: 'change' }],
  strategyBeanName: [{ required: true, message: '请选择策略Bean', trigger: 'change' }],
}

const formulaParamHint = computed(() =>
  form.value.chargeType ? FORMULA_HINT_MAP[form.value.chargeType] : '请先选择收费方式',
)

function onChargeTypeChange(val: number) {
  // 自动填充推荐的策略Bean
  if (CHARGE_STRATEGY_MAP[val]) {
    form.value.strategyBeanName = CHARGE_STRATEGY_MAP[val]
  }
}

function resetForm() {
  editId.value = null
  form.value = defaultForm()
  formRef.value?.clearValidate()
}

function handleCreate() {
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: RentSchemeVO) {
  editId.value = row.id
  form.value = {
    schemeCode: row.schemeCode,
    schemeName: row.schemeName,
    chargeType: row.chargeType,
    paymentCycle: row.paymentCycle,
    billingMode: row.billingMode,
    strategyBeanName: row.strategyBeanName,
    statusBool: row.status === 1,
    description: row.description,
  }
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const dto: RentSchemeSaveDTO = {
      schemeCode: form.value.schemeCode,
      schemeName: form.value.schemeName,
      chargeType: form.value.chargeType!,
      paymentCycle: form.value.paymentCycle!,
      billingMode: form.value.billingMode!,
      strategyBeanName: form.value.strategyBeanName,
      status: form.value.statusBool ? 1 : 0,
      description: form.value.description,
    }
    if (editId.value) {
      await updateRentScheme(editId.value, dto)
    } else {
      await createRentScheme(dto)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchList()
  } finally {
    saving.value = false
  }
}

async function handleToggleStatus(row: RentSchemeVO, enabled: boolean) {
  const newStatus = enabled ? 1 : 0
  await toggleRentSchemeStatus(row.id, newStatus)
  row.status = newStatus
  ElMessage.success(enabled ? '已启用' : '已停用')
}

async function handleDelete(row: RentSchemeVO) {
  await ElMessageBox.confirm(`确认删除方案 "${row.schemeName}"？`, '提示', { type: 'warning' })
  await deleteRentScheme(row.id)
  ElMessage.success('删除成功')
  fetchList()
}

onMounted(fetchList)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
