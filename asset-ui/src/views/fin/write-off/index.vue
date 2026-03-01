<template>
  <div class="fin-writeoff-page">
    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form :model="query" inline>
        <el-form-item label="核销单号">
          <el-input v-model="query.writeOffCode" placeholder="核销单号" clearable style="width:160px" />
        </el-form-item>
        <el-form-item label="合同ID">
          <el-input-number
            v-model="query.contractId"
            :min="1"
            controls-position="right"
            placeholder="合同ID"
            style="width:130px"
          />
        </el-form-item>
        <el-form-item label="核销类型">
          <el-select v-model="query.writeOffType" placeholder="全部" clearable style="width:120px">
            <el-option label="收款核销" :value="1" />
            <el-option label="保证金核销" :value="2" />
            <el-option label="预收款核销" :value="3" />
            <el-option label="负数核销" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:110px">
            <el-option label="待审核" :value="0" />
            <el-option label="审核通过" :value="1" />
            <el-option label="已撤销" :value="2" />
          </el-select>
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
          <span class="header-title">核销单列表</span>
          <span class="count-tag">共 {{ total }} 条</span>
        </div>
        <div class="header-actions">
          <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增核销</el-button>
        </div>
      </div>

      <div class="table-body">
        <el-table v-loading="loading" :data="tableData" border stripe style="width:100%">
          <el-table-column label="核销单号" prop="writeOffCode" min-width="160" />
          <el-table-column label="收款单号" prop="receiptCode" min-width="160" />
          <el-table-column label="合同名称" prop="contractName" min-width="160" show-overflow-tooltip />
          <el-table-column label="商家名称" prop="merchantName" min-width="120" show-overflow-tooltip />
          <el-table-column label="核销类型" prop="writeOffTypeName" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="typeTagType(row.writeOffType)" size="small">
                {{ row.writeOffTypeName || '-' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="核销总金额" prop="totalAmount" width="130" align="right">
            <template #default="{ row }">
              <span :class="row.totalAmount < 0 ? 'amount-negative' : 'amount-positive'">
                ¥{{ row.totalAmount?.toFixed(2) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="状态" prop="statusName" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">{{ row.statusName }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" prop="createTime" width="160" align="center" />
          <el-table-column label="操作" width="160" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
              <el-button
                v-if="row.status === 0"
                link type="warning" size="small"
                @click="handleCancel(row)"
              >撤销</el-button>
              <el-button
                v-if="row.status === 0 && row.approvalId"
                link type="success" size="small"
                @click="openApproveDialog(row)"
              >审批</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination">
          <el-pagination
            v-model:current-page="query.pageNum"
            v-model:page-size="query.pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            background
            @change="loadData"
          />
        </div>
      </div>
    </el-card>

    <!-- 新增核销弹窗 -->
    <el-dialog
      v-model="createDialog.visible"
      title="新增核销申请"
      width="780px"
      destroy-on-close
    >
      <el-form :model="createForm" label-width="90px" :rules="createRules" ref="createFormRef">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="收款单ID" prop="receiptId">
              <el-input-number
                v-model="createForm.receiptId"
                :min="1"
                controls-position="right"
                style="width:100%"
                @change="onReceiptChange"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="核销类型" prop="writeOffType">
              <el-select v-model="createForm.writeOffType" style="width:100%">
                <el-option label="收款核销" :value="1" />
                <el-option label="负数核销" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">
          选择应收明细
          <span style="color:#94a3b8;font-size:12px;margin-left:8px">（可勾选多条，填写本次核销金额）</span>
        </el-divider>

        <div v-if="createForm.receiptId && writableList.length === 0" style="color:#94a3b8;padding:12px 0">
          该收款单对应合同暂无可核销应收记录
        </div>

        <el-table
          v-if="writableList.length > 0"
          :data="writableList"
          border
          size="small"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="45" />
          <el-table-column label="费项" prop="feeName" min-width="100" />
          <el-table-column label="权责月" prop="accrualMonth" width="90" />
          <el-table-column label="应收日期" prop="dueDate" width="110" />
          <el-table-column label="应收金额" prop="actualAmount" width="100" align="right">
            <template #default="{ row }">¥{{ row.actualAmount?.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="欠费金额" prop="outstandingAmount" width="100" align="right">
            <template #default="{ row }">
              <span class="amount-negative">¥{{ row.outstandingAmount?.toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="本次核销" width="130">
            <template #default="{ row }">
              <el-input-number
                v-if="selectedIds.has(row.id)"
                v-model="writeOffAmounts[row.id]"
                :precision="2"
                :min="0.01"
                :max="row.outstandingAmount"
                controls-position="right"
                size="small"
                style="width:100%"
              />
              <span v-else style="color:#cbd5e1">-</span>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="selectedIds.size > 0" class="writeoff-total">
          本次核销合计：
          <span class="total-amount">¥{{ totalWriteOff.toFixed(2) }}</span>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="createDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailDialog.visible" title="核销单详情" width="720px" destroy-on-close>
      <template v-if="detailData">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="核销单号">{{ detailData.writeOffCode }}</el-descriptions-item>
          <el-descriptions-item label="收款单号">{{ detailData.receiptCode }}</el-descriptions-item>
          <el-descriptions-item label="合同">{{ detailData.contractCode }} {{ detailData.contractName }}</el-descriptions-item>
          <el-descriptions-item label="商家">{{ detailData.merchantName }}</el-descriptions-item>
          <el-descriptions-item label="核销类型">
            <el-tag :type="typeTagType(detailData.writeOffType)" size="small">{{ detailData.writeOffTypeName }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(detailData.status)" size="small">{{ detailData.statusName }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="核销总金额" :span="2">
            <span :class="detailData.totalAmount < 0 ? 'amount-negative' : 'amount-positive'" style="font-size:16px;font-weight:600">
              ¥{{ detailData.totalAmount?.toFixed(2) }}
            </span>
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left" style="margin-top:16px">核销明细</el-divider>
        <el-table :data="detailData.details" border size="small">
          <el-table-column label="应收ID" prop="receivableId" width="90" />
          <el-table-column label="权责月" prop="accrualMonth" width="90" />
          <el-table-column label="核销金额" prop="writeOffAmount" width="110" align="right">
            <template #default="{ row }">¥{{ row.writeOffAmount?.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="超额转预存" prop="overpayAmount" width="110" align="right">
            <template #default="{ row }">
              {{ row.overpayAmount > 0 ? `¥${row.overpayAmount.toFixed(2)}` : '-' }}
            </template>
          </el-table-column>
        </el-table>
      </template>
      <template #footer>
        <el-button @click="detailDialog.visible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 手动审批弹窗（Dev/测试用） -->
    <el-dialog v-model="approveDialog.visible" title="手动审批（测试）" width="440px" destroy-on-close>
      <el-form :model="approveForm" label-width="90px">
        <el-form-item label="审批结果">
          <el-radio-group v-model="approveForm.approved">
            <el-radio :value="true">通过</el-radio>
            <el-radio :value="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="approveForm.comment" type="textarea" rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="approving" @click="handleApprove">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import {
  getWriteOffPage,
  getWritableReceivables,
  submitWriteOff,
  cancelWriteOff,
  approveCallback,
  getWriteOffDetail,
  type WriteOffDetailVO,
  type WritableReceivableVO,
} from '@/api/fin/writeOff'

const route = useRoute()

// ─── 列表与查询 ───────────────────────────────────────────────────────────────
const loading = ref(false)
const tableData = ref<WriteOffDetailVO[]>([])
const total = ref(0)
const query = reactive({
  writeOffCode: '',
  contractId: undefined as number | undefined,
  writeOffType: undefined as number | undefined,
  status: undefined as number | undefined,
  pageNum: 1,
  pageSize: 20,
})

async function loadData() {
  loading.value = true
  try {
    const res = await getWriteOffPage(query)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadData()
}

function handleReset() {
  query.writeOffCode = ''
  query.contractId = undefined
  query.writeOffType = undefined
  query.status = undefined
  query.pageNum = 1
  loadData()
}

// 支持从收款管理页带参数跳转（?receiptId=xxx）
onMounted(() => {
  const receiptId = route.query.receiptId
  if (receiptId) {
    query.receiptId = Number(receiptId)
  }
  loadData()
})

// ─── 标签颜色 ─────────────────────────────────────────────────────────────────
function statusTagType(status: number) {
  return ({ 0: 'warning', 1: 'success', 2: 'info' } as Record<number, string>)[status] ?? ''
}

function typeTagType(type: number) {
  return ({ 1: '', 2: 'warning', 3: 'success', 4: 'danger' } as Record<number, string>)[type] ?? ''
}

// ─── 撤销 ─────────────────────────────────────────────────────────────────────
async function handleCancel(row: WriteOffDetailVO) {
  await ElMessageBox.confirm(`确定撤销核销单「${row.writeOffCode}」？`, '撤销确认', { type: 'warning' })
  await cancelWriteOff(row.id)
  ElMessage.success('撤销成功')
  loadData()
}

// ─── 详情 ─────────────────────────────────────────────────────────────────────
const detailDialog = reactive({ visible: false })
const detailData = ref<WriteOffDetailVO | null>(null)

async function viewDetail(row: WriteOffDetailVO) {
  const res = await getWriteOffDetail(row.id)
  detailData.value = res.data
  detailDialog.visible = true
}

// ─── 手动审批弹窗 ─────────────────────────────────────────────────────────────
const approveDialog = reactive({ visible: false, approvalId: '' })
const approveForm = reactive({ approved: true, comment: '' })
const approving = ref(false)

function openApproveDialog(row: WriteOffDetailVO) {
  approveDialog.approvalId = row.approvalId!
  approveForm.approved = true
  approveForm.comment = ''
  approveDialog.visible = true
}

async function handleApprove() {
  approving.value = true
  try {
    await approveCallback(approveDialog.approvalId, approveForm.approved, approveForm.comment)
    ElMessage.success(approveForm.approved ? '审批通过' : '已驳回')
    approveDialog.visible = false
    loadData()
  } finally {
    approving.value = false
  }
}

// ─── 新增核销弹窗 ─────────────────────────────────────────────────────────────
const createDialog = reactive({ visible: false })
const createFormRef = ref<FormInstance>()
const submitting = ref(false)

const createForm = reactive({
  receiptId: undefined as number | undefined,
  writeOffType: 1,
})

const createRules: FormRules = {
  receiptId: [{ required: true, message: '请填写收款单ID', trigger: 'blur' }],
  writeOffType: [{ required: true, message: '请选择核销类型', trigger: 'change' }],
}

// 可核销应收列表
const writableList = ref<WritableReceivableVO[]>([])
// 当前勾选的应收ID集合
const selectedIds = ref<Set<number>>(new Set())
// 各应收本次核销金额
const writeOffAmounts = reactive<Record<number, number>>({})

// 核销合计
const totalWriteOff = computed(() => {
  let sum = 0
  selectedIds.value.forEach(id => { sum += writeOffAmounts[id] || 0 })
  return sum
})

function openCreateDialog() {
  createForm.receiptId = undefined
  createForm.writeOffType = 1
  writableList.value = []
  selectedIds.value = new Set()
  createDialog.visible = true
}

async function onReceiptChange(val: number | undefined) {
  if (!val) {
    writableList.value = []
    return
  }
  try {
    const res = await getWritableReceivables(val)
    writableList.value = res.data || []
    selectedIds.value = new Set()
    writableList.value.forEach(r => {
      writeOffAmounts[r.id] = r.outstandingAmount
    })
  } catch {
    writableList.value = []
  }
}

function handleSelectionChange(rows: WritableReceivableVO[]) {
  selectedIds.value = new Set(rows.map(r => r.id))
}

async function handleSubmit() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (selectedIds.value.size === 0) {
    ElMessage.warning('请至少选择一条应收明细')
    return
  }

  const items = Array.from(selectedIds.value).map(id => {
    const row = writableList.value.find(r => r.id === id)!
    return {
      receivableId: id,
      feeItemId: row.feeItemId,
      accrualMonth: row.accrualMonth,
      writeOffAmount: writeOffAmounts[id] || 0,
    }
  })

  submitting.value = true
  try {
    await submitWriteOff({
      receiptId: createForm.receiptId!,
      writeOffType: createForm.writeOffType,
      items,
    })
    ElMessage.success('核销申请已提交，等待审批')
    createDialog.visible = false
    loadData()
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.fin-writeoff-page {
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

.amount-positive { color: #22c55e; font-weight: 600; }
.amount-negative { color: #f56c6c; font-weight: 600; }

.writeoff-total {
  text-align: right;
  margin-top: 12px;
  font-size: 14px;
  color: #64748b;

  .total-amount {
    font-weight: 700;
    color: #3b82f6;
    font-size: 16px;
    margin-left: 4px;
  }
}
</style>
