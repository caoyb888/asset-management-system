<template>
  <div class="fin-deposit-page">
    <!-- 合同搜索区 -->
    <el-card class="search-card mb-4">
      <el-form :model="searchForm" inline>
        <el-form-item label="合同ID">
          <el-input
            v-model.number="searchForm.contractId"
            placeholder="输入合同ID"
            style="width: 180px"
            clearable
            @keyup.enter="loadAccount"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadAccount">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 余额卡片区 -->
    <template v-if="account">
      <el-row :gutter="16" class="mb-4">
        <el-col :span="4">
          <el-card shadow="hover" class="balance-card main-balance">
            <div class="balance-label">可用余额</div>
            <div class="balance-amount">{{ formatAmount(account.balance) }}</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" class="balance-card">
            <div class="balance-label">累计收入</div>
            <div class="balance-amount text-green">{{ formatAmount(account.totalIn) }}</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" class="balance-card">
            <div class="balance-label">累计冲抵</div>
            <div class="balance-amount text-blue">{{ formatAmount(account.totalOffset) }}</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" class="balance-card">
            <div class="balance-label">累计退款</div>
            <div class="balance-amount text-orange">{{ formatAmount(account.totalRefund) }}</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" class="balance-card">
            <div class="balance-label">累计罚没</div>
            <div class="balance-amount text-red">{{ formatAmount(account.totalForfeit) }}</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" class="balance-card info-card">
            <div class="info-row">
              <span class="info-label">合同</span>
              <span class="info-value">{{ account.contractCode || '-' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">商家</span>
              <span class="info-value">{{ account.merchantName || '-' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">项目</span>
              <span class="info-value">{{ account.projectName || '-' }}</span>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 操作按钮区 -->
      <el-card class="mb-4">
        <el-space>
          <el-button type="success" :icon="Plus" @click="openPayInDialog">缴纳保证金</el-button>
          <el-button type="primary" :icon="Connection" @click="openOffsetDialog">申请冲抵</el-button>
          <el-button type="warning" :icon="Money" @click="openRefundDialog">申请退款</el-button>
          <el-button type="danger" :icon="Warning" @click="openForfeitDialog">申请罚没</el-button>
        </el-space>
      </el-card>

      <!-- 流水明细 -->
      <el-card>
        <template #header>
          <div class="card-header-row">
            <span>保证金流水</span>
            <el-select
              v-model="txQuery.transType"
              placeholder="交易类型"
              clearable
              style="width: 130px"
              @change="loadTransactions"
            >
              <el-option label="缴纳" :value="1" />
              <el-option label="冲抵" :value="2" />
              <el-option label="退款" :value="3" />
              <el-option label="罚没" :value="4" />
            </el-select>
          </div>
        </template>

        <el-table :data="transactions" v-loading="txLoading" stripe>
          <el-table-column label="交易日期" prop="transDate" width="110" />
          <el-table-column label="交易类型" width="90">
            <template #default="{ row }">
              <el-tag :type="transTypeTag(row.transType)" size="small">
                {{ transTypeLabel(row.transType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="金额（元）" align="right" width="120">
            <template #default="{ row }">
              <span :class="row.transType === 1 ? 'text-green' : 'text-red'">
                {{ row.transType === 1 ? '+' : '-' }}{{ formatAmount(row.amount) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="余额快照（元）" prop="balanceAfter" align="right" width="130">
            <template #default="{ row }">{{ formatAmount(row.balanceAfter) }}</template>
          </el-table-column>
          <el-table-column label="单据号" prop="sourceCode" min-width="180" show-overflow-tooltip />
          <el-table-column label="原因" prop="reason" min-width="160" show-overflow-tooltip />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status)" size="small">
                {{ statusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 0"
                link
                type="primary"
                size="small"
                @click="openApproveDialog(row)"
              >审批回调</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrap">
          <el-pagination
            v-model:current-page="txQuery.pageNum"
            v-model:page-size="txQuery.pageSize"
            :total="txTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @change="loadTransactions"
          />
        </div>
      </el-card>
    </template>

    <!-- 未查询到账户 -->
    <el-card v-else-if="searched">
      <el-empty description="该合同暂无保证金账户，缴纳后自动创建" :image-size="100" />
    </el-card>

    <!-- 初始提示 -->
    <el-card v-else>
      <el-empty description="请输入合同ID查询保证金账户" :image-size="100" />
    </el-card>

    <!-- ─── 缴纳保证金弹窗 ─── -->
    <el-dialog v-model="payInVisible" title="缴纳保证金" width="440px" destroy-on-close>
      <el-form ref="payInFormRef" :model="payInForm" :rules="payInRules" label-width="90px">
        <el-form-item label="合同ID" prop="contractId">
          <el-input-number v-model="payInForm.contractId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="缴纳金额" prop="amount">
          <el-input-number
            v-model="payInForm.amount"
            :min="0.01"
            :precision="2"
            style="width: 100%"
          />
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
      <el-alert
        class="mb-3"
        :title="`当前可用余额：¥ ${formatAmount(account?.balance)}`"
        type="info"
        show-icon
        :closable="false"
      />
      <el-form ref="offsetFormRef" :model="offsetForm" :rules="offsetRules" label-width="100px">
        <el-form-item label="冲抵应收ID" prop="receivableId">
          <el-input-number v-model="offsetForm.receivableId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="冲抵金额" prop="amount">
          <el-input-number
            v-model="offsetForm.amount"
            :min="0.01"
            :max="account?.balance ?? 999999999"
            :precision="2"
            style="width: 100%"
          />
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
      <el-alert
        class="mb-3"
        :title="`当前可用余额：¥ ${formatAmount(account?.balance)}`"
        type="info"
        show-icon
        :closable="false"
      />
      <el-form ref="refundFormRef" :model="refundForm" :rules="refundRules" label-width="100px">
        <el-form-item label="退款金额" prop="amount">
          <el-input-number
            v-model="refundForm.amount"
            :min="0.01"
            :max="account?.balance ?? 999999999"
            :precision="2"
            style="width: 100%"
          />
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
      <el-alert
        class="mb-3"
        :title="`当前可用余额：¥ ${formatAmount(account?.balance)}`"
        type="warning"
        show-icon
        :closable="false"
      />
      <el-form ref="forfeitFormRef" :model="forfeitForm" :rules="forfeitRules" label-width="100px">
        <el-form-item label="罚没金额" prop="amount">
          <el-input-number
            v-model="forfeitForm.amount"
            :min="0.01"
            :max="account?.balance ?? 999999999"
            :precision="2"
            style="width: 100%"
          />
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
import { Search, Plus, Connection, Money, Warning } from '@element-plus/icons-vue'
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

// ─── 搜索 & 账户 ───────────────────────────────────────────────────────────
const searchForm = reactive({ contractId: undefined as number | undefined })
const account = ref<DepositAccountVO | null>(null)
const searched = ref(false)

async function loadAccount() {
  if (!searchForm.contractId) {
    ElMessage.warning('请输入合同ID')
    return
  }
  searched.value = false
  try {
    const res: any = await getDepositAccount(searchForm.contractId)
    account.value = res.data ?? null
    searched.value = true
    if (account.value) {
      txQuery.contractId = searchForm.contractId
      loadTransactions()
    }
  } catch {
    account.value = null
    searched.value = true
  }
}

function resetSearch() {
  searchForm.contractId = undefined
  account.value = null
  searched.value = false
  transactions.value = []
}

// ─── 流水列表 ──────────────────────────────────────────────────────────────
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
  if (!txQuery.contractId) return
  txLoading.value = true
  try {
    const res: any = await getDepositTransactions(txQuery)
    transactions.value = res.data?.records ?? []
    txTotal.value = res.data?.total ?? 0
  } finally {
    txLoading.value = false
  }
}

// ─── 格式化工具 ────────────────────────────────────────────────────────────
function formatAmount(v?: number | null) {
  if (v == null) return '0.00'
  return Number(v).toFixed(2)
}

function transTypeLabel(t?: number) {
  const m: Record<number, string> = { 1: '缴纳', 2: '冲抵', 3: '退款', 4: '罚没' }
  return t ? m[t] ?? '-' : '-'
}

function transTypeTag(t?: number) {
  const m: Record<number, string> = { 1: 'success', 2: 'primary', 3: 'warning', 4: 'danger' }
  return t ? m[t] ?? '' : ''
}

function statusLabel(s?: number) {
  const m: Record<number, string> = { 0: '待审核', 1: '已生效', 2: '已驳回' }
  return s !== undefined ? m[s] ?? '-' : '-'
}

function statusTag(s?: number) {
  const m: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'danger' }
  return s !== undefined ? m[s] ?? '' : ''
}

// ─── 缴纳保证金 ────────────────────────────────────────────────────────────
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
  payInForm.amount = 0
  payInForm.sourceCode = ''
  payInForm.reason = ''
  payInVisible.value = true
}

async function submitPayIn() {
  await payInFormRef.value?.validate()
  payInLoading.value = true
  try {
    await payInDeposit({ ...payInForm })
    ElMessage.success('缴纳成功')
    payInVisible.value = false
    loadAccount()
  } finally {
    payInLoading.value = false
  }
}

// ─── 申请冲抵 ──────────────────────────────────────────────────────────────
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
  offsetForm.receivableId = 0
  offsetForm.amount = 0
  offsetForm.reason = ''
  offsetVisible.value = true
}

async function submitOffset() {
  await offsetFormRef.value?.validate()
  offsetLoading.value = true
  try {
    await applyOffset({ ...offsetForm })
    ElMessage.success('冲抵申请已提交，等待审批')
    offsetVisible.value = false
    loadTransactions()
  } finally {
    offsetLoading.value = false
  }
}

// ─── 申请退款 ──────────────────────────────────────────────────────────────
const refundVisible = ref(false)
const refundLoading = ref(false)
const refundFormRef = ref()
const refundForm = reactive({
  contractId: 0, amount: 0, reason: '', bankName: '', bankAccount: '', payee: '',
})
const refundRules = {
  amount: [{ required: true, message: '金额不能为空', trigger: 'blur' }],
  bankAccount: [{ required: true, message: '银行账号不能为空', trigger: 'blur' }],
  payee: [{ required: true, message: '收款人不能为空', trigger: 'blur' }],
}

function openRefundDialog() {
  refundForm.contractId = account.value?.contractId ?? 0
  refundForm.amount = 0
  refundForm.reason = refundForm.bankName = refundForm.bankAccount = refundForm.payee = ''
  refundVisible.value = true
}

async function submitRefund() {
  await refundFormRef.value?.validate()
  refundLoading.value = true
  try {
    await applyRefund({ ...refundForm })
    ElMessage.success('退款申请已提交，等待审批')
    refundVisible.value = false
    loadTransactions()
  } finally {
    refundLoading.value = false
  }
}

// ─── 申请罚没 ──────────────────────────────────────────────────────────────
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
  forfeitForm.amount = 0
  forfeitForm.reason = ''
  forfeitVisible.value = true
}

async function submitForfeit() {
  await forfeitFormRef.value?.validate()
  await ElMessageBox.confirm('确认罚没该合同保证金？此操作需OA审批后生效', '确认罚没', {
    type: 'warning',
    confirmButtonText: '确认提交',
    cancelButtonText: '取消',
  })
  forfeitLoading.value = true
  try {
    await applyForfeit({ ...forfeitForm })
    ElMessage.success('罚没申请已提交，等待审批')
    forfeitVisible.value = false
    loadTransactions()
  } finally {
    forfeitLoading.value = false
  }
}

// ─── 审批回调（开发用） ────────────────────────────────────────────────────
const approveVisible = ref(false)
const approveLoading = ref(false)
const approveRow = ref<DepositTransaction | null>(null)

function openApproveDialog(row: DepositTransaction) {
  approveRow.value = row
  approveVisible.value = true
}

async function doCallback(approved: boolean) {
  if (!approveRow.value?.approvalId) {
    ElMessage.warning('该流水尚无审批ID，无法回调')
    return
  }
  approveLoading.value = true
  try {
    await depositApprovalCallback(approveRow.value.approvalId, approved)
    ElMessage.success(approved ? '审批通过' : '已驳回')
    approveVisible.value = false
    loadAccount()
  } finally {
    approveLoading.value = false
  }
}

onMounted(() => {
  // 如有需要可从路由query读contractId
})
</script>

<style scoped>
.fin-deposit-page {
  padding: 16px;
}
.mb-4 {
  margin-bottom: 16px;
}
.mb-3 {
  margin-bottom: 12px;
}
.balance-card {
  text-align: center;
  padding: 4px 0;
}
.main-balance {
  border-top: 3px solid #2e75b6;
}
.balance-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}
.balance-amount {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}
.text-green { color: #67c23a; }
.text-blue  { color: #409eff; }
.text-orange { color: #e6a23c; }
.text-red   { color: #f56c6c; }
.info-card {
  text-align: left;
  font-size: 12px;
}
.info-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}
.info-label { color: #909399; }
.info-value { color: #303133; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 120px; }
.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
