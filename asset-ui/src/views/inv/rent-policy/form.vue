<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-button :icon="ArrowLeft" text @click="router.back()">返回</el-button>
          <span>{{ isEdit ? '编辑租决政策' : '新增租决政策' }}</span>
          <div />
        </div>
      </template>

      <el-tabs v-model="activeTab">

        <!-- ===== Tab 1: 政策配置 ===== -->
        <el-tab-pane label="政策配置" name="policy">
          <el-form ref="formRef" :model="form" label-width="140px" class="form-section">
            <el-divider content-position="left">基础设置</el-divider>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="所属项目" prop="projectId"
                  :rules="[{ required: true, message: '请选择项目', trigger: 'change' }]">
                  <el-select
                    v-model="form.projectId"
                    filterable remote :remote-method="searchProjects"
                    placeholder="请搜索项目" style="width: 100%"
                    :disabled="isReadonly"
                  >
                    <el-option v-for="p in projectOptions" :key="p.id" :label="p.projectName" :value="p.id" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="政策类型">
                  <el-select v-model="form.policyType" style="width: 100%" :disabled="isReadonly">
                    <el-option :value="1" label="标准政策" />
                    <el-option :value="2" label="临时政策" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="最短租期(月)">
                  <el-input-number v-model="form.minLeaseTerm" :min="1" :max="240"
                    controls-position="right" style="width: 100%" :disabled="isReadonly" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="最长租期(月)">
                  <el-input-number v-model="form.maxLeaseTerm" :min="1" :max="240"
                    controls-position="right" style="width: 100%" :disabled="isReadonly" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="付款周期">
                  <el-select v-model="form.paymentCycle" style="width: 100%" clearable :disabled="isReadonly">
                    <el-option :value="1" label="月付" />
                    <el-option :value="3" label="季付" />
                    <el-option :value="6" label="半年付" />
                    <el-option :value="12" label="年付" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="状态">
                  <el-tag :type="statusTagType(currentStatus)">
                    {{ statusLabel(currentStatus) }}
                  </el-tag>
                </el-form-item>
              </el-col>
            </el-row>

            <el-divider content-position="left">租金标准（元/㎡/月）</el-divider>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="第1年租金">
                  <el-input-number v-model="form.year1Rent" :precision="2" :min="0"
                    controls-position="right" style="width: 100%" :disabled="isReadonly" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="第2年租金">
                  <el-input-number v-model="form.year2Rent" :precision="2" :min="0"
                    controls-position="right" style="width: 100%" :disabled="isReadonly" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="第1年物管费">
                  <el-input-number v-model="form.year1PropertyFee" :precision="2" :min="0"
                    controls-position="right" style="width: 100%" :disabled="isReadonly" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="第2年物管费">
                  <el-input-number v-model="form.year2PropertyFee" :precision="2" :min="0"
                    controls-position="right" style="width: 100%" :disabled="isReadonly" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-divider content-position="left">递增与优惠</el-divider>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="租金递增率(%)">
                  <el-input-number v-model="form.rentGrowthRate" :precision="2" :min="0" :max="100"
                    controls-position="right" style="width: 100%" :disabled="isReadonly" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="物管费递增率(%)">
                  <el-input-number v-model="form.feeGrowthRate" :precision="2" :min="0" :max="100"
                    controls-position="right" style="width: 100%" :disabled="isReadonly" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="免租期(月)">
                  <el-input-number v-model="form.freeRentPeriod" :min="0" :max="36"
                    controls-position="right" style="width: 100%" :disabled="isReadonly" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="保证金(月)">
                  <el-input-number v-model="form.depositMonths" :min="0" :max="24"
                    controls-position="right" style="width: 100%" :disabled="isReadonly" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>

          <div class="tab-actions" v-if="!isReadonly">
            <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
            <el-button
              v-if="recordId && (currentStatus === 0 || currentStatus === 3)"
              type="warning" @click="approvalDialogVisible = true"
            >提交审批</el-button>
          </div>

          <div v-if="currentStatus === 1" class="mock-approval">
            <el-divider>Mock 审批操作（测试用）</el-divider>
            <el-button type="success" @click="mockApprove(true)">模拟审批通过</el-button>
            <el-button type="danger" @click="mockApprove(false)">模拟审批驳回</el-button>
          </div>
        </el-tab-pane>

        <!-- ===== Tab 3: 审批流程 ===== -->
        <el-tab-pane label="审批流程" name="timeline">
          <ApprovalTimeline :current-status="currentStatus" />
        </el-tab-pane>

        <!-- ===== Tab 2: 分类指标 ===== -->
        <el-tab-pane label="分类指标" name="indicators">
          <el-alert
            type="info" :closable="false"
            title="分类指标：按商铺类别（主力店/次主力店/一般商铺）分别设定租金单价、物管费等细化标准"
            style="margin-bottom: 16px"
          />
          <div v-if="!isReadonly && recordId && currentStatus !== 1" class="toolbar">
            <el-button type="primary" plain size="small" :icon="Plus" @click="addIndicatorRow">
              添加指标行
            </el-button>
          </div>

          <el-table :data="indicatorRows" border style="width: 100%">
            <el-table-column label="商铺类别" width="130">
              <template #default="{ row }">
                <el-select v-model="row.shopCategory" style="width: 100%"
                  :disabled="isReadonly || currentStatus === 1">
                  <el-option :value="1" label="主力店" />
                  <el-option :value="2" label="次主力店" />
                  <el-option :value="3" label="一般商铺" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="租金单价(元/㎡/月)" width="160">
              <template #default="{ row }">
                <el-input-number v-model="row.rentPrice" :precision="2" :min="0"
                  controls-position="right" style="width: 100%"
                  :disabled="isReadonly || currentStatus === 1" />
              </template>
            </el-table-column>
            <el-table-column label="物管费单价(元/㎡/月)" width="170">
              <template #default="{ row }">
                <el-input-number v-model="row.propertyFeePrice" :precision="2" :min="0"
                  controls-position="right" style="width: 100%"
                  :disabled="isReadonly || currentStatus === 1" />
              </template>
            </el-table-column>
            <el-table-column label="业态" width="120">
              <template #default="{ row }">
                <el-input v-model="row.formatType" placeholder="业态类型"
                  :disabled="isReadonly || currentStatus === 1" />
              </template>
            </el-table-column>
            <el-table-column label="免租(月)" width="100">
              <template #default="{ row }">
                <el-input-number v-model="row.freeRentMonths" :min="0" :max="36"
                  controls-position="right" style="width: 100%"
                  :disabled="isReadonly || currentStatus === 1" />
              </template>
            </el-table-column>
            <el-table-column label="押金(月)" width="100">
              <template #default="{ row }">
                <el-input-number v-model="row.depositMonths" :min="0" :max="24"
                  controls-position="right" style="width: 100%"
                  :disabled="isReadonly || currentStatus === 1" />
              </template>
            </el-table-column>
            <el-table-column v-if="!isReadonly && currentStatus !== 1" label="操作" width="65" align="center">
              <template #default="{ $index }">
                <el-button type="danger" link :icon="Delete" @click="indicatorRows.splice($index, 1)" />
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="indicatorRows.length === 0" description="暂无分类指标，请添加" :image-size="60" />

          <div class="tab-actions" v-if="!isReadonly && recordId && currentStatus !== 1">
            <el-button type="primary" :loading="indicatorSaving" @click="handleSaveIndicators">
              保存指标
            </el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>

  <!-- 审批发起弹窗 -->
  <ApprovalDialog
    v-model:visible="approvalDialogVisible"
    title="提交租决政策审批"
    :loading="submitting"
    @confirm="onApprovalConfirm"
  />
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Plus, Delete } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'

