<template>
  <div class="page-container">
    <el-page-header @back="router.back()" style="margin-bottom:16px">
      <template #content>
        <span class="page-title">变更详情 · {{ detail?.changeCode }}</span>
      </template>
      <template #extra>
        <el-space>
          <el-button
            v-if="detail?.status === 0 || detail?.status === 3"
            @click="goEdit"
          >编辑</el-button>
          <el-button
            v-if="detail?.status === 0"
            type="primary"
            @click="handleSubmitApproval"
          >提交审批</el-button>
          <el-button
            v-if="detail?.status === 1"
            type="success"
            @click="openCallback(2)"
          >审批通过</el-button>
          <el-button
            v-if="detail?.status === 1"
            type="danger" plain
            @click="openCallback(3)"
          >驳回</el-button>
        </el-space>
      </template>
    </el-page-header>

    <el-tabs v-model="activeTab" type="border-card" v-loading="loading">
      <!-- 基本信息 -->
      <el-tab-pane label="变更信息" name="info">
        <el-descriptions :column="3" border label-width="130px" size="small">
          <el-descriptions-item label="变更单号">{{ detail?.changeCode }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(detail?.status)" size="small">{{ detail?.statusName }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="生效日期">{{ detail?.effectiveDate }}</el-descriptions-item>
          <el-descriptions-item label="合同编号">{{ detail?.contractCode }}</el-descriptions-item>
          <el-descriptions-item label="合同名称">{{ detail?.contractName }}</el-descriptions-item>
          <el-descriptions-item label="商家名称">{{ detail?.merchantName }}</el-descriptions-item>
          <el-descriptions-item label="变更类型" :span="3">
            <el-space wrap>
              <el-tag
                v-for="(code, i) in (detail?.changeTypeCodes || [])"
                :key="code"
                :type="typeTagType(code)"
                size="small"
              >{{ detail?.changeTypeNames?.[i] || code }}</el-tag>
            </el-space>
          </el-descriptions-item>
          <el-descriptions-item label="变更原因" :span="3">{{ detail?.reason || '—' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDt(detail?.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="审批实例ID">{{ detail?.approvalId || '—' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 审批时间线 -->
        <div style="margin-top: 24px;">
          <div class="section-title">审批进度</div>
          <ApprovalTimeline
            :records="approvalRecords"
            :current-status="detail?.status ?? 0"
          />
        </div>
      </el-tab-pane>

      <!-- 字段明细对比 -->
      <el-tab-pane label="字段变更明细" name="details">
        <el-empty v-if="!detail?.details?.length" description="暂无字段明细" />
        <el-table v-else :data="detail.details" border size="small">
          <el-table-column prop="fieldLabel" label="变更字段" width="180" />
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
          <el-table-column prop="dataType" label="数据类型" width="100" align="center" />
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 审批回调弹窗 -->
    <el-dialog v-model="showCallbackDialog" :title="callbackStatus === 2 ? '确认审批通过' : '确认审批驳回'"
      width="420px" :close-on-click-modal="false">
      <el-form :model="callbackForm" label-width="90px">
        <el-form-item label="审批意见">
          <el-input v-model="callbackForm.comment" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCallbackDialog = false">取消</el-button>
        <el-button
          :type="callbackStatus === 2 ? 'primary' : 'danger'"
          :loading="submitting"
          @click="submitCallback"
        >确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getChangeById,
  submitChangeApproval,
  approvalCallback,
  type ChangeDetailVO,
} from '@/api/opr/change'
import ApprovalTimeline, { type ApprovalRecord } from '@/components/inv/ApprovalTimeline.vue'

const router = useRouter()
const route = useRoute()
const id = Number(route.params.id)

const loading = ref(false)
const detail = ref<ChangeDetailVO | null>(null)
const activeTab = ref('info')
const showCallbackDialog = ref(false)
const submitting = ref(false)
const callbackStatus = ref<2 | 3>(2)
const callbackForm = reactive({ comment: '' })

/** 根据变更状态派生审批时间线记录 */
const approvalRecords = computed<ApprovalRecord[]>(() => {
  if (!detail.value) return []
  const records: ApprovalRecord[] = []
  records.push({ action: 'create', operator: detail.value.createdBy as string | undefined, time: detail.value.createdAt })
  if (detail.value.status >= 1) {
    records.push({ action: 'submit', time: detail.value.updatedAt })
  }
  if (detail.value.status === 2) {
    records.push({ action: 'approve', comment: detail.value.approvalId ? '审批已通过' : undefined })
  } else if (detail.value.status === 3) {
    records.push({ action: 'reject', comment: '审批已驳回' })
  }
  return records
})

type TagType = 'success' | 'primary' | 'warning' | 'info' | 'danger' | undefined
const TYPE_TAG_MAP: Record<string, TagType> = {
  RENT: 'danger', FEE: 'warning', TERM: 'primary', AREA: 'success',
  BRAND: 'info', TENANT: 'info', COMPANY: 'info', CLAUSE: 'info',
}
function typeTagType(code: string): TagType { return TYPE_TAG_MAP[code] }
function statusTagType(s?: number): TagType {
  const m: Record<number, TagType> = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
  return s != null ? m[s] : undefined
}
function formatDt(v?: string): string { return v ? v.replace('T', ' ').substring(0, 16) : '—' }

async function loadDetail() {
  loading.value = true
  try {
    const res = await getChangeById(id)
    detail.value = res.data
  } finally {
    loading.value = false
  }
}

function goEdit() { router.push(`/opr/contract-changes/form?id=${id}`) }

async function handleSubmitApproval() {
  await submitChangeApproval(id)
  ElMessage.success('已提交审批')
  loadDetail()
}

function openCallback(status: 2 | 3) {
  callbackStatus.value = status
  callbackForm.comment = ''
  showCallbackDialog.value = true
}
async function submitCallback() {
  submitting.value = true
  try {
    await approvalCallback(id, { status: callbackStatus.value, comment: callbackForm.comment })
    ElMessage.success(callbackStatus.value === 2 ? '审批通过，应收重算已触发' : '审批已驳回')
    showCallbackDialog.value = false
    loadDetail()
  } finally {
    submitting.value = false
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.page-title { font-weight: 600; font-size: 16px; }
.old-value { color: #f56c6c; text-decoration: line-through; }
.new-value { color: #67c23a; font-weight: 600; }
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  padding-left: 8px;
  border-left: 3px solid #2e75b6;
}
</style>
