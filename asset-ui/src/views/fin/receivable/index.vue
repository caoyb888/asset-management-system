<template>
  <div class="fin-receivable-page">
    <!-- 欠费统计卡片 -->
    <el-row :gutter="16" class="stat-row" v-if="overdueStats">
      <el-col :span="6">
        <el-card class="stat-card stat-total">
          <div class="stat-value">{{ fmt(overdueStats.totalOverdueAmount) }}</div>
          <div class="stat-label">逾期总金额（元）</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card stat-30">
          <div class="stat-value">{{ fmt(overdueStats.overdue30Amount) }}</div>
          <div class="stat-label">30天内逾期（元）</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card stat-90">
          <div class="stat-value">{{ fmt(overdueStats.overdue30To90Amount) }}</div>
          <div class="stat-label">30~90天逾期（元）</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card stat-over90">
          <div class="stat-value">{{ fmt(overdueStats.overdueOver90Amount) }}</div>
          <div class="stat-label">90天以上逾期（元）</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card>
      <!-- 选项卡 -->
      <el-tabs v-model="activeTab" @tab-click="handleTabChange">
        <el-tab-pane label="应收明细" name="detail" />
        <el-tab-pane label="合同汇总" name="summary" />
        <el-tab-pane label="欠费租户 TOP10" name="overdue" />
      </el-tabs>

      <!-- 筛选区（明细/汇总 共用） -->
      <el-form
        v-if="activeTab !== 'overdue'"
        :model="query"
        inline
        class="filter-form"
        @submit.prevent="loadData"
      >
        <el-form-item label="应收编号">
          <el-input v-model="query.receivableCode" placeholder="请输入" clearable style="width:160px" />
        </el-form-item>
        <el-form-item label="合同ID">
          <el-input-number v-model="query.contractId" :min="1" :controls="false" placeholder="合同ID" style="width:120px" />
        </el-form-item>
        <el-form-item label="状态" v-if="activeTab === 'detail'">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:120px">
            <el-option :value="0" label="待收" />
            <el-option :value="1" label="部分收款" />
            <el-option :value="2" label="已收清" />
            <el-option :value="3" label="减免" />
            <el-option :value="9" label="作废" />
          </el-select>
        </el-form-item>
        <el-form-item label="账期月份" v-if="activeTab === 'detail'">
          <el-input v-model="query.accrualMonth" placeholder="2025-01" clearable style="width:120px" />
        </el-form-item>
        <el-form-item label="到期日" v-if="activeTab === 'detail'">
          <el-date-picker
            v-model="dueDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DD"
            style="width:240px"
          />
        </el-form-item>
        <el-form-item label="仅逾期" v-if="activeTab === 'detail'">
          <el-switch v-model="query.overdue" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
          <el-button type="success" @click="handleExport" v-if="activeTab === 'detail'">
            <el-icon><Download /></el-icon> 导出
          </el-button>
          <el-button @click="handleRefreshOverdue">
            <el-icon><Refresh /></el-icon> 刷新逾期状态
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 批量打印工具栏 -->
      <div v-if="activeTab === 'detail' && selectedRows.length > 0" style="margin-bottom:8px">
        <el-button type="primary" size="small" @click="handleMarkPrinted">
          打印账单（已选 {{ selectedRows.length }} 条）
        </el-button>
      </div>

      <!-- 应收明细表格 -->
      <el-table
        v-if="activeTab === 'detail'"
        v-loading="loading"
        :data="detailList"
        stripe
        border
        row-key="id"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="45" />
        <el-table-column prop="receivableCode" label="应收编号" width="160" fixed="left" />
        <el-table-column prop="contractCode" label="合同编号" width="150" />
        <el-table-column prop="merchantName" label="商家" width="130" />
        <el-table-column prop="projectName" label="项目" width="130" />
        <el-table-column prop="feeName" label="费项" width="100" />
        <el-table-column prop="accrualMonth" label="账期" width="90" />
        <el-table-column prop="billingStart" label="计费开始" width="110" />
        <el-table-column prop="billingEnd" label="计费结束" width="110" />
        <el-table-column prop="dueDate" label="到期日" width="110">
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.isOverdue }">{{ row.dueDate }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="actualAmount" label="应收金额" width="120" align="right">
          <template #default="{ row }">{{ fmt(row.actualAmount) }}</template>
        </el-table-column>
        <el-table-column prop="receivedAmount" label="已收金额" width="120" align="right">
          <template #default="{ row }">{{ fmt(row.receivedAmount) }}</template>
        </el-table-column>
        <el-table-column prop="outstandingAmount" label="未收金额" width="120" align="right">
          <template #default="{ row }">
            <span :class="row.outstandingAmount > 0 ? 'text-danger' : ''">{{ fmt(row.outstandingAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="overdueDays" label="逾期天数" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.overdueDays > 0" type="danger" size="small">{{ row.overdueDays }}天</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="statusName" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="已打印" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isPrinted === 1" type="success" size="small">已打印</el-tag>
            <span v-else style="color:#c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0 || row.status === 1"
              type="warning"
              link
              size="small"
              @click="openReductionDialog(row)"
            >减免</el-button>
            <el-button
              v-if="row.status === 0 || row.status === 1"
              type="primary"
              link
              size="small"
              @click="openAdjustDialog(row)"
            >调整</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 合同汇总表格 -->
      <el-table
        v-if="activeTab === 'summary'"
        v-loading="loading"
        :data="summaryList"
        stripe
        border
        row-key="contractId"
      >
        <el-table-column prop="contractCode" label="合同编号" width="150" fixed="left" />
        <el-table-column prop="contractName" label="合同名称" min-width="160" />
        <el-table-column prop="merchantName" label="商家" width="130" />
        <el-table-column prop="projectName" label="项目" width="130" />
        <el-table-column prop="totalActual" label="应收合计" width="130" align="right">
          <template #default="{ row }">{{ fmt(row.totalActual) }}</template>
        </el-table-column>
        <el-table-column prop="totalReceived" label="已收合计" width="130" align="right">
          <template #default="{ row }">{{ fmt(row.totalReceived) }}</template>
        </el-table-column>
        <el-table-column prop="totalOutstanding" label="未收合计" width="130" align="right">
          <template #default="{ row }">
            <span :class="row.totalOutstanding > 0 ? 'text-danger' : ''">{{ fmt(row.totalOutstanding) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalDeduction" label="减免合计" width="120" align="right">
          <template #default="{ row }">{{ fmt(row.totalDeduction) }}</template>
        </el-table-column>
        <el-table-column prop="overdueCount" label="逾期条数" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.overdueCount > 0" type="danger" size="small">{{ row.overdueCount }}</el-tag>
            <span v-else>0</span>
          </template>
        </el-table-column>
        <el-table-column prop="overdueAmount" label="逾期金额" width="130" align="right">
          <template #default="{ row }">
            <span :class="row.overdueAmount > 0 ? 'text-danger' : ''">{{ fmt(row.overdueAmount) }}</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 欠费TOP10表格 -->
      <div v-if="activeTab === 'overdue'">
        <el-table
          v-loading="loading"
          :data="overdueStats?.topDebtors ?? []"
          stripe
          border
        >
          <el-table-column type="index" label="排名" width="60" align="center" />
          <el-table-column prop="merchantName" label="商家" width="150" />
          <el-table-column prop="projectName" label="项目" width="150" />
          <el-table-column prop="contractCode" label="合同编号" width="150" />
          <el-table-column prop="totalOutstanding" label="未收合计" width="140" align="right">
            <template #default="{ row }">{{ fmt(row.totalOutstanding) }}</template>
          </el-table-column>
          <el-table-column prop="overdueCount" label="逾期条数" width="90" align="center">
            <template #default="{ row }">
              <el-tag type="danger" size="small">{{ row.overdueCount }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="overdueAmount" label="逾期金额" width="140" align="right">
            <template #default="{ row }">
              <span class="text-danger">{{ fmt(row.overdueAmount) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 分页（明细/汇总） -->
      <el-pagination
        v-if="activeTab !== 'overdue'"
        class="pagination"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-size="query.pageSize ?? 20"
        :page-sizes="[20, 50, 100]"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </el-card>

    <!-- 减免申请弹窗 -->
    <el-dialog v-model="reductionVisible" title="申请减免" width="440px" destroy-on-close>
      <el-descriptions :column="1" border class="mb-16">
        <el-descriptions-item label="应收编号">{{ reductionRow?.receivableCode }}</el-descriptions-item>
        <el-descriptions-item label="实际应收">{{ fmt(reductionRow?.actualAmount) }}</el-descriptions-item>
        <el-descriptions-item label="欠费金额">
          <span class="text-danger">{{ fmt(reductionRow?.outstandingAmount) }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <el-form :model="reductionForm" label-width="90px">
        <el-form-item label="减免金额" required>
          <el-input-number
            v-model="reductionForm.deductionAmount"
            :min="0.01"
            :max="reductionRow?.outstandingAmount ?? 9999999"
            :precision="2"
            controls-position="right"
            style="width:220px"
          />
          <span style="margin-left:8px;font-size:12px;color:#909399">
            最多 {{ fmt(reductionRow?.outstandingAmount) }} 元
          </span>
        </el-form-item>
        <el-form-item label="减免原因" required>
          <el-input v-model="reductionForm.reason" type="textarea" :rows="3" placeholder="请填写减免原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reductionVisible = false">取消</el-button>
        <el-button type="primary" :loading="reductionLoading" @click="submitReduction">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 调整申请弹窗 -->
    <el-dialog v-model="adjustVisible" title="申请调整" width="440px" destroy-on-close>
      <el-descriptions :column="1" border class="mb-16">
        <el-descriptions-item label="应收编号">{{ adjustRow?.receivableCode }}</el-descriptions-item>
        <el-descriptions-item label="原始应收">{{ fmt(adjustRow?.originalAmount) }}</el-descriptions-item>
        <el-descriptions-item label="实际应收">{{ fmt(adjustRow?.actualAmount) }}</el-descriptions-item>
        <el-descriptions-item label="欠费金额">
          <span class="text-danger">{{ fmt(adjustRow?.outstandingAmount) }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <el-form :model="adjustForm" label-width="90px">
        <el-form-item label="调整类型" required>
          <el-radio-group v-model="adjustForm.adjustType">
            <el-radio :value="1">增加应收</el-radio>
            <el-radio :value="2">减少应收</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="调整金额" required>
          <el-input-number
            v-model="adjustForm.adjustAmount"
            :min="0.01"
            :precision="2"
            controls-position="right"
            style="width:220px"
          />
        </el-form-item>
        <el-form-item label="调整原因" required>
          <el-input v-model="adjustForm.reason" type="textarea" :rows="3" placeholder="请填写调整原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" :loading="adjustLoading" @click="submitAdjust">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Refresh } from '@element-plus/icons-vue'
import {
  getReceivablePage,
  getReceivableSummary,
  getOverdueStatistics,
  exportReceivable,
  refreshOverdueDays,
  applyDeduction,
  applyAdjustment,
  markPrinted,
  type ReceivableDetailVO,
  type ReceivableSummaryVO,
  type OverdueStatisticsVO,
  type ReceivableQueryDTO,
} from '@/api/fin/receivable'

const activeTab = ref('detail')
const loading = ref(false)

// 明细列表
const detailList = ref<ReceivableDetailVO[]>([])
// 汇总列表
const summaryList = ref<ReceivableSummaryVO[]>([])
// 欠费统计
const overdueStats = ref<OverdueStatisticsVO | null>(null)

const total = ref(0)
const dueDateRange = ref<[string, string] | null>(null)

const query = reactive<ReceivableQueryDTO>({
  pageNum: 1,
  pageSize: 20,
})

function resetQuery() {
  query.receivableCode = undefined
  query.contractId = undefined
  query.status = undefined
  query.accrualMonth = undefined
  query.overdue = undefined
  dueDateRange.value = null
  query.dueDateFrom = undefined
  query.dueDateTo = undefined
  loadData()
}

async function loadData() {
  if (dueDateRange.value) {
    query.dueDateFrom = dueDateRange.value[0]
    query.dueDateTo = dueDateRange.value[1]
  } else {
    query.dueDateFrom = undefined
    query.dueDateTo = undefined
  }

  loading.value = true
  try {
    if (activeTab.value === 'detail') {
      const res = await getReceivablePage(query)
      const page = res.data?.data
      detailList.value = page?.records ?? []
      total.value = page?.total ?? 0
    } else if (activeTab.value === 'summary') {
      const res = await getReceivableSummary({
        contractId: query.contractId,
        projectId: query.projectId,
        merchantId: query.merchantId,
      })
      summaryList.value = res.data?.data?.records ?? []
      total.value = res.data?.data?.total ?? 0
    }
  } finally {
    loading.value = false
  }
}

async function loadOverdueStats() {
  loading.value = true
  try {
    const res = await getOverdueStatistics()
    overdueStats.value = res.data?.data ?? null
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  query.pageNum = 1
  if (activeTab.value === 'overdue') {
    loadOverdueStats()
  } else {
    loadData()
  }
}

function handlePageChange(page: number) {
  query.pageNum = page
  loadData()
}

function handleSizeChange(size: number) {
  query.pageSize = size
  query.pageNum = 1
  loadData()
}

async function handleExport() {
  try {
    const res = await exportReceivable(query)
    const url = URL.createObjectURL(new Blob([res.data]))
    const a = document.createElement('a')
    a.href = url
    a.download = `应收明细_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('导出失败')
  }
}

async function handleRefreshOverdue() {
  try {
    await refreshOverdueDays()
    ElMessage.success('逾期状态已刷新')
    loadData()
    loadOverdueStats()
  } catch {
    ElMessage.error('刷新失败')
  }
}

// ─── 表格多选（打印账单用）──────────────────────────────────────────────────
const selectedRows = ref<ReceivableDetailVO[]>([])
function handleSelectionChange(rows: ReceivableDetailVO[]) {
  selectedRows.value = rows
}

async function handleMarkPrinted() {
  if (!selectedRows.value.length) return
  const ids = selectedRows.value.map(r => r.id)
  try {
    await markPrinted(ids)
    ElMessage.success(`已标记 ${ids.length} 条为已打印`)
    loadData()
  } catch {
    ElMessage.error('标记失败')
  }
}

// ─── 减免申请弹窗 ─────────────────────────────────────────────────────────────
const reductionVisible = ref(false)
const reductionLoading = ref(false)
const reductionRow = ref<ReceivableDetailVO | null>(null)
const reductionForm = reactive({ receivableId: 0, deductionAmount: 0, reason: '' })

function openReductionDialog(row: ReceivableDetailVO) {
  reductionRow.value = row
  reductionForm.receivableId = row.id
  reductionForm.deductionAmount = 0
  reductionForm.reason = ''
  reductionVisible.value = true
}

async function submitReduction() {
  if (!reductionForm.deductionAmount || !reductionForm.reason.trim()) {
    ElMessage.warning('请填写减免金额和原因')
    return
  }
  reductionLoading.value = true
  try {
    await applyDeduction(reductionForm)
    ElMessage.success('减免申请已提交，等待审批')
    reductionVisible.value = false
    loadData()
  } catch {
    ElMessage.error('申请失败')
  } finally {
    reductionLoading.value = false
  }
}

// ─── 调整申请弹窗 ─────────────────────────────────────────────────────────────
const adjustVisible = ref(false)
const adjustLoading = ref(false)
const adjustRow = ref<ReceivableDetailVO | null>(null)
const adjustForm = reactive({ receivableId: 0, adjustType: 1 as 1 | 2, adjustAmount: 0, reason: '' })

function openAdjustDialog(row: ReceivableDetailVO) {
  adjustRow.value = row
  adjustForm.receivableId = row.id
  adjustForm.adjustType = 1
  adjustForm.adjustAmount = 0
  adjustForm.reason = ''
  adjustVisible.value = true
}

async function submitAdjust() {
  if (!adjustForm.adjustAmount || !adjustForm.reason.trim()) {
    ElMessage.warning('请填写调整金额和原因')
    return
  }
  adjustLoading.value = true
  try {
    await applyAdjustment(adjustForm)
    ElMessage.success('调整申请已提交，等待审批')
    adjustVisible.value = false
    loadData()
  } catch {
    ElMessage.error('申请失败')
  } finally {
    adjustLoading.value = false
  }
}

// 状态标签颜色
function statusTagType(status: number): 'success' | 'warning' | 'info' | 'danger' | undefined {
  const map: Record<number, 'success' | 'warning' | 'info' | 'danger'> = {
    0: 'warning',   // 待收
    1: 'warning',   // 部分收款
    2: 'success',   // 已收清
    3: 'info',      // 减免
    9: 'danger',    // 作废
  }
  return map[status]
}

// 金额格式化
function fmt(val?: number | null): string {
  if (val == null) return '-'
  return val.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

onMounted(() => {
  loadData()
  loadOverdueStats()
})
</script>

<style scoped lang="scss">
.fin-receivable-page {
  .stat-row {
    margin-bottom: 16px;

    .stat-card {
      text-align: center;
      .stat-value {
        font-size: 22px;
        font-weight: 700;
        margin-bottom: 4px;
      }
      .stat-label {
        font-size: 13px;
        color: #888;
      }
    }
    .stat-total .stat-value { color: #e6a23c; }
    .stat-30 .stat-value   { color: #f56c6c; }
    .stat-90 .stat-value   { color: #f56c6c; }
    .stat-over90 .stat-value { color: #909399; }
  }

  .filter-form {
    margin-bottom: 8px;
  }

  .pagination {
    margin-top: 16px;
    justify-content: flex-end;
  }

  .text-danger { color: #f56c6c; }
  .mb-16 { margin-bottom: 16px; }
}
</style>