import type { ProjectVO } from '@/api/base/project'
import { getProjectPage } from '@/api/base/project'
import {
  createRentPolicy, updateRentPolicy, getRentPolicyDetail,
  submitRentPolicyApproval, rentPolicyApprovalCallback,
  getPolicyIndicators, savePolicyIndicators,
  type RentPolicyVO, type PolicyIndicatorVO,
} from '@/api/inv/rentPolicy'
import ApprovalDialog from '@/components/inv/ApprovalDialog.vue'
import ApprovalTimeline from '@/components/inv/ApprovalTimeline.vue'

const route = useRoute()
const router = useRouter()
const routeId = computed(() => route.query.id ? Number(route.query.id) : null)
const isEdit = computed(() => !!routeId.value)
const isReadonly = computed(() => route.query.readonly === '1')
const activeTab = ref('policy')

const recordId = ref<number | null>(null)
const currentStatus = ref(0)
const saving = ref(false)
const submitting = ref(false)
const indicatorSaving = ref(false)
const approvalDialogVisible = ref(false)

const formRef = ref<FormInstance>()
const form = ref({
  projectId: null as number | null,
  policyType: 1 as number,
  year1Rent: undefined as number | undefined,
  year2Rent: undefined as number | undefined,
  year1PropertyFee: undefined as number | undefined,
  year2PropertyFee: undefined as number | undefined,
  minLeaseTerm: undefined as number | undefined,
  maxLeaseTerm: undefined as number | undefined,
  rentGrowthRate: undefined as number | undefined,
  feeGrowthRate: undefined as number | undefined,
  freeRentPeriod: undefined as number | undefined,
  depositMonths: undefined as number | undefined,
  paymentCycle: undefined as number | undefined,
})

