<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">租费算法管理</h2>
        <p class="page-desc">维护租金、保证金、服务费等费项的计算公式与试算工具</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增算法</el-button>
    </div>

    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="算法名称">
          <el-input v-model="query.algoName" placeholder="关键字" clearable style="width:160px"
            @keyup.enter="loadList" @clear="loadList" />
        </el-form-item>
        <el-form-item label="算法类型">
          <el-select v-model="query.algoType" placeholder="全部" clearable style="width:110px" @change="loadList">
            <el-option v-for="(label, val) in ALGO_TYPE_MAP" :key="val" :label="label" :value="Number(val)" />
          </el-select>
        </el-form-item>
        <el-form-item label="计算方式">
          <el-select v-model="query.calcMode" placeholder="全部" clearable style="width:110px" @change="loadList">
            <el-option v-for="(label, val) in CALC_MODE_MAP" :key="val" :label="label" :value="Number(val)" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:90px" @change="loadList">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card shadow="never" class="mt-16">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="algoCode" label="算法编码" width="150" />
        <el-table-column prop="algoName" label="算法名称" min-width="130" />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="algoTypeTagType(row.algoType)" size="small">
              {{ ALGO_TYPE_MAP[row.algoType] ?? '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="计算方式" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ CALC_MODE_MAP[row.calcMode] ?? '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="formula" label="公式" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <code class="formula-code">{{ row.formula }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="120" show-overflow-tooltip />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1" :inactive-value="0"
              size="small"
              @change="(v: string | number | boolean) => doChangeStatus(row, Number(v))"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
            <el-button type="success" link size="small" @click="openTester(row)">试算</el-button>
            <el-button type="danger" link size="small" @click="doDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
        class="mt-16"
        @change="loadList"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="算法编码" prop="algoCode">
              <el-input v-model="form.algoCode" :disabled="!!form.id" placeholder="唯一标识（创建后不可改）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="算法名称" prop="algoName">
              <el-input v-model="form.algoName" placeholder="如 固定租金" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="算法类型" prop="algoType">
              <el-select v-model="form.algoType" style="width:100%">
                <el-option v-for="(label, val) in ALGO_TYPE_MAP" :key="val" :label="label" :value="Number(val)" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计算方式" prop="calcMode">
              <el-select v-model="form.calcMode" style="width:100%">
                <el-option v-for="(label, val) in CALC_MODE_MAP" :key="val" :label="label" :value="Number(val)" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="公式" prop="formula">
          <el-input
            v-model="form.formula"
            type="textarea"
            :rows="3"
            placeholder="支持四则运算，变量用 ${key} 格式，如 ${unit_price} * ${area} * ${days}"
            style="font-family: monospace"
          />
        </el-form-item>
        <el-form-item label="变量定义">
          <el-input
            v-model="variablesJson"
            type="textarea"
            :rows="4"
            placeholder='JSON 数组，如 [{"key":"unit_price","label":"单价","unit":"元/㎡"}]'
            style="font-family: monospace; font-size: 12px"
          />
          <div class="form-tip">变量定义将用于试算界面显示输入框</div>
        </el-form-item>
        <el-form-item label="固定参数">
          <el-input
            v-model="paramsJson"
            type="textarea"
            :rows="3"
            placeholder='JSON 对象，如 {"rate":"0.05"} — 试算时自动代入'
            style="font-family: monospace; font-size: 12px"
          />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="算法用途说明" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="doSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 试算弹窗 -->
    <el-dialog v-model="testerVisible" title="公式试算" width="580px" destroy-on-close>
      <div v-if="testerAlgo">
        <el-descriptions :column="2" border size="small" class="mb-16">
          <el-descriptions-item label="算法">{{ testerAlgo.algoName }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ ALGO_TYPE_MAP[testerAlgo.algoType] }}</el-descriptions-item>
          <el-descriptions-item label="公式" :span="2">
            <code class="formula-code">{{ testerAlgo.formula }}</code>
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="testerVariables.length > 0">
          <div class="section-title">变量输入</div>
          <el-form label-width="120px" class="mb-8">
            <el-form-item v-for="v in testerVariables" :key="v.key" :label="v.label || v.key">
              <el-input
                v-model="testerInputs[v.key]"
                :placeholder="`数值${v.unit ? '（' + v.unit + '）' : ''}`"
                style="width:220px"
              />
              <span v-if="v.unit" class="var-unit">{{ v.unit }}</span>
            </el-form-item>
          </el-form>
        </div>
        <div v-else class="no-variables">该算法无变量，使用固定参数直接计算</div>

        <div class="tester-actions">
          <el-button type="primary" :loading="calcLoading" @click="doCalc">开始试算</el-button>
        </div>

        <!-- 结果区 -->
        <div v-if="calcResult" class="calc-result">
          <el-divider />
          <div class="result-amount">
            <span class="result-label">计算结果</span>
            <span class="result-value">¥ {{ calcResult.result }}</span>
          </div>
          <div class="result-formula">
            <span class="result-label">展开公式</span>
            <code class="formula-code">{{ calcResult.expandedFormula }}</code>
          </div>
          <el-collapse class="mt-8">
            <el-collapse-item title="计算详情">
              <pre class="calc-detail">{{ calcResult.detail }}</pre>
            </el-collapse-item>
          </el-collapse>
        </div>
      </div>
      <template #footer>
        <el-button @click="testerVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import {
  algorithmApi,
  type SysFeeAlgorithm,
  type FeeAlgorithmCreateDTO,
  type AlgoVariable,
  type CalcTestResultVO,
} from '@/api/sys/algorithm'

type TagType = 'success' | 'primary' | 'warning' | 'info' | 'danger' | undefined

const ALGO_TYPE_MAP: Record<number, string> = { 1: '租金', 2: '保证金', 3: '服务费', 4: '其他' }
const CALC_MODE_MAP: Record<number, string> = { 1: '固定金额', 2: '比率计算', 3: '阶梯计算', 4: '自定义公式' }
const ALGO_TYPE_TAG: Record<number, TagType> = { 1: 'primary', 2: 'warning', 3: 'success', 4: 'info' }
function algoTypeTagType(t: number): TagType { return ALGO_TYPE_TAG[t] }

// ── 列表 ──────────────────────────────────────────────────
const loading = ref(false)
const list = ref<SysFeeAlgorithm[]>([])
const total = ref(0)
const query = reactive({
  pageNum: 1, pageSize: 20,
  algoName: '',
  algoType: undefined as number | undefined,
  calcMode: undefined as number | undefined,
  status: undefined as number | undefined,
})

async function loadList() {
  loading.value = true
  try {
    const res: any = await algorithmApi.page(query)
    list.value = res.records ?? []
    total.value = res.total ?? 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  Object.assign(query, { pageNum: 1, algoName: '', algoType: undefined, calcMode: undefined, status: undefined })
  loadList()
}

// ── CRUD 弹窗 ──────────────────────────────────────────────
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()
const form = reactive<FeeAlgorithmCreateDTO>({
  algoCode: '', algoName: '', algoType: 1, calcMode: 1, formula: '', status: 1,
})
const variablesJson = ref('[]')
const paramsJson = ref('{}')

const formRules = {
  algoCode: [{ required: true, message: '请输入算法编码', trigger: 'blur' }],
  algoName: [{ required: true, message: '请输入算法名称', trigger: 'blur' }],
  algoType: [{ required: true, message: '请选择算法类型', trigger: 'change' }],
  calcMode: [{ required: true, message: '请选择计算方式', trigger: 'change' }],
  formula:  [{ required: true, message: '请输入公式', trigger: 'blur' }],
}

function openCreate() {
  Object.assign(form, { id: undefined, algoCode: '', algoName: '', algoType: 1, calcMode: 1, formula: '', description: '', status: 1 })
  variablesJson.value = '[]'
  paramsJson.value = '{}'
  dialogTitle.value = '新增算法'
  dialogVisible.value = true
}

function openEdit(row: SysFeeAlgorithm) {
  Object.assign(form, {
    id: row.id, algoCode: row.algoCode, algoName: row.algoName,
    algoType: row.algoType, calcMode: row.calcMode, formula: row.formula,
    description: row.description, status: row.status,
  })
  variablesJson.value = row.variables ? JSON.stringify(row.variables, null, 2) : '[]'
  paramsJson.value = row.params ? JSON.stringify(row.params, null, 2) : '{}'
  dialogTitle.value = '编辑算法'
  dialogVisible.value = true
}

function parseJson<T>(str: string, fallback: T): T {
  try { return JSON.parse(str) } catch { return fallback }
}

async function doSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const data: FeeAlgorithmCreateDTO = {
      ...form,
      variables: parseJson(variablesJson.value, []),
      params: parseJson(paramsJson.value, {}),
    }
    form.id ? await algorithmApi.update(form.id, data) : await algorithmApi.create(data)
    ElMessage.success('操作成功')
    dialogVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
}

async function doChangeStatus(row: SysFeeAlgorithm, status: number) {
  try {
    await algorithmApi.changeStatus(row.id, status)
    ElMessage.success(status === 1 ? '已启用' : '已停用')
  } catch {
    row.status = status === 1 ? 0 : 1
  }
}

async function doDelete(id: number) {
  await ElMessageBox.confirm('确认删除该算法？', '警告', { type: 'warning' })
  await algorithmApi.delete(id)
  ElMessage.success('删除成功')
  loadList()
}

// ── 试算弹窗 ──────────────────────────────────────────────
const testerVisible = ref(false)
const testerAlgo = ref<SysFeeAlgorithm | null>(null)
const testerVariables = ref<AlgoVariable[]>([])
const testerInputs = reactive<Record<string, string>>({})
const calcLoading = ref(false)
const calcResult = ref<CalcTestResultVO | null>(null)

function openTester(row: SysFeeAlgorithm) {
  testerAlgo.value = row
  testerVariables.value = Array.isArray(row.variables) ? row.variables : []
  // 预填默认值
  Object.keys(testerInputs).forEach(k => delete testerInputs[k])
  testerVariables.value.forEach(v => {
    testerInputs[v.key] = v.defaultValue ?? ''
  })
  calcResult.value = null
  testerVisible.value = true
}

async function doCalc() {
  if (!testerAlgo.value) return
  calcLoading.value = true
  try {
    const res: any = await algorithmApi.testCalc({
      algoId: testerAlgo.value.id,
      inputs: { ...testerInputs },
    })
    calcResult.value = res as CalcTestResultVO
  } finally {
    calcLoading.value = false
  }
}

onMounted(loadList)
</script>

<style scoped lang="scss">
.mt-16 { margin-top: 16px; }
.mb-8  { margin-bottom: 8px; }
.mb-16 { margin-bottom: 16px; }
.mt-8  { margin-top: 8px; }

.formula-code {
  font-family: 'Courier New', monospace;
  font-size: 12px;
  color: #c0392b;
  background: #fef9f9;
  padding: 2px 6px;
  border-radius: 3px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  padding-left: 8px;
  border-left: 3px solid #409eff;
}

.no-variables {
  color: #909399;
  font-size: 13px;
  text-align: center;
  padding: 16px 0;
}

.tester-actions {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.var-unit {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}

.calc-result {
  .result-amount {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;
    .result-label { color: #909399; font-size: 13px; width: 60px; }
    .result-value { font-size: 28px; font-weight: 700; color: #e6a23c; }
  }
  .result-formula {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    .result-label { color: #909399; font-size: 13px; width: 60px; flex-shrink: 0; margin-top: 4px; }
  }
}

.calc-detail {
  font-family: monospace;
  font-size: 12px;
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap;
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  margin: 0;
}
</style>
