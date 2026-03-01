<template>
  <div class="change-page">
    <!-- 搜索栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form :model="queryForm" inline>
        <el-form-item label="变更单号">
          <el-input v-model="queryForm.changeCode" placeholder="请输入变更单号" clearable style="width:160px" />
        </el-form-item>
        <el-form-item label="变更状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width:110px">
            <el-option :value="0" label="草稿" />
            <el-option :value="1" label="审批中" />
            <el-option :value="2" label="已通过" />
            <el-option :value="3" label="已驳回" />
          </el-select>
        </el-form-item>
        <el-form-item label="变更类型">
          <el-select v-model="queryForm.changeTypeCode" placeholder="全部" clearable style="width:140px">
            <el-option v-for="o in CHANGE_TYPE_OPTIONS" :key="o.code" :value="o.code" :label="o.label" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <div class="card-header">
        <div class="header-left">
          <span class="header-title">合同变更列表</span>
          <span class="count-tag">共 {{ total }} 条</span>
        </div>
        <div class="header-actions">
          <el-button type="primary" :icon="Plus" @click="goForm()">新增变更</el-button>
        </div>
      </div>

      <div class="table-body">
      <el-table v-loading="loading" :data="tableData" border stripe style="width:100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="changeCode" label="变更单号" width="160" />
        <el-table-column label="变更类型" min-width="200">
          <template #default="{ row }">
            <el-space wrap>
              <el-tag
                v-for="code in (row.changeTypeCodes || [])"
                :key="code"
                :type="typeTagType(code)"
                size="small"
              >{{ changeTypeLabel(code) }}</el-tag>
              <span v-if="!row.changeTypeCodes?.length" class="text-muted">—</span>
            </el-space>
          </template>
        </el-table-column>
        <el-table-column prop="effectiveDate" label="生效日期" width="110" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="变更原因" min-width="180" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="160" align="center">
          <template #default="{ row }">{{ formatDt(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="goDetail(row.id)">详情</el-button>
            <el-button
              v-if="row.status === 0 || row.status === 3"
              type="warning" link size="small"
              @click="goForm(row.id)"
            >编辑</el-button>
            <el-button
              v-if="row.status === 0"
              type="success" link size="small"
              @click="handleSubmitApproval(row)"
            >提交审批</el-button>
            <el-button
              v-if="row.status === 1"
              type="primary" link size="small"
              @click="openCallback(row)"
            >审批回调</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="queryForm.pageNum"
          v-model:page-size="queryForm.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @change="loadData"
        />
      </div>
      </div>
    </el-card>

    <!-- 审批回调弹窗 -->
    <el-dialog v-model="showCallbackDialog" title="审批回调（模拟）" width="420px" :close-on-click-modal="false">
      <el-form :model="callbackForm" label-width="90px">
        <el-form-item label="审批结果">
          <el-radio-group v-model="callbackForm.status">
            <el-radio :value="2">通过</el-radio>
            <el-radio :value="3">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="callbackForm.comment" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCallbackDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCallback">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import {
  CHANGE_TYPE_OPTIONS,
  changeTypeLabel,
  getChangePage,
  submitChangeApproval,
  approvalCallback,
  type OprContractChange,
} from '@/api/opr/change'

const router = useRouter()
const loading = ref(false)
const tableData = ref<OprContractChange[]>([])
const total = ref(0)
const showCallbackDialog = ref(false)
const submitting = ref(false)
const currentChangeId = ref<number>(0)

const queryForm = reactive({
  changeCode: '',
  status: undefined as number | undefined,
  changeTypeCode: '',
  pageNum: 1,
  pageSize: 20,
})

const callbackForm = reactive({ status: 2 as 2 | 3, comment: '' })

type TagType = 'success' | 'primary' | 'warning' | 'info' | 'danger' | undefined

const TYPE_TAG_MAP: Record<string, TagType> = {
  RENT: 'danger', FEE: 'warning', TERM: 'primary', AREA: 'success',
  BRAND: 'info', TENANT: 'info', COMPANY: 'info', CLAUSE: 'info',
}
function typeTagType(code: string): TagType { return TYPE_TAG_MAP[code] }

