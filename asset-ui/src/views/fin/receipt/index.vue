<template>
  <div class="fin-receipt-page">
    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form :model="query" inline>
        <el-form-item label="收款单号">
          <el-input v-model="query.receiptCode" placeholder="收款单号" clearable style="width:160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="合同ID">
          <el-input v-model.number="query.contractId" placeholder="合同ID" clearable style="width:120px" />
        </el-form-item>
        <el-form-item label="核销状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:120px">
            <el-option label="待核销" :value="0" />
            <el-option label="部分核销" :value="1" />
            <el-option label="已全部核销" :value="2" />
            <el-option label="已作废" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="款项类型">
          <el-select v-model="query.isUnnamed" placeholder="全部" clearable style="width:110px">
            <el-option label="正常款项" :value="0" />
            <el-option label="未名款项" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="收款日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width:230px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表卡片 -->
    <el-card shadow="never" class="table-card">
      <div class="card-header">
        <div class="header-left">
          <span class="header-title">收款单列表</span>
          <span class="count-tag">共 {{ total }} 条</span>
        </div>
        <div class="header-actions">
          <el-button type="primary" :icon="Plus" @click="handleCreate">新增收款单</el-button>
        </div>
      </div>

      <div class="table-body">
        <el-table :data="tableData" v-loading="loading" stripe border style="width:100%">
          <el-table-column prop="receiptCode" label="收款单号" width="180" fixed />
          <el-table-column prop="contractName" label="合同" min-width="150" show-overflow-tooltip />
          <el-table-column prop="merchantName" label="商家" width="120" show-overflow-tooltip />
          <el-table-column prop="payerName" label="付款方" width="140" show-overflow-tooltip />
          <el-table-column prop="totalAmount" label="收款金额(元)" width="130" align="right">
            <template #default="{ row }">
              <span class="amount-text">{{ formatAmount(row.totalAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="核销进度" width="160">
            <template #default="{ row }">
              <div class="write-off-progress">
                <el-progress
                  :percentage="calcWriteOffPercent(row)"
                  :status="row.status === 2 ? 'success' : ''"
                  :stroke-width="8"
                />
                <span class="progress-text">{{ formatAmount(row.writeOffAmount) }} / {{ formatAmount(row.totalAmount) }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="paymentMethodName" label="收款方式" width="100" />
          <el-table-column prop="receiptDate" label="收款日期" width="110" align="center" />
          <el-table-column label="状态" width="130" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">{{ row.statusName }}</el-tag>
              <el-tag v-if="row.isUnnamed === 1" type="warning" size="small" style="margin-left:4px">未名</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="handleView(row)">查看</el-button>
              <el-button
                v-if="row.status !== 3"
                link type="primary" size="small"
                @click="handleEdit(row)"
                :disabled="row.status !== 0"
              >编辑</el-button>
              <el-button
                v-if="row.isUnnamed === 1 && row.status !== 3"
                link type="warning" size="small"
                @click="handleBind(row)"
              >归名</el-button>
              <el-button
                v-if="row.status !== 3 && (row.status === 0 || row.status === 1)"
                link type="success" size="small"
                @click="goWriteOff(row)"
              >核销</el-button>
              <el-button
                v-if="row.status === 0"
                link type="danger" size="small"
                @click="handleCancel(row)"
              >作废</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination">
          <el-pagination
            v-model:current-page="query.pageNum"
            v-model:page-size="query.pageSize"
            :total="total"
            :page-sizes="[20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            background
            @change="loadData"
          />
        </div>
      </div>
    </el-card>

    <!-- 详情/新增/编辑 抽屉 -->
    <ReceiptForm
      v-model:visible="formVisible"
      :mode="formMode"
      :row-id="currentId"
      @saved="loadData"
    />

    <!-- 作废确认弹窗 -->
    <el-dialog v-model="cancelVisible" title="作废收款单" width="400px" append-to-body>
      <el-form>
        <el-form-item label="作废原因">
          <el-input v-model="cancelReason" type="textarea" :rows="3" placeholder="请输入作废原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelVisible = false">取消</el-button>
        <el-button type="danger" :loading="cancelLoading" @click="doCancel">确认作废</el-button>
      </template>
    </el-dialog>

    <!-- 归名弹窗 -->
    <el-dialog v-model="bindVisible" title="未名款项归名" width="400px" append-to-body>
      <el-form label-width="80px">
        <el-form-item label="合同ID">
          <el-input-number v-model="bindContractId" :min="1" controls-position="right" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindVisible = false">取消</el-button>
        <el-button type="primary" :loading="bindLoading" @click="doBind">确认归名</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { getReceiptPage, cancelReceipt, bindReceipt, type ReceiptQueryDTO, type ReceiptDetailVO } from '@/api/fin/receipt'
import ReceiptForm from './form.vue'

const router = useRouter()

// ─── 列表状态 ──────────────────────────────────────────────────────────────
const loading = ref(false)
const tableData = ref<ReceiptDetailVO[]>([])
const total = ref(0)

const query = ref<ReceiptQueryDTO>({
  pageNum: 1,
  pageSize: 20,
})
const dateRange = ref<string[]>([])

watch(dateRange, (val) => {
  query.value.receiptDateFrom = val?.[0] || undefined
  query.value.receiptDateTo = val?.[1] || undefined
})

async function loadData() {
  loading.value = true
  try {
    const res = await getReceiptPage(query.value)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.value.pageNum = 1
  loadData()
}

function handleReset() {
  query.value = { pageNum: 1, pageSize: 20 }
  dateRange.value = []
  loadData()
}

onMounted(loadData)

// ─── 表单（新增/编辑/查看）─────────────────────────────────────────────────
const formVisible = ref(false)
const formMode = ref<'create' | 'edit' | 'view'>('create')
const currentId = ref<number | undefined>()

function handleCreate() {
  formMode.value = 'create'
  currentId.value = undefined
  formVisible.value = true
}

function handleView(row: ReceiptDetailVO) {
  formMode.value = 'view'
  currentId.value = row.id
  formVisible.value = true
}

function handleEdit(row: ReceiptDetailVO) {
  formMode.value = 'edit'
  currentId.value = row.id
  formVisible.value = true
}

// ─── 作废 ─────────────────────────────────────────────────────────────────
const cancelVisible = ref(false)
const cancelReason = ref('')
const cancelLoading = ref(false)
const cancelTargetId = ref<number>(0)

function handleCancel(row: ReceiptDetailVO) {
  cancelTargetId.value = row.id
  cancelReason.value = ''
  cancelVisible.value = true
}

async function doCancel() {
  cancelLoading.value = true
  try {
    await cancelReceipt(cancelTargetId.value, cancelReason.value)
    ElMessage.success('作废成功')
    cancelVisible.value = false
    loadData()
  } catch {
    // 错误由 request 拦截器统一处理
  } finally {
    cancelLoading.value = false
  }
}

// ─── 归名 ─────────────────────────────────────────────────────────────────
const bindVisible = ref(false)
const bindContractId = ref<number>(0)
const bindLoading = ref(false)
const bindTargetId = ref<number>(0)

function handleBind(row: ReceiptDetailVO) {
  bindTargetId.value = row.id
  bindContractId.value = 0
  bindVisible.value = true
}

async function doBind() {
  if (!bindContractId.value) {
    ElMessage.warning('请输入合同ID')
    return
  }
  bindLoading.value = true
  try {
    await bindReceipt(bindTargetId.value, bindContractId.value)
    ElMessage.success('归名成功')
    bindVisible.value = false
    loadData()
  } catch {
  } finally {
    bindLoading.value = false
  }
}

// ─── 跳转核销管理（带收款单筛选） ─────────────────────────────────────────
function goWriteOff(row: ReceiptDetailVO) {
  router.push({ path: '/fin/write-offs', query: { receiptId: row.id } })
}

// ─── 工具函数 ─────────────────────────────────────────────────────────────
function formatAmount(val?: number) {
  if (val == null) return '0.00'
  return val.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

function calcWriteOffPercent(row: ReceiptDetailVO) {
  if (!row.totalAmount) return 0
  return Math.round(((row.writeOffAmount || 0) / row.totalAmount) * 100)
}

type TagType = 'success' | 'primary' | 'warning' | 'info' | 'danger' | undefined
function statusTagType(status: number): TagType {
  const map: Record<number, TagType> = { 0: 'warning', 1: undefined, 2: 'success', 3: 'info' }
  return map[status]
}
</script>

<style scoped lang="scss">
.fin-receipt-page {
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

  .header-left { display: flex; align-items: center; gap: 10px; }

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

  .header-actions { display: flex; gap: 8px; align-items: center; }
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

    .el-table__row:hover > td.el-table__cell { background-color: #f0f7ff !important; }
    .el-table__row--striped > td.el-table__cell { background-color: #fafbfc; }
    td.el-table__cell { border-bottom: 1px solid #f4f6f9; }
  }
}

.pagination { margin-top: 14px; display: flex; justify-content: flex-end; }

.amount-text { font-weight: 600; color: #1e293b; }
.write-off-progress { display: flex; flex-direction: column; gap: 2px; }
.progress-text { font-size: 11px; color: #94a3b8; }
</style>
