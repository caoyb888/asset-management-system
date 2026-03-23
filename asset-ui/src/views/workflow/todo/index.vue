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

    <!-- 待办列表 -->
    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column label="标题" prop="title" min-width="200" show-overflow-tooltip />
        <el-table-column label="业务类型" width="140">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.businessTypeName || businessTypeLabel(row.businessType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发起人" prop="initiatorName" width="100" />
        <el-table-column label="项目" prop="projectName" width="150" show-overflow-tooltip />
        <el-table-column label="发起时间" prop="createdAt" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleApprove(row)">通过</el-button>
            <el-button type="danger" link size="small" @click="handleReject(row)">驳回</el-button>
            <el-button type="info" link size="small" @click="handleViewBiz(row)">查看单据</el-button>
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

    <!-- 审批弹窗 -->
    <el-dialog v-model="approvalDialog.visible" :title="approvalDialog.title" width="460px">
      <el-form label-width="80px">
        <el-form-item label="审批意见">
          <el-input
            v-model="approvalDialog.comment"
            type="textarea"
            :rows="3"
            placeholder="填写审批意见（可选）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approvalDialog.visible = false">取消</el-button>
        <el-button
          :type="approvalDialog.action === 'approve' ? 'primary' : 'danger'"
          :loading="approvalDialog.loading"
          @click="confirmApproval"
        >
          确认{{ approvalDialog.action === 'approve' ? '通过' : '驳回' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getTodoPage,
  BUSINESS_TYPE_OPTIONS,
  businessTypeLabel,
  BUSINESS_ROUTE_MAP,
  type TodoTaskVO,
  type TaskQueryDTO,
} from '@/api/workflow/task'
import { approveTask, rejectTask } from '@/api/workflow/approval'
import { useWorkflowStore } from '@/store/modules/workflow/useWorkflowStore'

const router = useRouter()
const workflowStore = useWorkflowStore()

const loading = ref(false)
const tableData = ref<TodoTaskVO[]>([])
const total = ref(0)

const query = reactive<TaskQueryDTO>({
  businessType: undefined,
  title: undefined,
  pageNum: 1,
  pageSize: 20,
})

const approvalDialog = reactive({
  visible: false,
  title: '',
  action: '' as 'approve' | 'reject',
  processInstanceId: '',
  comment: '',
  loading: false,
})

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const res = await getTodoPage(query)
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

function handleApprove(row: TodoTaskVO) {
  approvalDialog.visible = true
  approvalDialog.title = '审批通过 - ' + row.title
  approvalDialog.action = 'approve'
  approvalDialog.processInstanceId = row.processInstanceId
  approvalDialog.comment = ''
}

function handleReject(row: TodoTaskVO) {
  approvalDialog.visible = true
  approvalDialog.title = '驳回 - ' + row.title
  approvalDialog.action = 'reject'
  approvalDialog.processInstanceId = row.processInstanceId
  approvalDialog.comment = ''
}

async function confirmApproval() {
  approvalDialog.loading = true
  try {
    const data = { comment: approvalDialog.comment }
    if (approvalDialog.action === 'approve') {
      await approveTask(approvalDialog.processInstanceId, data)
      ElMessage.success('审批通过')
    } else {
      await rejectTask(approvalDialog.processInstanceId, data)
      ElMessage.success('已驳回')
    }
    approvalDialog.visible = false
    loadData()
    workflowStore.fetchTodoCount()
  } catch {
    // error handled by interceptor
  } finally {
    approvalDialog.loading = false
  }
}

function handleViewBiz(row: TodoTaskVO) {
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
