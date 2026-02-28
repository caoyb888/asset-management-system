<template>
  <div class="form-page">
    <el-card shadow="never" class="form-card">
      <div class="card-header">
        <el-button :icon="ArrowLeft" text @click="router.back()">返回</el-button>
        <span class="header-title">{{ isEdit ? '编辑开业审批' : '新增开业审批' }}</span>
        <div />
      </div>

      <!-- 审批进度步骤条 -->
      <el-steps :active="approvalStep" align-center class="approval-steps">
        <el-step title="草稿" description="单据已创建" />
        <el-step title="审批中" description="等待审批结果" />
        <el-step
          :title="currentStatus === 3 ? '已驳回' : '审批通过'"
          :status="currentStatus === 3 ? 'error' : undefined"
          description="审批流程完成"
        />
      </el-steps>

      <el-tabs v-model="activeTab">

        <!-- ===== Tab 1: 基本信息 ===== -->
        <el-tab-pane label="基本信息" name="basic">

          <!-- 驳回提示：不可修改原单，需重新创建 -->
          <el-alert
            v-if="currentStatus === 3"
            type="error"
            :closable="false"
            show-icon
            class="reject-alert"
          >
            <template #title>
              <strong>本单已被驳回，不可修改原单</strong>
            </template>
            <template #default>
              请基于当前单据重新创建一份新的开业审批，补充或修改相关信息后再次提交。
              <el-button
                type="danger" plain size="small" style="margin-left: 12px"
                :loading="creatingFromPrev" @click="handleCreateFromPrevious"
              >基于历史单据创建</el-button>
            </template>
          </el-alert>

          <el-form ref="formRef" :model="form" label-width="120px" class="form-section">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="关联合同" prop="contractId"
                  :rules="[{ required: true, message: '请选择合同', trigger: 'change' }]">
                  <el-select
                    v-model="form.contractId"
                    filterable remote clearable :remote-method="searchContracts"
                    placeholder="请搜索合同编号" style="width: 100%"
                    :disabled="isReadonly || currentStatus === 3"
                    @change="onContractChange"
                  >
                    <el-option
                      v-for="c in contractOptions"
                      :key="c.id"
                      :label="`${c.contractCode} - ${c.contractName}`"
                      :value="c.id"
                    />
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
              <el-col :span="12">
                <el-form-item label="计划开业日" prop="plannedOpeningDate"
                  :rules="[{ required: true, message: '请选择计划开业日期', trigger: 'change' }]">
                  <el-date-picker
                    v-model="form.plannedOpeningDate" type="date"
                    value-format="YYYY-MM-DD" style="width: 100%"
                    :disabled="isReadonly || currentStatus === 3"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="实际开业日">
                  <el-date-picker
                    v-model="form.actualOpeningDate" type="date"
                    value-format="YYYY-MM-DD" style="width: 100%"
                    :disabled="isReadonly || currentStatus === 3"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="备注">
                  <el-input
                    v-model="form.remark" type="textarea" :rows="3"
                    placeholder="备注说明" maxlength="500" show-word-limit
                    :disabled="isReadonly || currentStatus === 3"
                  />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>

          <div class="tab-actions" v-if="!isReadonly && currentStatus !== 3">
            <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
            <el-button
              v-if="recordId && currentStatus === 0"
              type="warning" @click="approvalDialogVisible = true"
            >提交审批</el-button>
          </div>

          <div v-if="currentStatus === 1" class="mock-approval">
            <el-divider>Mock 审批操作（测试用）</el-divider>
            <el-button type="success" @click="mockApprove(true)">模拟审批通过</el-button>
            <el-button type="danger" @click="mockApprove(false)">模拟审批驳回</el-button>
          </div>
        </el-tab-pane>

        <!-- ===== Tab 2: 审批材料 ===== -->
        <el-tab-pane label="审批材料" name="attachments">
          <div v-if="!isReadonly && recordId && currentStatus === 0" class="toolbar">
            <el-button type="primary" plain size="small" :icon="Plus" @click="showAddAttachment = true">
              添加材料
            </el-button>
          </div>

          <el-table :data="attachments" border stripe>
            <el-table-column prop="fileName" label="文件名" />
            <el-table-column prop="fileType" label="类型" width="100" />
            <el-table-column label="大小" width="120">
              <template #default="{ row }">
                {{ row.fileSize ? `${(row.fileSize / 1024).toFixed(1)} KB` : '-' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" align="center">
              <template #default="{ row }">
                <el-button link type="primary" @click="previewFile(row.fileUrl)">预览</el-button>
                <el-button
                  v-if="!isReadonly && currentStatus === 0"
                  link type="danger" @click="handleDeleteAttachment(row)"
                >删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="attachments.length === 0" description="暂无审批材料" :image-size="60" />
        </el-tab-pane>

        <!-- ===== Tab 3: 审批流程 ===== -->
        <el-tab-pane label="审批流程" name="timeline">
          <ApprovalTimeline :current-status="currentStatus" style="max-width: 600px; padding: 8px 4px;" />
        </el-tab-pane>

      </el-tabs>
    </el-card>

    <!-- 审批发起弹窗 -->
    <ApprovalDialog
      v-model:visible="approvalDialogVisible"
      title="提交开业审批"
      :loading="submitting"
      @confirm="onApprovalConfirm"
    />

    <!-- 添加附件 Dialog -->
    <el-dialog v-model="showAddAttachment" title="添加审批材料" width="480px">
      <el-form :model="attachForm" label-width="80px">
        <el-form-item label="文件名">
          <el-input v-model="attachForm.fileName" placeholder="如：营业执照.pdf" />
        </el-form-item>
        <el-form-item label="文件地址">
          <el-input v-model="attachForm.fileUrl" placeholder="请填写文件URL" />
        </el-form-item>
        <el-form-item label="文件类型">
          <el-select v-model="attachForm.fileType" style="width: 100%">
            <el-option label="PDF" value="pdf" />
            <el-option label="图片" value="image" />
            <el-option label="Word" value="doc" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddAttachment = false">取消</el-button>
        <el-button type="primary" :loading="attachSaving" @click="handleAddAttachment">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'

import ApprovalDialog from '@/components/inv/ApprovalDialog.vue'
import ApprovalTimeline from '@/components/inv/ApprovalTimeline.vue'
import { getContractPage, type ContractVO } from '@/api/inv/contract'
import {
  createOpeningApproval, updateOpeningApproval, getOpeningApprovalDetail,
  submitOpeningApproval, openingApprovalCallback, createFromPreviousApproval,
  getOpeningAttachments, addOpeningAttachment, deleteOpeningAttachment,
  type OpeningApprovalVO, type OpeningAttachmentVO,
} from '@/api/inv/openingApproval'

const route = useRoute()
const router = useRouter()
const routeId = computed(() => route.query.id ? Number(route.query.id) : null)
const isEdit = computed(() => !!routeId.value)
const isReadonly = computed(() => route.query.readonly === '1')
const activeTab = ref('basic')

const recordId = ref<number | null>(null)
const currentStatus = ref(0)
const previousApprovalId = ref<number | null>(null)
const saving = ref(false)
const submitting = ref(false)
const attachSaving = ref(false)
const creatingFromPrev = ref(false)
const showAddAttachment = ref(false)
const approvalDialogVisible = ref(false)

// 审批进度步骤（0=草稿, 1=审批中, 2=已完成）
const approvalStep = computed(() => {
  if (currentStatus.value <= 0) return 0
  if (currentStatus.value === 1) return 1
  return 2
})

const formRef = ref<FormInstance>()
const form = ref({
  contractId: null as number | null,
  projectId: null as number | null,
  merchantId: null as number | null,
  plannedOpeningDate: '',
  actualOpeningDate: '',
  remark: '',
})

const contractOptions = ref<ContractVO[]>([])
const attachments = ref<OpeningAttachmentVO[]>([])

const attachForm = reactive({
  fileName: '',
  fileUrl: '',
  fileType: 'pdf',
})

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

// ─── 合同搜索 ─────────────────────────────────────────────
async function searchContracts(q: string) {
  const res = await getContractPage({ pageNum: 1, pageSize: 20, keyword: q, status: 2 })
  contractOptions.value = res.records ?? []
}

// 选择合同后自动联动填充项目/商家信息
function onContractChange(contractId: number | null) {
  const contract = contractOptions.value.find(c => c.id === contractId)
  if (contract) {
    form.value.projectId = contract.projectId ?? null
    form.value.merchantId = contract.merchantId ?? null
  }
}

// ─── 基于历史驳回单据创建新单 ──────────────────────────────
async function handleCreateFromPrevious() {
  if (!recordId.value) return
  creatingFromPrev.value = true
  try {
    const newId = await createFromPreviousApproval(recordId.value)
    ElMessage.success('已创建新开业审批单，正在跳转编辑...')
    router.push(`/inv/opening-approvals/form?id=${newId}`)
  } catch (err: unknown) {
    ElMessage.error((err as { message?: string })?.message || '创建失败')
  } finally {
    creatingFromPrev.value = false
  }
}

// ─── 保存 ─────────────────────────────────────────────────
async function handleSave() {
  const valid = await formRef.value?.validate().then(() => true).catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const dto = {
      contractId: form.value.contractId ?? undefined,
      projectId: form.value.projectId ?? undefined,
      merchantId: form.value.merchantId ?? undefined,
      plannedOpeningDate: form.value.plannedOpeningDate || undefined,
      actualOpeningDate: form.value.actualOpeningDate || undefined,
      remark: form.value.remark || undefined,
    }
    if (!recordId.value) {
      recordId.value = await createOpeningApproval(dto)
      ElMessage.success('开业审批已创建')
    } else {
      await updateOpeningApproval(recordId.value, dto)
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
  submitting.value = true
  try {
    await submitOpeningApproval(recordId.value)
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
    await openingApprovalCallback(recordId.value, approved)
    currentStatus.value = approved ? 2 : 3
    ElMessage.success(approved ? '审批通过' : '已驳回')
  } catch (err: unknown) {
    ElMessage.error((err as { message?: string })?.message || '操作失败')
  }
}

// ─── 附件管理 ─────────────────────────────────────────────
async function loadAttachments() {
  if (!recordId.value) return
  attachments.value = await getOpeningAttachments(recordId.value)
}

async function handleAddAttachment() {
  if (!recordId.value) return
  if (!attachForm.fileName || !attachForm.fileUrl) {
    ElMessage.warning('请填写文件名和文件地址')
    return
  }
  attachSaving.value = true
  try {
    await addOpeningAttachment(recordId.value, { ...attachForm })
    showAddAttachment.value = false
    Object.assign(attachForm, { fileName: '', fileUrl: '', fileType: 'pdf' })
    await loadAttachments()
    ElMessage.success('添加成功')
  } catch (err: unknown) {
    ElMessage.error((err as { message?: string })?.message || '添加失败')
  } finally {
    attachSaving.value = false
  }
}

async function handleDeleteAttachment(row: OpeningAttachmentVO) {
  await ElMessageBox.confirm(`确认删除「${row.fileName}」？`, '删除确认', { type: 'warning' })
  await deleteOpeningAttachment(row.id)
  await loadAttachments()
  ElMessage.success('已删除')
}

function previewFile(url: string) {
  if (url) window.open(url, '_blank')
  else ElMessage.warning('文件地址无效')
}

// ─── 初始化 ───────────────────────────────────────────────
async function loadData(id: number) {
  const detail: OpeningApprovalVO = await getOpeningApprovalDetail(id)
  currentStatus.value = detail.status ?? 0
  previousApprovalId.value = detail.previousApprovalId ?? null
  form.value.contractId = detail.contractId ?? null
  form.value.projectId = detail.projectId ?? null
  form.value.merchantId = detail.merchantId ?? null
  form.value.plannedOpeningDate = detail.plannedOpeningDate
    ? String(detail.plannedOpeningDate) : ''
  form.value.actualOpeningDate = detail.actualOpeningDate
    ? String(detail.actualOpeningDate) : ''
  form.value.remark = detail.remark ?? ''
  await loadAttachments()
}

onMounted(async () => {
  await searchContracts('')
  if (isEdit.value && routeId.value) {
    recordId.value = routeId.value
    await loadData(routeId.value)
  }
})
</script>

<style scoped>
.form-page { padding-bottom: 24px; }
.form-card { border-radius: 12px !important; border: 1px solid rgba(0, 0, 0, 0.06) !important; }
.card-header { display: flex; justify-content: space-between; align-items: center; padding: 4px 0; }
.card-header .header-title { font-size: 16px; font-weight: 600; color: #1e293b; }
.form-section { max-width: 900px; }
.toolbar { margin-bottom: 10px; }
.tab-actions {
  display: flex; align-items: center; gap: 12px;
  margin-top: 16px; padding-top: 16px; border-top: 1px solid #f0f0f0;
}
.mock-approval { margin-top: 20px; text-align: center; }
.approval-steps {
  padding: 20px 40px 16px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 4px;
}
.reject-alert {
  margin-bottom: 16px;
  max-width: 900px;
}
</style>