const projectOptions = ref<ProjectVO[]>([])
const indicatorRows = ref<PolicyIndicatorVO[]>([])

// ─── 状态映射 ─────────────────────────────────────────────
type TagType = 'primary' | 'success' | 'warning' | 'danger' | 'info' | undefined
const STATUS_MAP: Record<number, { label: string; type: TagType }> = {
  0: { label: '草稿', type: 'info' },
  1: { label: '审批中', type: 'warning' },
  2: { label: '已通过', type: 'success' },
  3: { label: '已驳回', type: 'danger' },
}
const statusLabel = (s: number) => STATUS_MAP[s]?.label ?? '-'
const statusTagType = (s: number) => STATUS_MAP[s]?.type ?? 'info'

async function searchProjects(q: string) {
  const res = await getProjectPage({ pageNum: 1, pageSize: 20, projectName: q })
  projectOptions.value = res.records ?? []
}

function addIndicatorRow() {
  indicatorRows.value.push({
    shopCategory: 3,
    rentPrice: 0,
    propertyFeePrice: 0,
    formatType: '',
    rentGrowthRate: 0,
    feeGrowthRate: 0,
    freeRentMonths: 0,
    depositMonths: 3,
  })
}

// ─── 保存政策 ─────────────────────────────────────────────
async function handleSave() {
  const valid = await formRef.value?.validate().then(() => true).catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const dto = { ...form.value, projectId: form.value.projectId ?? undefined }
    if (!recordId.value) {
      recordId.value = await createRentPolicy(dto)
      ElMessage.success('租决政策已创建')
    } else {
      await updateRentPolicy(recordId.value, dto)
      ElMessage.success('保存成功')
    }
  } catch (err: unknown) {
    ElMessage.error((err as { message?: string })?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ─── 提交审批（由 ApprovalDialog confirm 触发） ─────────────
async function onApprovalConfirm(_payload: { approverIds: number[]; comment: string }) {
  if (!recordId.value) return
  // _payload.approverIds / _payload.comment 留给后续审批引擎集成使用
  submitting.value = true
  try {
    await submitRentPolicyApproval(recordId.value)
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
    await rentPolicyApprovalCallback(recordId.value, approved)
    currentStatus.value = approved ? 2 : 3
    ElMessage.success(approved ? '审批通过' : '已驳回')
  } catch (err: unknown) {
    ElMessage.error((err as { message?: string })?.message || '操作失败')
  }
}

// ─── 保存分类指标 ──────────────────────────────────────────
async function handleSaveIndicators() {
  if (!recordId.value) { ElMessage.warning('请先保存政策信息'); return }
  indicatorSaving.value = true
  try {
    await savePolicyIndicators(recordId.value, indicatorRows.value)
    ElMessage.success(`分类指标保存成功，共 ${indicatorRows.value.length} 条`)
  } catch (err: unknown) {
    ElMessage.error((err as { message?: string })?.message || '保存失败')
  } finally {
    indicatorSaving.value = false
  }
}

// ─── 初始化 ───────────────────────────────────────────────
async function loadData(id: number) {
  const [detail, indicators] = await Promise.all([
    getRentPolicyDetail(id),
    getPolicyIndicators(id),
  ])
  const d: RentPolicyVO = detail
  currentStatus.value = d.status ?? 0
  form.value.projectId = d.projectId ?? null
  form.value.policyType = d.policyType ?? 1
  form.value.year1Rent = d.year1Rent
  form.value.year2Rent = d.year2Rent
  form.value.year1PropertyFee = d.year1PropertyFee
  form.value.year2PropertyFee = d.year2PropertyFee
  form.value.minLeaseTerm = d.minLeaseTerm
  form.value.maxLeaseTerm = d.maxLeaseTerm
  form.value.rentGrowthRate = d.rentGrowthRate
  form.value.feeGrowthRate = d.feeGrowthRate
  form.value.freeRentPeriod = d.freeRentPeriod
  form.value.depositMonths = d.depositMonths
  form.value.paymentCycle = d.paymentCycle
  indicatorRows.value = indicators
}

onMounted(async () => {
  await searchProjects('')
  if (isEdit.value && routeId.value) {
    recordId.value = routeId.value
    await loadData(routeId.value)
  }
})
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.form-section { max-width: 960px; }
.toolbar { margin-bottom: 10px; }
.tab-actions {
  display: flex; align-items: center; gap: 12px;
  margin-top: 16px; padding-top: 16px; border-top: 1px solid #f0f0f0;
}
.mock-approval { margin-top: 20px; text-align: center; }
</style>
