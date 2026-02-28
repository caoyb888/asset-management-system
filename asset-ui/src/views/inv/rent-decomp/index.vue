<template>
  <div class="rent-decomp-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="filter-card">
      <el-form :model="query" inline @keyup.enter="fetchList">
        <el-form-item label="项目">
          <el-input v-model="query.projectName" placeholder="项目名称" clearable />
        </el-form-item>
        <el-form-item label="年度">
          <el-date-picker v-model="query.businessYear" type="year" placeholder="选择年度" value-format="YYYY" style="width: 120px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 100px">
            <el-option label="草稿" :value="0" />
            <el-option label="审批中" :value="1" />
            <el-option label="已通过" :value="2" />
            <el-option label="已驳回" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="fetchList">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <div class="card-header">
        <div class="header-left">
          <span class="header-title">租金分解列表</span>
          <span class="count-tag">共 {{ total }} 条</span>
        </div>
        <div class="header-actions">
          <el-button :icon="Upload" @click="handleImport">导入</el-button>
          <el-button type="primary" :icon="Plus" @click="router.push('/inv/rent-decomps/form')">新增分解</el-button>
        </div>
      </div>

      <div class="table-body">
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="decompCode" label="分解编号" width="150" />
        <el-table-column label="项目">
          <template #default="{ row }">{{ row.projectName || row.projectId }}</template>
        </el-table-column>
        <el-table-column label="业务年度" width="100">
          <template #default="{ row }">{{ row.createdAt?.slice(0, 4) }}</template>
        </el-table-column>
        <el-table-column label="关联租决政策">
          <template #default="{ row }">
            {{ row.policySnapshot ? (row.policySnapshot as Record<string, unknown>)['policyCode'] ?? '-' : row.policyId }}
          </template>
        </el-table-column>
        <el-table-column prop="totalAnnualRent" label="年租金合计(元)" width="150" align="right">
          <template #default="{ row }">
            {{ row.totalAnnualRent != null ? Number(row.totalAnnualRent).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="STATUS_MAP[row.status]?.type ?? 'info'">
              {{ STATUS_MAP[row.status]?.label ?? '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push({ path: '/inv/rent-decomps/form', query: { id: row.id } })">编辑</el-button>
            <el-button
              v-if="row.status === 0 || row.status === 3"
              link type="warning" :loading="submittingId === row.id"
              @click="handleSubmitApproval(row)"
            >提交审批</el-button>
            <el-button link type="success" :loading="exportingId === row.id" @click="handleExport(row)">导出</el-button>
            <el-button link type="danger" :disabled="row.status === 1 || row.status === 2" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @change="fetchList"
        />
      </div>
      </div>
    </el-card>

    <!-- 导入弹窗 -->
    <el-dialog v-model="importVisible" title="导入租金分解数据" width="540px" @close="resetImport">
      <el-alert v-if="!importTargetId" type="warning" :closable="false"
        title="请先在列表中选择要导入到的租金分解记录（点击导入按钮时需先编辑/查看某条记录）" />
      <template v-else>
        <el-upload
          ref="uploadRef"
          drag action="#" :auto-upload="false" accept=".xlsx,.xls"
          :limit="1" :on-change="onFileChange" :on-remove="() => importFile = null"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">拖拽 Excel 文件到此处，或 <em>点击上传</em></div>
          <template #tip>
            <div class="el-upload__tip">仅支持 .xlsx / .xls 格式；导入将覆盖该记录的现有明细数据</div>
          </template>
        </el-upload>
        <div v-if="importErrors.length" class="import-errors">
          <el-alert type="error" :closable="false" title="部分行导入失败：" />
          <ul><li v-for="(e, i) in importErrors" :key="i" class="error-item">{{ e }}</li></ul>
        </div>
      </template>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" :disabled="!importTargetId || !importFile"
          @click="doImport">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadFile, UploadInstance } from 'element-plus'
import { Plus, Search, Refresh, Upload, UploadFilled } from '@element-plus/icons-vue'
import {
  getRentDecompPage, deleteRentDecomp,
  importRentDecompDetails, exportRentDecompDetails,
  submitRentDecompApproval,
  type RentDecompVO,
} from '@/api/inv/rentDecomp'

const router = useRouter()
const loading = ref(false)
const list = ref<RentDecompVO[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, projectName: '', businessYear: '', status: undefined as number | undefined })

// ── 状态映射 ──
type TagType = 'primary' | 'success' | 'warning' | 'danger' | 'info' | undefined
const STATUS_MAP: Record<number, { label: string; type: TagType }> = {
  0: { label: '草稿', type: 'info' },
  1: { label: '审批中', type: 'warning' },
  2: { label: '已通过', type: 'success' },
  3: { label: '已驳回', type: 'danger' },
}

// ── 导出状态 ──
const exportingId = ref<number | null>(null)

// ── 提交审批状态 ──
const submittingId = ref<number | null>(null)

// ── 导入状态 ──
const importVisible = ref(false)
const importTargetId = ref<number | null>(null)
const importFile = ref<File | null>(null)
const importing = ref(false)
const importErrors = ref<string[]>([])
const uploadRef = ref<UploadInstance>()

async function fetchList() {
  loading.value = true
  try {
    const res = await getRentDecompPage(query)
    list.value = res.records; total.value = res.total
  } finally { loading.value = false }
}

function handleReset() { query.page = 1; query.projectName = ''; query.businessYear = ''; query.status = undefined; fetchList() }

async function handleSubmitApproval(row: RentDecompVO) {
  submittingId.value = row.id
  try {
    await submitRentDecompApproval(row.id)
    ElMessage.success('已提交审批')
    fetchList()
  } catch (err: unknown) {
    ElMessage.error((err as { message?: string })?.message || '提交失败')
  } finally {
    submittingId.value = null
  }
}

/** 打开导入弹窗（不强依赖选中行，允许通用导入后再指定记录） */
function handleImport() {
  importTargetId.value = null
  importVisible.value = true
}

function resetImport() {
  importFile.value = null
  importErrors.value = []
  uploadRef.value?.clearFiles()
}

function onFileChange(uploadFile: UploadFile) {
  importFile.value = uploadFile.raw ?? null
}

async function doImport() {
  if (!importTargetId.value || !importFile.value) return
  importing.value = true
  importErrors.value = []
  try {
    const res = await importRentDecompDetails(importTargetId.value, importFile.value)
    if (res.errorCount > 0) {
      importErrors.value = res.errors
      ElMessage.warning(`导入完成：成功 ${res.successCount} 条，失败 ${res.errorCount} 条`)
    } else {
      ElMessage.success(`导入成功，共 ${res.successCount} 条`)
      importVisible.value = false
    }
    fetchList()
  } catch (err: unknown) {
    ElMessage.error((err as { message?: string })?.message || '导入失败')
  } finally {
    importing.value = false
  }
}

async function handleExport(row: RentDecompVO) {
  exportingId.value = row.id
  try {
    await exportRentDecompDetails(row.id, row.decompCode)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exportingId.value = null
  }
}

async function handleDelete(row: RentDecompVO) {
  await ElMessageBox.confirm(`确认删除 "${row.decompCode}"？`, '提示', { type: 'warning' })
  await deleteRentDecomp(row.id)
  ElMessage.success('删除成功'); fetchList()
}

onMounted(fetchList)
</script>

<style scoped lang="scss">
.rent-decomp-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

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
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid #f1f5f9;
  background: #fff;

  .header-left {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .header-title {
    font-size: 15px;
    font-weight: 600;
    color: #1e293b;
    display: flex;
    align-items: center;
    gap: 8px;
    &::before {
      content: '';
      display: inline-block;
      width: 3px;
      height: 16px;
      background: linear-gradient(180deg, #3b82f6, #60a5fa);
      border-radius: 2px;
    }
  }

  .count-tag {
    font-size: 12px;
    background: #eff6ff;
    color: #3b82f6;
    border: 1px solid #bfdbfe;
    border-radius: 10px;
    padding: 2px 10px;
    font-weight: 500;
  }

  .header-actions {
    display: flex;
    gap: 8px;
    align-items: center;
  }
}

.table-body {
  padding: 16px 20px;

  :deep(.el-table) {
    border-radius: 8px;
    overflow: hidden;

    .el-table__header-wrapper th.el-table__cell {
      background: #f8fafc;
      color: #64748b;
      font-weight: 600;
      font-size: 13px;
      border-bottom: 1px solid #e8edf3;
    }

    .el-table__row:hover > td.el-table__cell {
      background-color: #f0f7ff !important;
    }

    .el-table__row--striped > td.el-table__cell {
      background-color: #fafbfc;
    }

    td.el-table__cell {
      border-bottom: 1px solid #f4f6f9;
    }
  }
}

.pagination {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}

.import-errors { margin-top: 12px; max-height: 200px; overflow-y: auto; }
.error-item { font-size: 12px; color: #f56c6c; margin: 4px 0; }
</style>
