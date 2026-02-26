<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form :model="query" inline>
        <el-form-item label="填报月份">
          <el-date-picker
            v-model="query.reportMonth"
            type="month"
            value-format="YYYY-MM"
            placeholder="选择月份"
            clearable
            style="width:150px"
          />
        </el-form-item>
        <el-form-item label="合同ID">
          <el-input v-model.number="query.contractId" placeholder="合同ID" clearable style="width:130px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:110px">
            <el-option label="待确认" :value="0" />
            <el-option label="已确认" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="doSearch">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top:12px">
      <!-- 操作栏 -->
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
        <span style="font-size:15px;font-weight:600">营收填报列表</span>
        <div style="display:flex;gap:8px">
          <el-button :icon="Download" @click="handleDownloadTemplate">下载模板</el-button>
          <el-button :icon="Upload" @click="importVisible=true">批量导入</el-button>
          <el-button :icon="Download" @click="handleExport">导出</el-button>
          <el-button type="primary" :icon="Plus" @click="$router.push('/opr/revenue-reports/form')">
            新增填报
          </el-button>
        </div>
      </div>

      <!-- 数据表格 -->
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column label="合同ID" prop="contractId" width="100" />
        <el-table-column label="商铺ID" prop="shopId" width="100" />
        <el-table-column label="填报日期" prop="reportDate" width="120" />
        <el-table-column label="填报月份" prop="reportMonth" width="110" />
        <el-table-column label="营业额（元）" prop="revenueAmount" align="right">
          <template #default="{ row }">
            <span style="color:#2E75B6;font-weight:600">
              {{ formatAmount(row.revenueAmount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '已确认' : '待确认' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createdAt" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0"
              link
              type="primary"
              size="small"
              @click="openEdit(row)"
            >编辑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        style="margin-top:12px;justify-content:flex-end"
        @change="loadList"
      />
    </el-card>

    <!-- 批量导入弹窗 -->
    <el-dialog v-model="importVisible" title="批量导入营收" width="560px" :close-on-click-modal="false">
      <el-upload
        ref="uploadRef"
        drag
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :on-change="onFileChange"
        :on-exceed="() => ElMessage.warning('只能上传一个文件')"
      >
        <el-icon class="el-icon--upload"><Upload /></el-icon>
        <div class="el-upload__text">拖拽文件到此处，或 <em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">
            仅支持 .xlsx / .xls 格式，请先
            <el-link type="primary" @click="handleDownloadTemplate">下载模板</el-link>
          </div>
        </template>
      </el-upload>

      <!-- 错误列表 -->
      <div v-if="importResult" style="margin-top:12px">
        <el-alert
          :title="`导入完成：成功 ${importResult.successCount} 条`"
          :type="importResult.errorList.length ? 'warning' : 'success'"
          show-icon
          :closable="false"
        />
        <el-collapse v-if="importResult.errorList.length" style="margin-top:8px">
          <el-collapse-item :title="`错误明细（${importResult.errorList.length} 条）`" name="errors">
            <div v-for="(err, i) in importResult.errorList" :key="i" style="color:#E6A23C;font-size:13px;line-height:1.8">
              {{ err }}
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>

      <template #footer>
        <el-button @click="importVisible = false">关闭</el-button>
        <el-button type="primary" :loading="importing" @click="doImport">开始导入</el-button>
      </template>
    </el-dialog>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" title="编辑营收" width="440px">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="填报日期">
          <el-input :value="editForm.reportDate" disabled />
        </el-form-item>
        <el-form-item label="营业额（元）" required>
          <el-input-number
            v-model="editForm.revenueAmount"
            :precision="2"
            :min="0"
            style="width:100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Plus, Upload, Download } from '@element-plus/icons-vue'
import { revenueReportApi, type OprRevenueReport, type RevenueReportQueryDTO, type ImportResultVO } from '@/api/opr/revenue'

// ── 状态 ────────────────────────────────────────────────────────
const loading    = ref(false)
const tableData  = ref<OprRevenueReport[]>([])
const total      = ref(0)
const query      = reactive<RevenueReportQueryDTO>({ pageNum: 1, pageSize: 20 })

const importVisible = ref(false)
const importing     = ref(false)
const importResult  = ref<ImportResultVO | null>(null)
const selectedFile  = ref<File | null>(null)
const uploadRef     = ref()

const editVisible = ref(false)
const saving      = ref(false)
const editForm    = reactive<{ id: number; reportDate: string; revenueAmount: number }>({
  id: 0, reportDate: '', revenueAmount: 0,
})

// ── 方法 ────────────────────────────────────────────────────────
async function loadList() {
  loading.value = true
  try {
    const res = await revenueReportApi.page(query) as any
    if (res.data) {
      tableData.value = res.data.records || []
      total.value     = res.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

function doSearch() { query.pageNum = 1; loadList() }
function resetQuery() {
  Object.assign(query, { reportMonth: undefined, contractId: undefined, status: undefined, pageNum: 1 })
  loadList()
}

function formatAmount(val: number) {
  if (val == null) return '—'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

// 编辑
function openEdit(row: OprRevenueReport) {
  editForm.id = row.id
  editForm.reportDate = row.reportDate
  editForm.revenueAmount = row.revenueAmount
  editVisible.value = true
}
async function doEdit() {
  saving.value = true
  try {
    await revenueReportApi.update(editForm.id, { revenueAmount: editForm.revenueAmount })
    ElMessage.success('保存成功')
    editVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

// 导入
function onFileChange(file: any) { selectedFile.value = file.raw }
async function doImport() {
  if (!selectedFile.value) { ElMessage.warning('请先选择文件'); return }
  importing.value = true
  importResult.value = null
  try {
    const res = await revenueReportApi.importExcel(selectedFile.value) as any
    importResult.value = res.data
    ElMessage.success(`导入完成，成功 ${res.data.successCount} 条`)
    loadList()
  } finally {
    importing.value = false
  }
}

// 导出
async function handleExport() {
  const res = await revenueReportApi.exportExcel(query) as any
  const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
  const url  = URL.createObjectURL(blob)
  const a    = document.createElement('a')
  a.href     = url
  a.download = '营收填报.xlsx'
  a.click()
  URL.revokeObjectURL(url)
}

async function handleDownloadTemplate() {
  const res = await revenueReportApi.downloadTemplate() as any
  const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
  const url  = URL.createObjectURL(blob)
  const a    = document.createElement('a')
  a.href     = url
  a.download = '营收填报导入模板.xlsx'
  a.click()
  URL.revokeObjectURL(url)
}

onMounted(loadList)
</script>

<style scoped>
.filter-card { margin-bottom: 0; }
</style>