function statusLabel(s?: number): string {
  const m: Record<number, string> = { 0: '草稿', 1: '审批中', 2: '已通过', 3: '已驳回' }
  return s != null ? (m[s] ?? String(s)) : '-'
}
function statusTagType(s?: number): TagType {
  const m: Record<number, TagType> = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
  return s != null ? m[s] : undefined
}
function formatDt(v?: string): string {
  return v ? v.replace('T', ' ').substring(0, 16) : '-'
}

async function loadData() {
  loading.value = true
  try {
    const res = await getChangePage(queryForm)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() { queryForm.pageNum = 1; loadData() }
function handleReset() {
  queryForm.changeCode = ''
  queryForm.status = undefined
  queryForm.changeTypeCode = ''
  queryForm.pageNum = 1
  loadData()
}

function goForm(id?: number) {
  router.push(id ? `/opr/contract-changes/form?id=${id}` : '/opr/contract-changes/form')
}
function goDetail(id: number) {
  router.push(`/opr/contract-changes/${id}`)
}

async function handleSubmitApproval(row: OprContractChange) {
  await ElMessageBox.confirm(`确认提交变更单【${row.changeCode}】进入审批流程？`, '提交审批', { type: 'warning' })
  await submitChangeApproval(row.id)
  ElMessage.success('已提交审批')
  loadData()
}

function openCallback(row: OprContractChange) {
  currentChangeId.value = row.id
  callbackForm.status = 2
  callbackForm.comment = ''
  showCallbackDialog.value = true
}
async function submitCallback() {
  submitting.value = true
  try {
    await approvalCallback(currentChangeId.value, callbackForm)
    ElMessage.success(callbackForm.status === 2 ? '审批通过，应收重算已触发' : '审批已驳回')
    showCallbackDialog.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.change-page { display: flex; flex-direction: column; gap: 16px; }

.filter-card {
  border-radius: 12px !important;
  border: 1px solid rgba(0, 0, 0, 0.06) !important;
  transition: box-shadow 0.2s;
  &:hover { box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08) !important; }
  :deep(.el-card__body) { padding: 14px 20px; }
  :deep(.el-form-item) { margin-bottom: 0; }
}

.table-card {
  border-radius: 12px !important;
  border: 1px solid rgba(0, 0, 0, 0.06) !important;
  overflow: hidden;
  :deep(.el-card__body) { padding: 0; }
}

.card-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 20px; border-bottom: 1px solid #f1f5f9; background: #fff;
  .header-left { display: flex; align-items: center; gap: 10px; }
  .header-title {
    font-size: 15px; font-weight: 600; color: #1e293b;
    display: flex; align-items: center; gap: 8px;
    &::before { content: ''; display: inline-block; width: 3px; height: 16px;
      background: linear-gradient(180deg, #3b82f6, #60a5fa); border-radius: 2px; }
  }
  .count-tag {
    font-size: 12px; background: #eff6ff; color: #3b82f6;
    border: 1px solid #bfdbfe; border-radius: 10px; padding: 2px 10px; font-weight: 500;
  }
  .header-actions { display: flex; gap: 8px; align-items: center; }
}

.table-body {
  padding: 16px 20px;
  :deep(.el-table) {
    border-radius: 8px; overflow: hidden;
    .el-table__header-wrapper th.el-table__cell {
      background: #f8fafc; color: #64748b; font-weight: 600; font-size: 13px;
      border-bottom: 1px solid #e8edf3;
    }
    .el-table__row:hover > td.el-table__cell { background-color: #f0f7ff !important; }
    .el-table__row--striped > td.el-table__cell { background-color: #fafbfc; }
    td.el-table__cell { border-bottom: 1px solid #f4f6f9; }
  }
}

.pagination { margin-top: 14px; display: flex; justify-content: flex-end; }
.text-muted { color: #c0c4cc; font-size: 12px; }
</style>
