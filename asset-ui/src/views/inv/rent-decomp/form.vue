<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-button :icon="ArrowLeft" text @click="router.back()">返回</el-button>
          <span>{{ isEdit ? '编辑租金分解' : '新增租金分解' }}</span>
          <div />
        </div>
      </template>

      <!-- 基础信息 -->
      <el-form ref="formRef" :model="form" label-width="120px" class="base-form">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所属项目" prop="projectId"
              :rules="[{ required: true, message: '请选择项目', trigger: 'change' }]">
              <el-select
                v-model="form.projectId"
                filterable remote :remote-method="searchProjects"
                placeholder="请搜索项目" style="width: 100%"
                :disabled="isReadonly || isEdit"
              >
                <el-option v-for="p in projectOptions" :key="p.id" :label="p.projectName" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="参考租决政策">
              <el-select
                v-model="form.policyId"
                filterable clearable placeholder="可选参考政策"
                style="width: 100%" :disabled="isReadonly"
              >
                <el-option
                  v-for="p in policyOptions"
                  :key="p.id"
                  :label="`${p.policyCode}`"
                  :value="p.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分解编号">
              <el-input :value="decompCode || '（保存后自动生成）'" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="审批状态">
              <el-tag :type="STATUS_MAP[currentStatus]?.type ?? 'info'">
                {{ STATUS_MAP[currentStatus]?.label ?? '-' }}
              </el-tag>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="年度总租金">
              <el-input-number
                :value="totalAnnualRent" disabled
                :precision="2" style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="年度总物管费">
              <el-input-number
                :value="totalAnnualFee" disabled
                :precision="2" style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div class="base-actions" v-if="!isReadonly">
        <el-button type="primary" :loading="saving" @click="handleSave">保存基础信息</el-button>
        <el-button
          v-if="recordId"
          type="success" :loading="calculating" @click="handleCalculate"
        >汇总计算</el-button>
        <el-button
          v-if="recordId && (currentStatus === 0 || currentStatus === 3)"
          type="warning" @click="approvalDialogVisible = true"
        >提交审批</el-button>
      </div>

      <!-- Mock 审批操作（测试用，审批中状态显示） -->
      <div v-if="recordId && currentStatus === 1" class="mock-approval">
        <el-divider>Mock 审批操作（测试用）</el-divider>
        <el-button type="success" @click="mockApprove(true)">模拟审批通过</el-button>
        <el-button type="danger" @click="mockApprove(false)">模拟审批驳回</el-button>
      </div>

      <!-- 明细分类 Tab -->
      <el-divider />
      <div v-if="!recordId" class="no-record-tip">
        <el-alert type="info" :closable="false" title="请先保存基础信息，再录入分类明细" />
      </div>
      <template v-else>
        <el-tabs v-model="activeCategory" type="border-card">
          <el-tab-pane
            v-for="cat in categories"
            :key="cat.value"
            :label="cat.label"
            :name="String(cat.value)"
          >
            <div class="toolbar" v-if="!isReadonly">
              <el-button type="primary" plain size="small" :icon="Plus"
                @click="addDetailRow(cat.value)">
                添加明细
              </el-button>
            </div>
            <el-table :data="detailsByCategory[cat.value] || []" border>
              <el-table-column label="业态" width="140">
                <template #default="{ row }">
                  <el-input v-model="row.formatType" placeholder="业态名称"
                    :disabled="isReadonly" />
                </template>
              </el-table-column>
              <el-table-column label="租金单价(元/㎡/月)" width="160">
                <template #default="{ row }">
                  <el-input-number
                    v-model="row.rentUnitPrice" :precision="2" :min="0"
                    controls-position="right" style="width: 100%"
                    :disabled="isReadonly"
                    @change="calcRow(row)"
                  />
                </template>
              </el-table-column>
              <el-table-column label="物管费单价(元/㎡/月)" width="170">
                <template #default="{ row }">
                  <el-input-number
                    v-model="row.propertyUnitPrice" :precision="2" :min="0"
                    controls-position="right" style="width: 100%"
                    :disabled="isReadonly"
                    @change="calcRow(row)"
                  />
                </template>
              </el-table-column>
              <el-table-column label="面积(㎡)" width="120">
                <template #default="{ row }">
                  <el-input-number
                    v-model="row.area" :precision="2" :min="0"
                    controls-position="right" style="width: 100%"
                    :disabled="isReadonly"
                    @change="calcRow(row)"
                  />
                </template>
              </el-table-column>
              <el-table-column label="年租金(元)" width="130" align="right">
                <template #default="{ row }">
                  <span class="amount-text">{{ fmtAmount(row.annualRent) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="年物管费(元)" width="130" align="right">
                <template #default="{ row }">
                  <span class="amount-text">{{ fmtAmount(row.annualFee) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="备注" min-width="120">
                <template #default="{ row }">
                  <el-input v-model="row.remark" placeholder="备注" :disabled="isReadonly" />
                </template>
              </el-table-column>
              <el-table-column v-if="!isReadonly" label="操作" width="65" align="center">
                <template #default="{ $index }">
                  <el-button type="danger" link :icon="Delete"
                    @click="removeDetailRow(cat.value, $index)" />
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!(detailsByCategory[cat.value]?.length)"
              description="暂无明细，点击「添加明细」录入数据" :image-size="60" />

            <!-- 小计 -->
            <div v-if="detailsByCategory[cat.value]?.length" class="subtotal">
              <span>
                {{ cat.label }}合计：
                年租金 <strong>¥{{ fmtAmount(categorySubtotal(cat.value).rent) }}</strong>
                &nbsp;|&nbsp;
                年物管费 <strong>¥{{ fmtAmount(categorySubtotal(cat.value).fee) }}</strong>
              </span>
            </div>
          </el-tab-pane>
        </el-tabs>

        <div class="tab-actions" v-if="!isReadonly">
          <el-button type="primary" :loading="detailSaving" @click="handleSaveDetails">
            保存分类明细
          </el-button>
          <el-button type="success" :loading="calculating" @click="handleCalculate">
            汇总计算合计
          </el-button>
        </div>
      </template>

      <!-- 审批流程时间线（已保存后显示） -->
      <template v-if="recordId">
        <el-divider content-position="left">审批流程</el-divider>
        <div class="timeline-wrap">
          <ApprovalTimeline :current-status="currentStatus" />
        </div>
      </template>
    </el-card>
  </div>

  <!-- 审批发起弹窗 -->
  <ApprovalDialog
    v-model:visible="approvalDialogVisible"
    title="提交租金分解审批"
    :loading="submitting"
    @confirm="onApprovalConfirm"
  />
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Plus, Delete } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'

import type { ProjectVO } from '@/api/base/project'
import { getProjectPage } from '@/api/base/project'
import { getApprovedPolicies, type RentPolicyVO } from '@/api/inv/rentPolicy'
import {
  createRentDecomp, updateRentDecomp, getRentDecompDetail,
  getRentDecompDetails, saveRentDecompDetails, calculateRentDecomp,
  submitRentDecompApproval, rentDecompApprovalCallback,
  type RentDecompVO, type RentDecompDetailVO,
} from '@/api/inv/rentDecomp'
import ApprovalDialog from '@/components/inv/ApprovalDialog.vue'
import ApprovalTimeline from '@/components/inv/ApprovalTimeline.vue'

const route = useRoute()
const router = useRouter()
const routeId = computed(() => route.query.id ? Number(route.query.id) : null)
const isEdit = computed(() => !!routeId.value)
const isReadonly = computed(() => route.query.readonly === '1')

const recordId = ref<number | null>(null)
const decompCode = ref('')
const totalAnnualRent = ref<number | undefined>(undefined)
const totalAnnualFee = ref<number | undefined>(undefined)
const currentStatus = ref(0)
const saving = ref(false)
const detailSaving = ref(false)
const calculating = ref(false)
const submitting = ref(false)
const approvalDialogVisible = ref(false)
const activeCategory = ref('1')

// ─── 状态映射 ─────────────────────────────────────────────
type TagType = 'primary' | 'success' | 'warning' | 'danger' | 'info' | undefined
const STATUS_MAP: Record<number, { label: string; type: TagType }> = {
  0: { label: '草稿', type: 'info' },
  1: { label: '审批中', type: 'warning' },
  2: { label: '已通过', type: 'success' },
  3: { label: '已驳回', type: 'danger' },
}

const formRef = ref<FormInstance>()
const form = ref({
  projectId: null as number | null,
  policyId: null as number | null,
})

const projectOptions = ref<ProjectVO[]>([])
const policyOptions = ref<RentPolicyVO[]>([])

// 三类商铺
const categories = [
  { value: 1, label: '主力店' },
  { value: 2, label: '次主力店' },
  { value: 3, label: '一般商铺' },
]

// 按类别分组的明细行（响应式对象）
const detailsByCategory = reactive<Record<number, RentDecompDetailVO[]>>({
  1: [], 2: [], 3: [],
})

// ─── 工具函数 ─────────────────────────────────────────────
function fmtAmount(n: number | null | undefined) {
  if (n == null) return '-'
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function calcRow(row: RentDecompDetailVO) {
  const rent = (row.rentUnitPrice ?? 0) * (row.area ?? 0) * 12
  const fee = (row.propertyUnitPrice ?? 0) * (row.area ?? 0) * 12
  row.annualRent = Math.round(rent * 100) / 100
  row.annualFee = Math.round(fee * 100) / 100
}

function categorySubtotal(cat: number) {
  const rows = detailsByCategory[cat] ?? []
  return {
    rent: rows.reduce((s, r) => s + (r.annualRent ?? 0), 0),
    fee: rows.reduce((s, r) => s + (r.annualFee ?? 0), 0),
  }
}

function addDetailRow(category: number) {
  if (!detailsByCategory[category]) detailsByCategory[category] = []
  detailsByCategory[category].push({
    shopCategory: category,
    formatType: '',
    rentUnitPrice: 0,
    propertyUnitPrice: 0,
    area: 0,
    annualRent: 0,
    annualFee: 0,
    remark: '',
  })
}

function removeDetailRow(category: number, index: number) {
  detailsByCategory[category]?.splice(index, 1)
}

// ─── 远程搜索 ─────────────────────────────────────────────
async function searchProjects(q: string) {
  const res = await getProjectPage({ pageNum: 1, pageSize: 20, projectName: q })
  projectOptions.value = res.records ?? []
}

// ─── 保存基础信息 ─────────────────────────────────────────
async function handleSave() {
  const valid = await formRef.value?.validate().then(() => true).catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const dto = {
      projectId: form.value.projectId ?? undefined,
      policyId: form.value.policyId ?? undefined,
    }
    if (!recordId.value) {
      recordId.value = await createRentDecomp(dto)
      // 重新加载获取编号
      const detail: RentDecompVO = await getRentDecompDetail(recordId.value)
      decompCode.value = detail.decompCode
      ElMessage.success('租金分解已创建')
    } else {
      await updateRentDecomp(recordId.value, dto)
      ElMessage.success('保存成功')
    }
  } catch (err: unknown) {
    ElMessage.error((err as { message?: string })?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ─── 保存明细 ─────────────────────────────────────────────
async function handleSaveDetails() {
  if (!recordId.value) { ElMessage.warning('请先保存基础信息'); return }
  const allDetails: RentDecompDetailVO[] = [
    ...detailsByCategory[1],
    ...detailsByCategory[2],
    ...detailsByCategory[3],
  ]
  detailSaving.value = true
  try {
    await saveRentDecompDetails(recordId.value, allDetails)
    ElMessage.success(`明细保存成功，共 ${allDetails.length} 条`)
  } catch (err: unknown) {
    ElMessage.error((err as { message?: string })?.message || '保存失败')
  } finally {
    detailSaving.value = false
  }
}

// ─── 汇总计算 ─────────────────────────────────────────────
async function handleCalculate() {
  if (!recordId.value) { ElMessage.warning('请先保存基础信息'); return }
  calculating.value = true
  try {
    const result = await calculateRentDecomp(recordId.value)
    totalAnnualRent.value = result.totalAnnualRent
    totalAnnualFee.value = result.totalAnnualFee
    ElMessage.success(
      `汇总计算完成：年总租金 ¥${fmtAmount(result.totalAnnualRent)}，年总物管费 ¥${fmtAmount(result.totalAnnualFee)}`
    )
  } catch (err: unknown) {
    ElMessage.error((err as { message?: string })?.message || '计算失败')
  } finally {
    calculating.value = false
  }
}

// ─── 提交审批（由 ApprovalDialog confirm 触发） ─────────────
async function onApprovalConfirm(_payload: { approverIds: number[]; comment: string }) {
  if (!recordId.value) return
  submitting.value = true
  try {
    await submitRentDecompApproval(recordId.value)
    currentStatus.value = 1
    approvalDialogVisible.value = false
    ElMessage.success('已成功提交审批')
  } catch (err: unknown) {
    ElMessage.error((err as { message?: string })?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

// ─── Mock 审批 ────────────────────────────────────────────
async function mockApprove(approved: boolean) {
  if (!recordId.value) return
  try {
    await rentDecompApprovalCallback(recordId.value, approved)
    currentStatus.value = approved ? 2 : 3
    ElMessage.success(approved ? '审批通过' : '已驳回')
  } catch (err: unknown) {
    ElMessage.error((err as { message?: string })?.message || '操作失败')
  }
}

// ─── 同步明细到分组 ──────────────────────────────────────
function syncDetails(details: RentDecompDetailVO[]) {
  detailsByCategory[1] = []
  detailsByCategory[2] = []
  detailsByCategory[3] = []
  for (const d of details) {
    const cat = d.shopCategory ?? 3
    if (!detailsByCategory[cat]) detailsByCategory[cat] = []
    detailsByCategory[cat].push({ ...d })
  }
}

// ─── 初始化 ───────────────────────────────────────────────
async function loadData(id: number) {
  const [detail, details, policies] = await Promise.all([
    getRentDecompDetail(id),
    getRentDecompDetails(id),
    getApprovedPolicies(),
  ])
  const d: RentDecompVO = detail
  decompCode.value = d.decompCode
  form.value.projectId = d.projectId ?? null
  form.value.policyId = d.policyId ?? null
  totalAnnualRent.value = d.totalAnnualRent
  totalAnnualFee.value = d.totalAnnualFee
  currentStatus.value = d.status ?? 0
  policyOptions.value = policies
  syncDetails(details)
}

onMounted(async () => {
  const [, policies] = await Promise.all([
    searchProjects(''),
    getApprovedPolicies(),
  ])
  policyOptions.value = policies
  if (isEdit.value && routeId.value) {
    recordId.value = routeId.value
    await loadData(routeId.value)
  }
})
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.base-form { max-width: 960px; margin-bottom: 0; }
.base-actions {
  display: flex; gap: 12px;
  margin: 12px 0 0; padding-top: 12px; border-top: 1px solid #f0f0f0;
  max-width: 960px;
}
.toolbar { margin-bottom: 10px; }
.tab-actions {
  display: flex; align-items: center; gap: 12px;
  margin-top: 16px; padding-top: 16px; border-top: 1px solid #f0f0f0;
}
.subtotal {
  margin-top: 12px; padding: 10px 16px;
  background: #f8f9fa; border-radius: 4px;
  font-size: 14px; color: #606266;
}
.subtotal strong { color: #e6a23c; font-size: 15px; }
.amount-text { color: #303133; font-weight: 500; }
.no-record-tip { margin: 20px 0; }
.mock-approval { margin-top: 20px; text-align: center; }
.timeline-wrap { max-width: 600px; padding: 0 4px 16px; }
</style>
