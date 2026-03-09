<template>
  <div class="fin-prepayment-page">
    <!-- 搜索栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="合同ID">
          <el-input
            v-model.number="searchForm.contractId"
            placeholder="输入合同ID"
            style="width:180px"
            clearable
            @keyup.enter="loadAccount"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadAccount">查询</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 账户余额卡片（仅查询合同后显示） -->
    <template v-if="account">
      <el-row :gutter="16">
        <el-col :span="6">
          <div class="balance-stat-card main-balance">
            <div class="bsc-label">可用余额（元）</div>
            <div class="bsc-amount">{{ formatAmount(account.balance) }}</div>
            <div class="bsc-sub">合同：{{ account.contractCode || account.contractId }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="balance-stat-card info-block">
            <div class="info-row"><span class="info-label">商家</span><span class="info-value">{{ account.merchantName || '-' }}</span></div>
            <div class="info-row"><span class="info-label">项目</span><span class="info-value">{{ account.projectName || '-' }}</span></div>
            <div class="info-row"><span class="info-label">合同名称</span><span class="info-value">{{ account.contractName || '-' }}</span></div>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="balance-stat-card action-block">
            <el-button type="success" :icon="Plus" size="large" @click="openDepositDialog">录入预收款</el-button>
            <el-button type="primary" :icon="Connection" size="large" @click="openOffsetDialog">抵冲应收</el-button>
            <el-button type="warning" :icon="Money" size="large" @click="openRefundDialog">申请退款</el-button>
          </div>
        </el-col>
      </el-row>
    </template>

    <el-card v-if="searched && !account" shadow="never" class="empty-card">
      <el-empty description="该合同暂无预收款账户，录入后自动创建" :image-size="100" />
    </el-card>

    <!-- 流水明细（始终显示） -->
    <el-card shadow="never" class="table-card">
      <div class="card-header">
        <div class="header-left">
          <span class="header-title">预收款流水记录</span>
        </div>
        <div class="header-actions">
          <el-select
            v-model="txQuery.transType"
            placeholder="全部类型"
            clearable
            style="width:130px"
            @change="loadTransactions"
          >
            <el-option label="转入" :value="1" />
            <el-option label="抵冲" :value="2" />
            <el-option label="退款" :value="3" />
          </el-select>
        </div>
      </div>

      <div class="table-body">
        <el-table :data="transactions" v-loading="txLoading" stripe border style="width:100%">
          <el-table-column label="交易日期" prop="transDate" width="110" align="center" />
          <el-table-column label="交易类型" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="transTypeTag(row.transType)" size="small">{{ transTypeLabel(row.transType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="金额（元）" align="right" width="130">
            <template #default="{ row }">
              <span :class="row.transType === 1 ? 'amount-in' : 'amount-out'">
                {{ row.transType === 1 ? '+' : '-' }}{{ formatAmount(row.amount) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="余额快照（元）" align="right" width="130">
            <template #default="{ row }">{{ formatAmount(row.balanceAfter) }}</template>
          </el-table-column>
          <el-table-column label="关联单据" prop="sourceCode" min-width="160" show-overflow-tooltip />
          <el-table-column label="备注" prop="remark" min-width="180" show-overflow-tooltip />
          <el-table-column label="创建时间" prop="createTime" width="160" align="center" />
        </el-table>

        <div class="pagination">
          <el-pagination
            v-model:current-page="txQuery.pageNum"
            v-model:page-size="txQuery.pageSize"
            :total="txTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            background
            @change="loadTransactions"
          />
        </div>
      </div>
    </el-card>

    <!-- ─── 录入预收款弹窗 ─── -->
    <el-dialog v-model="depositVisible" title="录入预收款" width="440px" destroy-on-close>
      <el-form ref="depositFormRef" :model="depositForm" :rules="depositRules" label-width="100px">
        <el-form-item label="合同ID" prop="contractId">
          <el-input-number v-model="depositForm.contractId" :min="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="录入金额" prop="amount">
          <el-input-number v-model="depositForm.amount" :min="0.01" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="关联单据号">
          <el-input v-model="depositForm.sourceCode" placeholder="收款单号、凭证号等" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="depositForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="depositVisible = false">取消</el-button>
        <el-button type="primary" :loading="depositLoading" @click="submitDeposit">确认录入</el-button>
      </template>
    </el-dialog>

    <!-- ─── 抵冲应收弹窗 ─── -->
    <el-dialog v-model="offsetVisible" title="预收款抵冲应收" width="460px" destroy-on-close>
      <el-alert class="mb-3" :title="`当前可用余额：¥ ${formatAmount(account?.balance)}`" type="info" show-icon :closable="false" />
      <el-form ref="offsetFormRef" :model="offsetForm" :rules="offsetRules" label-width="100px">
        <el-form-item label="应收记录ID" prop="receivableId">
          <el-input-number v-model="offsetForm.receivableId" :min="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="抵冲金额" prop="amount">
          <el-input-number v-model="offsetForm.amount" :min="0.01" :max="account?.balance ?? 999999999" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="offsetForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="offsetVisible = false">取消</el-button>
        <el-button type="primary" :loading="offsetLoading" @click="submitOffset">确认抵冲</el-button>
      </template>
    </el-dialog>

    <!-- ─── 退款弹窗 ─── -->
    <el-dialog v-model="refundVisible" title="预收款退款" width="480px" destroy-on-close>
      <el-alert class="mb-3" :title="`当前可用余额：¥ ${formatAmount(account?.balance)}`" type="warning" show-icon :closable="false" />
      <el-form ref="refundFormRef" :model="refundForm" :rules="refundRules" label-width="100px">
        <el-form-item label="退款金额" prop="amount">
          <el-input-number v-model="refundForm.amount" :min="0.01" :max="account?.balance ?? 999999999" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="收款账号" prop="bankAccount">
          <el-input v-model="refundForm.bankAccount" placeholder="银行账号" />
        </el-form-item>
        <el-form-item label="收款人" prop="payee">
          <el-input v-model="refundForm.payee" placeholder="收款人姓名" />
        </el-form-item>
        <el-form-item label="开户行">
          <el-input v-model="refundForm.bankName" placeholder="开户行名称（选填）" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="refundForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundVisible = false">取消</el-button>
        <el-button type="warning" :loading="refundLoading" @click="submitRefund">确认退款</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Connection, Money } from '@element-plus/icons-vue'
import {
  getPrepayAccount,
  getPrepayTransactions,
  depositPrepay,
  offsetPrepay,
  refundPrepay,
  type PrepayAccountVO,
  type PrepayTransaction,
} from '@/api/fin/prepayment'

const searchForm = reactive({ contractId: undefined as number | undefined })
const account = ref<PrepayAccountVO | null>(null)
const searched = ref(false)

async function loadAccount() {
  if (!searchForm.contractId) {
    account.value = null
    searched.value = false
    txQuery.contractId = undefined
    txQuery.pageNum = 1
    loadTransactions()
    return
  }
  searched.value = false
  try {
    const res: any = await getPrepayAccount(searchForm.contractId)
    account.value = res ?? null
    searched.value = true
    txQuery.contractId = searchForm.contractId
    txQuery.pageNum = 1
    loadTransactions()
  } catch { account.value = null; searched.value = true }
}

function resetSearch() {
  searchForm.contractId = undefined
  account.value = null
  searched.value = false
  txQuery.contractId = undefined
  txQuery.transType = undefined
  txQuery.pageNum = 1
  loadTransactions()
}

onMounted(() => loadTransactions())

const txLoading = ref(false)
const transactions = ref<PrepayTransaction[]>([])
const txTotal = ref(0)
const txQuery = reactive({
  contractId: undefined as number | undefined,
  transType: undefined as number | undefined,
  pageNum: 1,
  pageSize: 10,
})

async function loadTransactions() {
  txLoading.value = true
  try {
    const res: any = await getPrepayTransactions(txQuery)
    transactions.value = res.records ?? []
    txTotal.value = res.total ?? 0
  } finally { txLoading.value = false }
}

function formatAmount(v?: number | null) {
  if (v == null) return '0.00'
  return Number(v).toFixed(2)
}

function transTypeLabel(t?: number) {
  const m: Record<number, string> = { 1: '转入', 2: '抵冲', 3: '退款' }
  return t ? m[t] ?? '-' : '-'
}

type TagType = 'success' | 'primary' | 'warning' | 'info' | 'danger' | undefined
function transTypeTag(t?: number): TagType {
  const m: Record<number, TagType> = { 1: 'success', 2: 'primary', 3: 'warning' }
  return t ? m[t] : undefined
}

// 录入预收款
const depositVisible = ref(false)
const depositLoading = ref(false)
const depositFormRef = ref()
const depositForm = reactive({ contractId: 0, amount: 0, sourceCode: '', remark: '' })
const depositRules = {
  contractId: [{ required: true, message: '合同ID不能为空', trigger: 'blur' }],
  amount: [{ required: true, message: '金额不能为空', trigger: 'blur' }],
}

function openDepositDialog() {
  depositForm.contractId = searchForm.contractId ?? 0
  depositForm.amount = 0; depositForm.sourceCode = ''; depositForm.remark = ''
  depositVisible.value = true
}

async function submitDeposit() {
  await depositFormRef.value?.validate()
  depositLoading.value = true
  try {
    await depositPrepay({ ...depositForm })
    ElMessage.success('预收款录入成功')
    depositVisible.value = false; loadAccount()
  } finally { depositLoading.value = false }
}

// 抵冲应收
const offsetVisible = ref(false)
const offsetLoading = ref(false)
const offsetFormRef = ref()
const offsetForm = reactive({ contractId: 0, receivableId: 0, amount: 0, remark: '' })
const offsetRules = {
  receivableId: [{ required: true, message: '应收ID不能为空', trigger: 'blur' }],
  amount: [{ required: true, message: '抵冲金额不能为空', trigger: 'blur' }],
}

function openOffsetDialog() {
  offsetForm.contractId = account.value?.contractId ?? 0
  offsetForm.receivableId = 0; offsetForm.amount = 0; offsetForm.remark = ''
  offsetVisible.value = true
}

async function submitOffset() {
  await offsetFormRef.value?.validate()
  offsetLoading.value = true
  try {
    await offsetPrepay({ ...offsetForm })
    ElMessage.success('抵冲成功，应收已更新')
    offsetVisible.value = false; loadAccount()
  } finally { offsetLoading.value = false }
}

// 退款
const refundVisible = ref(false)
const refundLoading = ref(false)
const refundFormRef = ref()
const refundForm = reactive({ contractId: 0, amount: 0, bankName: '', bankAccount: '', payee: '', remark: '' })
const refundRules = {
  amount: [{ required: true, message: '退款金额不能为空', trigger: 'blur' }],
  bankAccount: [{ required: true, message: '银行账号不能为空', trigger: 'blur' }],
  payee: [{ required: true, message: '收款人不能为空', trigger: 'blur' }],
}

function openRefundDialog() {
  refundForm.contractId = account.value?.contractId ?? 0
  refundForm.amount = 0
  refundForm.bankName = refundForm.bankAccount = refundForm.payee = refundForm.remark = ''
  refundVisible.value = true
}

async function submitRefund() {
  await refundFormRef.value?.validate()
  await ElMessageBox.confirm(
    `确认退还预收款 ¥${refundForm.amount.toFixed(2)} 元？操作立即生效，不可撤销。`,
    '确认退款',
    { type: 'warning', confirmButtonText: '确认退款', cancelButtonText: '取消' }
  )
  refundLoading.value = true
  try {
    await refundPrepay({ ...refundForm })
    ElMessage.success('退款成功')
    refundVisible.value = false; loadAccount()
  } finally { refundLoading.value = false }
}
</script>

<style scoped lang="scss">
.fin-prepayment-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ── 搜索栏 ── */
.filter-card {
  border-radius: 12px !important;
  border: 1px solid rgba(0,0,0,0.06) !important;
  :deep(.el-card__body) { padding: 14px 20px; }
  :deep(.el-form-item) { margin-bottom: 0; }
}

/* ── 余额卡片 ── */
.balance-stat-card {
  padding: 18px 20px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid rgba(0,0,0,0.06);
  transition: box-shadow 0.2s;
  height: 100%;
  box-sizing: border-box;
  &:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.08); }

  .bsc-label { font-size: 12px; color: #94a3b8; margin-bottom: 8px; }
  .bsc-amount { font-size: 26px; font-weight: 700; margin-bottom: 6px; }
  .bsc-sub { font-size: 12px; color: #cbd5e1; }
}

.main-balance {
  border-top: 3px solid #3b82f6;
  .bsc-amount { color: #3b82f6; }
}

.info-block {
  border-top: 3px solid #e2e8f0;
  font-size: 12px;
  .info-row {
    display: flex; justify-content: space-between; align-items: center;
    margin-bottom: 8px;
    &:last-child { margin-bottom: 0; }
  }
  .info-label { color: #94a3b8; }
  .info-value {
    color: #1e293b; font-weight: 500;
    overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 150px;
  }
}

.action-block {
  border-top: 3px solid #e2e8f0;
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;

  :deep(.el-button) { flex: 1; min-width: 110px; }
}

/* ── 流水表格卡片 ── */
.table-card {
  border-radius: 12px !important;
  border: 1px solid rgba(0,0,0,0.06) !important;
  overflow: hidden;
  :deep(.el-card__body) { padding: 0; }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid #f1f5f9;

  .header-left { display: flex; align-items: center; gap: 10px; }

  .header-title {
    font-size: 15px; font-weight: 600; color: #1e293b;
    display: flex; align-items: center; gap: 8px;
    &::before {
      content: '';
      display: inline-block;
      width: 3px; height: 16px;
      background: linear-gradient(180deg, #3b82f6, #60a5fa);
      border-radius: 2px;
    }
  }

  .header-actions { display: flex; gap: 8px; align-items: center; }
}

.table-body {
  padding: 16px 20px;

  :deep(.el-table) {
    border-radius: 8px; overflow: hidden;

    .el-table__header-wrapper th.el-table__cell {
      background: #f8fafc; color: #64748b; font-weight: 600; font-size: 13px;
      border-bottom: 1px solid #e8edf3;
    }
    .el-table__row:hover > td.el-table__cell { background-color: #f0f7ff !important; }
    .el-table__row--striped > td.el-table__cell { background-color: #fafbfc; }
    td.el-table__cell { border-bottom: 1px solid #f4f6f9; }
  }
}

.pagination { margin-top: 14px; display: flex; justify-content: flex-end; }

/* ── 空状态卡片 ── */
.empty-card {
  border-radius: 12px !important;
  border: 1px solid rgba(0,0,0,0.06) !important;
}

.amount-in  { color: #22c55e; font-weight: 600; }
.amount-out { color: #f56c6c; font-weight: 600; }
.mb-3 { margin-bottom: 12px; }
</style>
