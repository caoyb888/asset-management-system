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
        <el-table-column prop="decompositionCode" label="分解编号" width="140" />
        <el-table-column prop="projectName" label="项目" />
        <el-table-column prop="businessYear" label="业务年度" width="100" />
        <el-table-column prop="policyName" label="关联租决政策" />
        <el-table-column prop="totalAnnualRent" label="年租金合计(元)" width="140">
          <template #default="{ row }">
            {{ row.totalAnnualRent?.toLocaleString() ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push({ path: '/inv/rent-decomps/form', query: { id: row.id } })">编辑</el-button>
            <el-button link type="success" @click="handleExport(row)">导出</el-button>
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
    <el-dialog v-model="importVisible" title="导入租金分解数据" width="500px">
      <el-upload drag action="#" :auto-upload="false" accept=".xlsx,.xls">
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">拖拽 Excel 文件到此处，或 <em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">仅支持 .xlsx / .xls 格式，数据量大时请耐心等待</div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, Upload, UploadFilled } from '@element-plus/icons-vue'
import { getRentDecompPage, deleteRentDecomp, type RentDecompVO } from '@/api/inv/rentDecomp'

const router = useRouter()
const loading = ref(false)
const list = ref<RentDecompVO[]>([])
const total = ref(0)
const importVisible = ref(false)
const query = reactive({ page: 1, size: 10, projectName: '', businessYear: '' })

async function fetchList() {
  loading.value = true
  try {
    const res = await getRentDecompPage(query)
    list.value = res.records; total.value = res.total
  } finally { loading.value = false }
}

function handleReset() { query.page = 1; query.projectName = ''; query.businessYear = ''; fetchList() }
function handleImport() { importVisible.value = true }
function handleExport(row: RentDecompVO) { ElMessage.info(`导出 ${row.decompositionCode} - 开发中`) }

async function handleDelete(row: RentDecompVO) {
  await ElMessageBox.confirm(`确认删除 "${row.decompositionCode}"？`, '提示', { type: 'warning' })
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
</style>
