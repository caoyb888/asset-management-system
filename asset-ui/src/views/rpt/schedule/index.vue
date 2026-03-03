<template>
  <div class="rpt-schedule">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="filter-card">
      <el-row :gutter="12" align="middle">
        <el-col :span="6">
          <el-input
            v-model="keyword"
            placeholder="搜索任务名称/报表编码..."
            clearable
            :prefix-icon="Search"
            @keyup.enter="loadData"
          />
        </el-col>
        <el-col :span="4">
          <el-select v-model="filterStatus" placeholder="全部状态" clearable @change="loadData">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-col>
        <el-col :span="14" class="toolbar-right">
          <el-button :loading="loading" @click="loadData">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
          <el-button type="primary" @click="openCreateDialog">
            <el-icon><Plus /></el-icon>
            新增推送任务
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never" class="table-card">
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        row-key="id"
      >
        <el-table-column prop="taskCode" label="任务编码" width="180" />
        <el-table-column prop="taskName" label="任务名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="reportCode" label="关联报表" width="180">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.reportCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="cronExpression" label="Cron 表达式" width="160" />
        <el-table-column label="收件人" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.recipients?.join('；') }}
          </template>
        </el-table-column>
        <el-table-column prop="exportFormat" label="导出格式" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.exportFormat === 'PDF' ? 'danger' : 'success'" size="small">
              {{ row.exportFormat }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="上次执行" width="165">
          <template #default="{ row }">
            {{ row.lastRunTime ? formatDateTime(row.lastRunTime) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="下次执行" width="165">
          <template #default="{ row }">
            <span :class="{ 'text-warning': isNextRunSoon(row.nextRunTime) }">
              {{ row.nextRunTime ? formatDateTime(row.nextRunTime) : '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="执行次数" width="90" align="center">
          <template #default="{ row }">
            {{ row.runCount ?? 0 }}
            <span v-if="row.failCount > 0" class="fail-badge">失败 {{ row.failCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              :loading="togglingIds.has(row.id)"
              @change="onToggle(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-popconfirm
              title="确认删除该推送任务？"
              @confirm="handleDelete(row.id)"
            >
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @change="loadData"
        />
      </div>
    </el-card>

    <!-- 新增/编辑 Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑推送任务' : '新增推送任务'"
      width="620px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="form.taskName" placeholder="例：每月末财务应收汇总" maxlength="60" show-word-limit />
        </el-form-item>
        <el-form-item label="关联报表" prop="reportCode">
          <el-select v-model="form.reportCode" placeholder="选择报表" style="width: 100%">
            <el-option-group
              v-for="cat in reportCategoryOptions"
              :key="cat.label"
              :label="cat.label"
            >
              <el-option
                v-for="r in cat.options"
                :key="r.value"
                :label="r.label"
                :value="r.value"
              />
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item label="Cron 表达式" prop="cronExpression">
          <el-input v-model="form.cronExpression" placeholder="例：0 30 8 * * ?（每天 8:30）" />
          <div class="form-tip">Spring 6位格式：秒 分 时 日 月 周，如每周一 9:00 = <code>0 0 9 ? * MON</code></div>
        </el-form-item>
        <el-form-item label="导出格式" prop="exportFormat">
          <el-radio-group v-model="form.exportFormat">
            <el-radio-button value="EXCEL">Excel</el-radio-button>
            <el-radio-button value="PDF">PDF</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="收件人" prop="recipients">
          <el-select
            v-model="form.recipients"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="输入邮箱后按回车添加"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="抄送人">
          <el-select
            v-model="form.ccRecipients"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="输入邮箱后按回车添加（可选）"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="立即启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ editingId ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Search, Plus, Refresh } from '@element-plus/icons-vue'
import {
  pageScheduleTasks, createScheduleTask, updateScheduleTask,
  deleteScheduleTask, toggleScheduleTask,
  type ScheduleTaskVO, type ScheduleTaskDTO,
} from '@/api/rpt/schedule'

// ─────────────────────────── 报表选项（对应 home.vue 中的 reportCode）───────────────────────────

const reportCategoryOptions = [
  {
    label: '资产类报表',
    options: [
      { value: 'AST_DASHBOARD', label: '资产数据看板' },
      { value: 'AST_VACANCY', label: '空置率统计' },
      { value: 'AST_RATES', label: '出租率/开业率' },
      { value: 'AST_BRAND_DIST', label: '品牌业态分布' },
      { value: 'AST_SHOP_RENTAL', label: '商铺租赁信息' },
    ],
  },
  {
    label: '招商类报表',
    options: [
      { value: 'INV_DASHBOARD', label: '招商数据看板' },
      { value: 'INV_FUNNEL', label: '客户漏斗分析' },
      { value: 'INV_PERFORMANCE', label: '招商业绩对比' },
      { value: 'INV_RENT_LEVEL', label: '租金水平分析' },
    ],
  },
  {
    label: '营运类报表',
    options: [
      { value: 'OPR_DASHBOARD', label: '营运数据看板' },
      { value: 'OPR_REVENUE', label: '营收汇总分析' },
      { value: 'OPR_CHANGES', label: '合同变更分析' },
      { value: 'OPR_REGION', label: '地区业务对比' },
    ],
  },
  {
    label: '财务类报表',
    options: [
      { value: 'FIN_DASHBOARD', label: '财务数据看板' },
      { value: 'FIN_OUTSTANDING', label: '欠款统计分析' },
      { value: 'FIN_AGING', label: '账龄分析' },
      { value: 'FIN_COLLECTION', label: '收缴率趋势' },
    ],
  },
]

// ─────────────────────────── 列表 ───────────────────────────

const loading = ref(false)
const tableData = ref<ScheduleTaskVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const filterStatus = ref<0 | 1 | null>(null)

async function loadData() {
  loading.value = true
  try {
    const res = await pageScheduleTasks({
      keyword: keyword.value || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    })
    const page = (res as any).data ?? res
    tableData.value = page.records ?? []
    total.value = page.total ?? 0
  } finally {
    loading.value = false
  }
}

// 本地按状态过滤（后端暂不支持 status 参数时的前端兜底）
const filteredData = computed(() => {
  if (filterStatus.value === null || filterStatus.value === undefined) return tableData.value
  return tableData.value.filter(r => r.status === filterStatus.value)
})

// ─────────────────────────── 启用/禁用 ───────────────────────────

const togglingIds = ref(new Set<number>())

async function onToggle(row: ScheduleTaskVO) {
  togglingIds.value.add(row.id)
  try {
    const newStatus = await toggleScheduleTask(row.id)
    row.status = ((newStatus as any).data ?? newStatus) as 0 | 1
    ElMessage.success(row.status === 1 ? '已启用' : '已禁用')
  } catch {
    ElMessage.error('操作失败，请稍后重试')
  } finally {
    togglingIds.value.delete(row.id)
  }
}

// ─────────────────────────── 删除 ───────────────────────────

async function handleDelete(id: number) {
  try {
    await deleteScheduleTask(id)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    ElMessage.error('删除失败，请稍后重试')
  }
}

// ─────────────────────────── 新增/编辑 Dialog ───────────────────────────

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const defaultForm = (): ScheduleTaskDTO => ({
  taskName: '',
  reportCode: '',
  cronExpression: '',
  recipients: [],
  ccRecipients: [],
  exportFormat: 'EXCEL',
  filterParams: {},
  enabled: true,
})

const form = reactive<ScheduleTaskDTO>(defaultForm())

const rules: FormRules = {
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  reportCode: [{ required: true, message: '请选择关联报表', trigger: 'change' }],
  cronExpression: [{ required: true, message: '请输入 Cron 表达式', trigger: 'blur' }],
  recipients: [{ required: true, type: 'array', min: 1, message: '至少添加一个收件人邮箱', trigger: 'change' }],
}

function openCreateDialog() {
  editingId.value = null
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

function openEditDialog(row: ScheduleTaskVO) {
  editingId.value = row.id
  Object.assign(form, {
    taskName: row.taskName,
    reportCode: row.reportCode,
    cronExpression: row.cronExpression,
    recipients: [...(row.recipients ?? [])],
    ccRecipients: [...(row.ccRecipients ?? [])],
    exportFormat: row.exportFormat as 'EXCEL' | 'PDF',
    filterParams: {},
    enabled: row.status === 1,
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (editingId.value) {
      await updateScheduleTask(editingId.value, form)
      ElMessage.success('保存成功')
    } else {
      await createScheduleTask(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch {
    ElMessage.error('提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

// ─────────────────────────── 工具函数 ───────────────────────────

function formatDateTime(dt: string) {
  return dt ? dt.replace('T', ' ').slice(0, 16) : '-'
}

function isNextRunSoon(dt: string | undefined): boolean {
  if (!dt) return false
  return new Date(dt).getTime() - Date.now() < 30 * 60 * 1000
}

// ─────────────────────────── 初始化 ───────────────────────────

onMounted(loadData)
</script>

<style scoped lang="scss">
.rpt-schedule {
  .filter-card {
    margin-bottom: 16px;

    .toolbar-right {
      display: flex;
      justify-content: flex-end;
      gap: 8px;
    }
  }

  .table-card {
    .pagination-wrap {
      display: flex;
      justify-content: flex-end;
      margin-top: 16px;
    }
  }

  .form-tip {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
    line-height: 1.5;

    code {
      background: #f4f4f5;
      padding: 1px 5px;
      border-radius: 3px;
      font-family: monospace;
    }
  }

  .fail-badge {
    display: inline-block;
    margin-left: 4px;
    font-size: 11px;
    color: #f56c6c;
  }

  .text-warning {
    color: #e6a23c;
    font-weight: 500;
  }
}
</style>
