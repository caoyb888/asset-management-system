<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>租金分解列表</span>
          <div class="header-actions">
            <el-button :icon="Upload" @click="handleImport">导入</el-button>
            <el-button type="primary" :icon="Plus" @click="router.push('/inv/rent-decomps/form')">新增分解</el-button>
          </div>
        </div>
      </template>

      <el-form :model="query" inline class="search-form">
        <el-form-item label="项目">
          <el-input v-model="query.projectName" placeholder="项目名称" clearable />
        </el-form-item>
        <el-form-item label="年度">
          <el-date-picker v-model="query.businessYear" type="year" placeholder="选择年度" value-format="YYYY" style="width: 120px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="fetchList">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

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
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push({ path: '/inv/rent-decomps/form', query: { id: row.id } })">编辑</el-button>
            <el-button link type="success" :loading="exportingId === row.id" @click="handleExport(row)">导出</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="mt-4"
        @change="fetchList"
      />
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
  type RentDecompVO,
} from '@/api/inv/rentDecomp'

const router = useRouter()
const loading = ref(false)
const list = ref<RentDecompVO[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, projectName: '', businessYear: '' })

// ── 导出状态 ──
const exportingId = ref<number | null>(null)

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

function handleReset() { query.page = 1; query.projectName = ''; query.businessYear = ''; fetchList() }

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

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-actions { display: flex; gap: 8px; }
.search-form { margin-bottom: 12px; }
.mt-4 { margin-top: 16px; }
.import-errors { margin-top: 12px; max-height: 200px; overflow-y: auto; }
.error-item { font-size: 12px; color: #f56c6c; margin: 4px 0; }
</style>
