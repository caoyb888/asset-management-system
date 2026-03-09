<template>
  <div class="fin-deposit-page">
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
        <el-col :span="4">
          <div class="balance-stat-card main-balance">
            <div class="bsc-label">可用余额</div>
            <div class="bsc-amount">{{ formatAmount(account.balance) }}</div>
          </div>
        </el-col>
        <el-col :span="4">
          <div class="balance-stat-card income">
            <div class="bsc-label">累计收入</div>
            <div class="bsc-amount">{{ formatAmount(account.totalIn) }}</div>
          </div>
        </el-col>
        <el-col :span="4">
          <div class="balance-stat-card offset">
            <div class="bsc-label">累计冲抵</div>
            <div class="bsc-amount">{{ formatAmount(account.totalOffset) }}</div>
          </div>
        </el-col>
        <el-col :span="4">
          <div class="balance-stat-card refund">
            <div class="bsc-label">累计退款</div>
            <div class="bsc-amount">{{ formatAmount(account.totalRefund) }}</div>
          </div>
        </el-col>
        <el-col :span="4">
          <div class="balance-stat-card forfeit">
            <div class="bsc-label">累计罚没</div>
            <div class="bsc-amount">{{ formatAmount(account.totalForfeit) }}</div>
          </div>
        </el-col>
        <el-col :span="4">
          <div class="balance-stat-card info-block">
            <div class="info-row"><span class="info-label">合同</span><span class="info-value">{{ account.contractCode || '-' }}</span></div>
            <div class="info-row"><span class="info-label">商家</span><span class="info-value">{{ account.merchantName || '-' }}</span></div>
            <div class="info-row"><span class="info-label">项目</span><span class="info-value">{{ account.projectName || '-' }}</span></div>
          </div>
        </el-col>
      </el-row>

      <!-- 操作按钮区 -->
      <el-card shadow="never" class="action-card">
        <div class="action-row">
          <el-button type="success" :icon="Plus" @click="openPayInDialog">缴纳保证金</el-button>
          <el-button type="primary" :icon="Connection" @click="openOffsetDialog">申请冲抵</el-button>
          <el-button type="warning" :icon="Money" @click="openRefundDialog">申请退款</el-button>
          <el-button type="danger" :icon="Warning" @click="openForfeitDialog">申请罚没</el-button>
        </div>
      </el-card>
    </template>

    <el-card v-if="searched && !account" shadow="never" class="empty-card">
      <el-empty description="该合同暂无保证金账户，缴纳后自动创建" :image-size="100" />
    </el-card>

    <!-- 流水明细（始终显示） -->
    <el-card shadow="never" class="table-card">
      <div class="card-header">
        <div class="header-left">
          <span class="header-title">保证金流水</span>
        </div>
        <div class="header-actions">
          <el-select
            v-model="txQuery.transType"
            placeholder="交易类型"
            clearable
            style="width:130px"
            @change="loadTransactions"
          >
            <el-option label="缴纳" :value="1" />
            <el-option label="冲抵" :value="2" />
            <el-option label="退款" :value="3" />
            <el-option label="罚没" :value="4" />
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
          <el-table-column label="余额快照（元）" prop="balanceAfter" align="right" width="130">
            <template #default="{ row }">{{ formatAmount(row.balanceAfter) }}</template>
          </el-table-column>
          <el-table-column label="单据号" prop="sourceCode" min-width="180" show-overflow-tooltip />
          <el-table-column label="原因" prop="reason" min-width="160" show-overflow-tooltip />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right" align="center">
            <template #default="{ row }">
              <el-button v-if="row.status === 0" link type="primary" size="small" @click="openApproveDialog(row)">审批回调</el-button>
            </template>
          </el-table-column>
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

    <!-- ─── 缴纳保证金弹窗 ─── -->
    <el-dialog v-model="payInVisible" title="缴纳保证金" width="440px" destroy-on-close>
      <el-form ref="payInFormRef" :model="payInForm" :rules="payInRules" label-width="90px">
        <el-form-item label="合同ID" prop="contractId">
          <el-input-number v-model="payInForm.contractId" :min="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="缴纳金额" prop="amount">
          <el-input-number v-model="payInForm.amount" :min="0.01" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="单据号" prop="sourceCode">
          <el-input v-model="payInForm.sourceCode" placeholder="银行流水号或凭证号" />
        </el-form-item>
        <el-form-item label="备注" prop="reason">
          <el-input v-model="payInForm.reason" type="textarea" :rows="2" placeholder="缴纳说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="payInVisible = false">取消</el-button>
        <el-button type="primary" :loading="payInLoading" @click="submitPayIn">确认缴纳</el-button>
      </template>
    </el-dialog>

    <!-- ─── 申请冲抵弹窗 ─── -->
    <el-dialog v-model="offsetVisible" title="申请保证金冲抵" width="460px" destroy-on-close>
      <el-alert class="mb-3" :title="`当前可用余额：¥ ${formatAmount(account?.balance)}`" type="info" show-icon :closable="false" />
      <el-form ref="offsetFormRef" :model="offsetForm" :rules="offsetRules" label-width="100px">
        <el-form-item label="冲抵应收ID" prop="receivableId">
          <el-input-number v-model="offsetForm.receivableId" :min="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="冲抵金额" prop="amount">
          <el-input-number v-model="offsetForm.amount" :min="0.01" :max="account?.balance ?? 999999999" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="原因" prop="reason">
          <el-input v-model="offsetForm.reason" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="offsetVisible = false">取消</el-button>
        <el-button type="primary" :loading="offsetLoading" @click="submitOffset">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- ─── 申请退款弹窗 ─── -->
    <el-dialog v-model="refundVisible" title="申请保证金退款" width="480px" destroy-on-close>
      <el-alert class="mb-3" :title="`当前可用余额：¥ ${formatAmount(account?.balance)}`" type="info" show-icon :closable="false" />
      <el-form ref="refundFormRef" :model="refundForm" :rules="refundRules" label-width="100px">
        <el-form-item label="退款金额" prop="amount">
          <el-input-number v-model="refundForm.amount" :min="0.01" :max="account?.balance ?? 999999999" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="收款账户" prop="bankAccount">
          <el-input v-model="refundForm.bankAccount" placeholder="银行账号" />
        </el-form-item>
        <el-form-item label="收款人" prop="payee">
          <el-input v-model="refundForm.payee" placeholder="收款人姓名" />
        </el-form-item>
        <el-form-item label="开户行">
          <el-input v-model="refundForm.bankName" placeholder="开户行名称" />
        </el-form-item>
        <el-form-item label="退款原因" prop="reason">
          <el-input v-model="refundForm.reason" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundVisible = false">取消</el-button>
        <el-button type="primary" :loading="refundLoading" @click="submitRefund">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- ─── 申请罚没弹窗 ─── -->
    <el-dialog v-model="forfeitVisible" title="申请保证金罚没" width="460px" destroy-on-close>
      <el-alert class="mb-3" :title="`当前可用余额：¥ ${formatAmount(account?.balance)}`" type="warning" show-icon :closable="false" />
      <el-form ref="forfeitFormRef" :model="forfeitForm" :rules="forfeitRules" label-width="100px">
        <el-form-item label="罚没金额" prop="amount">
          <el-input-number v-model="forfeitForm.amount" :min="0.01" :max="account?.balance ?? 999999999" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="罚没原因" prop="reason">
          <el-input v-model="forfeitForm.reason" type="textarea" :rows="3" placeholder="请填写违约事项说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="forfeitVisible = false">取消</el-button>
        <el-button type="danger" :loading="forfeitLoading" @click="submitForfeit">确认罚没</el-button>
      </template>
    </el-dialog>

    <!-- ─── 审批回调弹窗（开发用） ─── -->
    <el-dialog v-model="approveVisible" title="模拟OA审批回调" width="380px" destroy-on-close>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="流水ID">{{ approveRow?.id }}</el-descriptions-item>
        <el-descriptions-item label="审批ID">{{ approveRow?.approvalId || '(未提交)' }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ formatAmount(approveRow?.amount) }} 元</el-descriptions-item>
        <el-descriptions-item label="类型">{{ transTypeLabel(approveRow?.transType) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="danger" :loading="approveLoading" @click="doCallback(false)">驳回</el-button>
        <el-button type="success" :loading="approveLoading" @click="doCallback(true)">通过</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Connection, Money, Warning } from '@element-plus/icons-vue'
import {
  getDepositAccount,
  getDepositTransactions,
  payInDeposit,
  applyOffset,
  applyRefund,
  applyForfeit,
  depositApprovalCallback,
  type DepositAccountVO,
  type DepositTransaction,
} from '@/api/fin/deposit'

const searchForm = reactive({ contractId: undefined as number | undefined })
const account = ref<DepositAccountVO | null>(null)
const searched = ref(false)

async function loadAccount() {
  if (!searchForm.contractId) {
    // 无合同ID：清除账户卡片，展示全部流水
    account.value = null
    searched.value = false
    txQuery.contractId = undefined
    txQuery.pageNum = 1
    loadTransactions()
    return
  }
  searched.value = false
  try {
    const res: any = await getDepositAccount(searchForm.contractId)
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

const txLoading = ref(false)
const transactions = ref<DepositTransaction[]>([])
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
    const res: any = await getDepositTransactions(txQuery)
    transactions.value = res.records ?? []
    txTotal.value = res.total ?? 0
  } finally { txLoading.value = false }
}

function formatAmount(v?: number | null) {
  if (v == null) return '0.00'
  return Number(v).toFixed(2)
}

function transTypeLabel(t?: number) {
  const m: Record<number, string> = { 1: '缴纳', 2: '冲抵', 3: '退款', 4: '罚没' }
  return t ? m[t] ?? '-' : '-'
}

type TagType = 'success' | 'primary' | 'warning' | 'info' | 'danger' | undefined
function transTypeTag(t?: number): TagType {
  const m: Record<number, TagType> = { 1: 'success', 2: 'primary', 3: 'warning', 4: 'danger' }
  return t ? m[t] : undefined
}

function statusLabel(s?: number) {
  const m: Record<number, string> = { 0: '待审核', 1: '已生效', 2: '已驳回' }
  return s !== undefined ? m[s] ?? '-' : '-'
}

function statusTag(s?: number): TagType {
  const m: Record<number, TagType> = { 0: 'warning', 1: 'success', 2: 'danger' }
  return s !== undefined ? m[s] : undefined
}

// 缴纳
const payInVisible = ref(false)
const payInLoading = ref(false)
const payInFormRef = ref()
const payInForm = reactive({ contractId: 0, amount: 0, sourceCode: '', reason: '' })
const payInRules = {
  contractId: [{ required: true, message: '合同ID不能为空', trigger: 'blur' }],
  amount: [{ required: true, message: '金额不能为空', trigger: 'blur' }],
}

function openPayInDialog() {
  payInForm.contractId = searchForm.contractId ?? 0
  payInForm.amount = 0; payInForm.sourceCode = ''; payInForm.reason = ''
  payInVisible.value = true
}

async function submitPayIn() {
  await payInFormRef.value?.validate()
  payInLoading.value = true
  try { await payInDeposit({ ...payInForm }); ElMessage.success('缴纳成功'); payInVisible.value = false; loadAccount() }
  finally { payInLoading.value = false }
}

// 冲抵
const offsetVisible = ref(false)
const offsetLoading = ref(false)
const offsetFormRef = ref()
const offsetForm = reactive({ contractId: 0, receivableId: 0, amount: 0, reason: '' })
const offsetRules = {
  receivableId: [{ required: true, message: '应收ID不能为空', trigger: 'blur' }],
  amount: [{ required: true, message: '金额不能为空', trigger: 'blur' }],
}

function openOffsetDialog() {
  offsetForm.contractId = account.value?.contractId ?? 0
  offsetForm.receivableId = 0; offsetForm.amount = 0; offsetForm.reason = ''
  offsetVisible.value = true
}

async function submitOffset() {
  await offsetFormRef.value?.validate()
  offsetLoading.value = true
  try { await applyOffset({ ...offsetForm }); ElMessage.success('冲抵申请已提交，等待审批'); offsetVisible.value = false; loadTransactions() }
  finally { offsetLoading.value = false }
}

// 退款
const refundVisible = ref(false)
const refundLoading = ref(false)
const refundFormRef = ref()
const refundForm = reactive({ contractId: 0, amount: 0, reason: '', bankName: '', bankAccount: '', payee: '' })
const refundRules = {
  amount: [{ required: true, message: '金额不能为空', trigger: 'blur' }],
  bankAccount: [{ required: true, message: '银行账号不能为空', trigger: 'blur' }],
  payee: [{ required: true, message: '收款人不能为空', trigger: 'blur' }],
}

function openRefundDialog() {
  refundForm.contractId = account.value?.contractId ?? 0
  refundForm.amount = 0; refundForm.reason = refundForm.bankName = refundForm.bankAccount = refundForm.payee = ''
  refundVisible.value = true
}

async function submitRefund() {
  await refundFormRef.value?.validate()
  refundLoading.value = true
  try { await applyRefund({ ...refundForm }); ElMessage.success('退款申请已提交，等待审批'); refundVisible.value = false; loadTransactions() }
  finally { refundLoading.value = false }
}

// 罚没
const forfeitVisible = ref(false)
const forfeitLoading = ref(false)
const forfeitFormRef = ref()
const forfeitForm = reactive({ contractId: 0, amount: 0, reason: '' })
const forfeitRules = {
  amount: [{ required: true, message: '金额不能为空', trigger: 'blur' }],
  reason: [{ required: true, message: '罚没原因不能为空', trigger: 'blur' }],
}

function openForfeitDialog() {
  forfeitForm.contractId = account.value?.contractId ?? 0
  forfeitForm.amount = 0; forfeitForm.reason = ''
  forfeitVisible.value = true
}

async function submitForfeit() {
  await forfeitFormRef.value?.validate()
  await ElMessageBox.confirm('确认罚没该合同保证金？此操作需OA审批后生效', '确认罚没', {
    type: 'warning', confirmButtonText: '确认提交', cancelButtonText: '取消',
  })
  forfeitLoading.value = true
  try { await applyForfeit({ ...forfeitForm }); ElMessage.success('罚没申请已提交，等待审批'); forfeitVisible.value = false; loadTransactions() }
  finally { forfeitLoading.value = false }
}

onMounted(() => loadTransactions())

// 审批回调
const approveVisible = ref(false)
const approveLoading = ref(false)
const approveRow = ref<DepositTransaction | null>(null)

function openApproveDialog(row: DepositTransaction) {
  approveRow.value = row; approveVisible.value = true
}

async function doCallback(approved: boolean) {
  if (!approveRow.value?.approvalId) { ElMessage.warning('该流水尚无审批ID，无法回调'); return }
  approveLoading.value = true
  try {
    await depositApprovalCallback(approveRow.value.approvalId, approved)
    ElMessage.success(approved ? '审批通过' : '已驳回')
    approveVisible.value = false; loadAccount()
  } finally { approveLoading.value = false }
}
</script>

<style scoped lang="scss">
.fin-deposit-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ── 余额统计卡片 ── */
.balance-stat-card {
  padding: 16px 18px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid rgba(0,0,0,0.06);
  transition: box-shadow 0.2s;
  height: 100%;
  box-sizing: border-box;
  &:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.08); }

  .bsc-label { font-size: 12px; color: #94a3b8; margin-bottom: 6px; }
  .bsc-amount { font-size: 20px; font-weight: 700; }
}
.main-balance {
  border-top: 3px solid #3b82f6;
  .bsc-amount { color: #3b82f6; }
}
.income  { border-top: 3px solid #22c55e; .bsc-amount { color: #22c55e; } }
.offset  { border-top: 3px solid #3b82f6; .bsc-amount { color: #3b82f6; } }
.refund  { border-top: 3px solid #f59e0b; .bsc-amount { color: #d97706; } }
.forfeit { border-top: 3px solid #ef4444; .bsc-amount { color: #ef4444; } }

.info-block {
  border-top: 3px solid #e2e8f0;
  font-size: 12px;
  .info-row {
    display: flex; justify-content: space-between; align-items: center;
    margin-bottom: 6px;
    &:last-child { margin-bottom: 0; }
  }
  .info-label { color: #94a3b8; }
  .info-value {
    color: #1e293b; font-weight: 500;
    overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 110px;
  }
}

/* ── 操作按钮卡片 ── */
.action-card {
  border-radius: 12px !important;
  border: 1px solid rgba(0,0,0,0.06) !important;
  :deep(.el-card__body) { padding: 14px 20px; }

  .action-row { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
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
.filter-card {
  border-radius: 12px !important;
  border: 1px solid rgba(0,0,0,0.06) !important;
  :deep(.el-card__body) { padding: 14px 20px; }
  :deep(.el-form-item) { margin-bottom: 0; }
}

.empty-card {
  border-radius: 12px !important;
  border: 1px solid rgba(0,0,0,0.06) !important;
}

.amount-in  { color: #22c55e; font-weight: 600; }
.amount-out { color: #f56c6c; font-weight: 600; }
.mb-3 { margin-bottom: 12px; }
</style>
