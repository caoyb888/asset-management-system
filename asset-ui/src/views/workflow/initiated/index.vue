<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="filter-card">
      <el-form :model="query" inline>
        <el-form-item label="业务类型">
          <el-select v-model="query.businessType" clearable placeholder="全部" style="width: 180px">
            <el-option
              v-for="opt in BUSINESS_TYPE_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="query.title" clearable placeholder="搜索标题" style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 发起列表 -->
    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column label="标题" prop="title" min-width="200" show-overflow-tooltip />
        <el-table-column label="业务类型" width="140">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.businessTypeName || businessTypeLabel(row.businessType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前状态" width="110">
          <template #default="{ row }">
            <el-tag :type="STATUS_TAG_MAP[row.status]" size="small">
              {{ row.statusName || STATUS_LABEL_MAP[row.status] || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前审批人" prop="currentAssignee" width="110" />
        <el-table-column label="项目" prop="projectName" width="150" show-overflow-tooltip />
        <el-table-column label="发起时间" prop="createdAt" width="170" />
        <el-table-column label="完成时间" prop="completedAt" width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 1"
              type="primary"
              link
              size="small"
              @click="handleUrge(row)"
            >催办</el-button>
            <el-button
              v-if="row.status === 1"
              type="warning"
              link
              size="small"
              @click="handleRevoke(row)"
            >撤回</el-button>
            <el-button type="primary" link size="small" @click="handleViewBiz(row)">查看单据</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > 0"
        class="pagination"
        :current-page="query.pageNum"
        :page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="(val: number) => { query.pageNum = val; loadData() }"
        @size-change="(val: number) => { query.pageSize = val; query.pageNum = 1; loadData() }"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getInitiatedPage,
  BUSINESS_TYPE_OPTIONS,
  businessTypeLabel,
  BUSINESS_ROUTE_MAP,
  type InitiatedVO,
  type TaskQueryDTO,
} from '@/api/workflow/task'
import { revokeProcess, urgeProcess } from '@/api/workflow/approval'

const router = useRouter()

const loading = ref(false)
const tableData = ref<InitiatedVO[]>([])
const total = ref(0)

const query = reactive<TaskQueryDTO>({
  businessType: undefined,
  title: undefined,
  pageNum: 1,
  pageSize: 20,
})

type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'
const STATUS_TAG_MAP: Record<number, TagType> = {
  1: 'warning',
  2: 'success',
  3: 'danger',
  4: 'info',
  5: 'info',
}

const STATUS_LABEL_MAP: Record<number, string> = {
  1: '审批中',
  2: '已通过',
  3: '已驳回',
  4: '已撤回',
  5: '已作废',
}

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const res = await getInitiatedPage(query)
    tableData.value = res.records
    total.value = res.total
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadData()
}

function handleReset() {
  query.businessType = undefined
  query.title = undefined
  query.pageNum = 1
  loadData()
}

async function handleUrge(row: InitiatedVO) {
  try {
    await ElMessageBox.confirm(
      `确认催办「${row.title}」？将通知当前审批人尽快处理。`,
      '催办确认',
      { type: 'info', confirmButtonText: '确认催办', cancelButtonText: '取消' },
    )
    await urgeProcess(row.processInstanceId)
    ElMessage.success('已催办，已通知审批人')
  } catch {
    // 用户取消或请求失败
  }
}

async function handleRevoke(row: InitiatedVO) {
  try {
    await ElMessageBox.confirm(
      `确认撤回「${row.title}」？撤回后单据将恢复为草稿状态。`,
      '撤回确认',
      { type: 'warning', confirmButtonText: '确认撤回', cancelButtonText: '取消' },
    )
    await revokeProcess(row.processInstanceId)
    ElMessage.success('已撤回')
    loadData()
  } catch {
    // 用户取消或请求失败
  }
}

function handleViewBiz(row: InitiatedVO) {
  const routeFn = BUSINESS_ROUTE_MAP[row.businessType]
  if (routeFn) {
    router.push(routeFn(row.businessId))
  } else {
    ElMessage.warning('未配置该业务类型的跳转路径')
  }
}
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.filter-card :deep(.el-card__body) {
  padding-bottom: 2px;
}

.pagination {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
