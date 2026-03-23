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

    <!-- 已办列表 -->
    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column label="标题" prop="title" min-width="200" show-overflow-tooltip />
        <el-table-column label="业务类型" width="140">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.businessTypeName || businessTypeLabel(row.businessType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发起人" prop="initiatorName" width="100" />
        <el-table-column label="我的审批结果" width="120">
          <template #default="{ row }">
            <el-tag :type="row.result === 2 ? 'success' : 'danger'" size="small">
              {{ row.resultName || (row.result === 2 ? '已通过' : '已驳回') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审批意见" prop="comment" min-width="160" show-overflow-tooltip />
        <el-table-column label="审批时间" prop="completedAt" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
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
import { ElMessage } from 'element-plus'
import {
  getDonePage,
  BUSINESS_TYPE_OPTIONS,
  businessTypeLabel,
  BUSINESS_ROUTE_MAP,
  type DoneTaskVO,
  type TaskQueryDTO,
} from '@/api/workflow/task'

const router = useRouter()

const loading = ref(false)
const tableData = ref<DoneTaskVO[]>([])
const total = ref(0)

const query = reactive<TaskQueryDTO>({
  businessType: undefined,
  title: undefined,
  pageNum: 1,
  pageSize: 20,
})

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const res = await getDonePage(query)
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

function handleViewBiz(row: DoneTaskVO) {
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
