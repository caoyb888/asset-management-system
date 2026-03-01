<template>
  <div class="ledger-detail-page">
    <!-- 页面头部 -->
    <div class="page-header-bar">
      <div class="bar-left">
        <el-button :icon="ArrowLeft" text @click="router.back()">返回</el-button>
        <span class="bar-divider" />
        <span class="bar-title">台账详情 · {{ detail?.ledgerCode }}</span>
      </div>
      <div class="bar-right">
        <el-button
          v-if="detail?.doubleSignStatus === 0"
          type="success"
          @click="handleDoubleSign"
        >双签确认</el-button>
        <el-button
          v-if="detail?.receivableStatus === 0 && detail?.doubleSignStatus === 1"
          type="warning"
          @click="handleGenerateReceivable"
        >生成应收计划</el-button>
        <el-button
          v-if="detail?.auditStatus === 0 && detail?.receivableStatus === 1"
          type="primary"
          @click="handleAudit(1)"
        >审核通过</el-button>
        <el-button
          v-if="detail?.auditStatus === 0 && detail?.receivableStatus === 1"
          type="danger"
          plain
          @click="handleAudit(2)"
        >驳回</el-button>
        <el-button
          v-if="detail?.auditStatus === 1 && detail?.receivableStatus >= 1"
          type="primary"
          plain
          @click="handlePushReceivable"
        >推送财务</el-button>
        <el-button
          v-if="detail?.receivableStatus >= 1"
          @click="openOneTimePayment"
        >录入首款</el-button>
      </div>
    </div>

    <!-- Tab 内容 -->
    <el-tabs v-model="activeTab" type="border-card" v-loading="loading" class="detail-tabs">
      <!-- Tab1: 合同信息 -->
      <el-tab-pane label="合同信息" name="info">
        <el-descriptions :column="3" border label-width="120px" size="small">
          <el-descriptions-item label="台账编号">{{ detail?.ledgerCode }}</el-descriptions-item>
          <el-descriptions-item label="合同编号">{{ detail?.contractCode }}</el-descriptions-item>
          <el-descriptions-item label="合同名称">{{ detail?.contractName }}</el-descriptions-item>
          <el-descriptions-item label="所属项目">{{ detail?.projectName }}</el-descriptions-item>
          <el-descriptions-item label="商家名称">{{ detail?.merchantName }}</el-descriptions-item>
          <el-descriptions-item label="品牌名称">{{ detail?.brandName }}</el-descriptions-item>
          <el-descriptions-item label="商铺编号">{{ detail?.shopCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="合同类型">
            <el-tag :type="contractTypeTagType(detail?.contractType)" size="small">
              {{ detail?.contractTypeName }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="台账状态">
            <el-tag :type="ledgerStatusType(detail?.status)" size="small">
              {{ detail?.statusName }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="合同开始">{{ detail?.contractStart }}</el-descriptions-item>
          <el-descriptions-item label="合同到期">
            <span :class="expiryClass(detail?.contractEnd)">{{ detail?.contractEnd }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDt(detail?.createdAt) }}</el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>

      <!-- Tab2: 应收计划 -->
      <el-tab-pane label="应收计划" name="receivables" lazy>
        <div class="tab-toolbar">
          <span class="tab-stat">
            共 {{ detail?.receivablePlans?.length ?? 0 }} 条 ·
            待收: {{ pendingCount }} 条 ·
            应收合计: <strong>{{ totalAmount }}</strong> 元
          </span>
          <el-space>
            <el-tag :type="receivableStatusType(detail?.receivableStatus)" size="small">
              {{ detail?.receivableStatusName }}
            </el-tag>
            <el-radio-group v-model="viewMode" size="small">
              <el-radio-button value="table">表格</el-radio-button>
              <el-radio-button value="calendar">日历</el-radio-button>
            </el-radio-group>
          </el-space>
        </div>
        <!-- 表格视图 -->
        <el-table v-if="viewMode === 'table'" :data="detail?.receivablePlans" border stripe size="small" max-height="500">
          <el-table-column type="index" label="序号" width="50" align="center" />
          <el-table-column prop="feeName" label="费项名称" width="120" />
          <el-table-column prop="billingStart" label="账期开始" width="110" align="center" />
          <el-table-column prop="billingEnd" label="账期结束" width="110" align="center" />
          <el-table-column prop="dueDate" label="应收日期" width="110" align="center">
            <template #default="{ row }">
              <span :class="dueDateClass(row.dueDate)">{{ row.dueDate }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="amount" label="应收金额" width="120" align="right">
            <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
          </el-table-column>
          <el-table-column prop="receivedAmount" label="已收金额" width="120" align="right">
            <template #default="{ row }">{{ formatMoney(row.receivedAmount) }}</template>
          </el-table-column>
          <el-table-column label="收款状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="receivablePlanStatusType(row.status)" size="small">
                {{ receivablePlanStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="推送状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.pushStatus === 1 ? 'success' : 'info'" size="small">
                {{ row.pushStatus === 1 ? '已推送' : '未推送' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="来源" width="100" align="center">
            <template #default="{ row }">{{ sourceTypeLabel(row.sourceType) }}</template>
          </el-table-column>
        </el-table>
        <!-- 日历视图 -->
        <ReceivableCalendar
          v-else
          :receivables="detail?.receivablePlans ?? []"
          :year="calendarYear"
          :month="calendarMonth"
        />
      </el-tab-pane>

      <!-- Tab3: 双签管理 -->
      <el-tab-pane label="双签管理" name="doublesign">
        <el-descriptions :column="2" border label-width="140px" size="small">
          <el-descriptions-item label="双签状态">
            <el-tag :type="detail?.doubleSignStatus === 1 ? 'success' : 'info'">
              {{ detail?.doubleSignStatusName }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="双签完成时间">
            {{ formatDt(detail?.doubleSignDate) || '-' }}
          </el-descriptions-item>
        </el-descriptions>
        <div v-if="detail?.doubleSignStatus === 0" style="margin-top:24px">
          <el-empty description="尚未完成双签，请确认甲乙双方均已签署合同">
            <el-button type="primary" @click="handleDoubleSign">立即双签确认</el-button>
          </el-empty>
        </div>
        <el-result
          v-else
          icon="success"
          title="双签已完成"
          :sub-title="`完成时间：${formatDt(detail?.doubleSignDate)}`"
          style="margin-top:16px"
        />
      </el-tab-pane>

      <!-- Tab4: 变更历史 -->
      <el-tab-pane label="变更历史" name="changes" lazy>
        <div style="margin-bottom:12px;display:flex;justify-content:flex-end">
          <el-button type="primary" size="small" :icon="Plus"
            @click="$router.push('/opr/contract-changes/form')">发起变更</el-button>
        </div>
        <div v-if="changeHistory.length === 0" style="padding:24px 0">
          <el-empty description="暂无变更记录" />
        </div>
        <el-timeline v-else>
          <el-timeline-item
            v-for="c in changeHistory"
            :key="c.id"
            :timestamp="formatDt(c.createdAt)"
            placement="top"
            :type="changeTimelineType(c.status)"
          >
            <el-card shadow="never" class="change-card">
              <div class="change-card-header">
                <span class="change-code">{{ c.changeCode }}</span>
                <el-tag :type="changeStatusTagType(c.status)" size="small">{{ c.statusName }}</el-tag>
              </div>
              <el-space wrap style="margin:8px 0">
                <el-tag
                  v-for="(code, i) in (c.changeTypeCodes || [])"
                  :key="code"
                  :type="changeTypeTagType(code)"
                  size="small"
                >{{ c.changeTypeNames?.[i] || code }}</el-tag>
              </el-space>
              <div v-if="c.reason" class="change-reason">原因：{{ c.reason }}</div>
              <div style="margin-top:8px">
                <el-button type="primary" link size="small"
                  @click="$router.push(`/opr/contract-changes/${c.id}`)">查看详情</el-button>
              </div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </el-tab-pane>

      <!-- Tab5: 审核记录 -->
      <el-tab-pane label="审核记录" name="audit">
        <el-descriptions :column="2" border label-width="140px" size="small">
          <el-descriptions-item label="审核状态">
            <el-tag :type="auditStatusType(detail?.auditStatus)" size="small">
              {{ detail?.auditStatusName }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="推送财务时间">
            {{ formatDt(detail?.pushTime) || '-' }}
          </el-descriptions-item>
        </el-descriptions>
        <el-empty v-if="detail?.auditStatus === 0" description="待审核" style="margin-top:24px" />
        <el-result
          v-else-if="detail?.auditStatus === 1"
          icon="success"
          title="审核已通过"
          style="margin-top:16px"
        />
        <el-result
          v-else-if="detail?.auditStatus === 2"
          icon="error"
          title="审核已驳回"
          style="margin-top:16px"
        />
      </el-tab-pane>
    </el-tabs>

    <!-- 一次性首款录入弹窗 -->
    <el-dialog
      v-model="showOneTimePaymentDialog"
      title="录入一次性首款"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-tabs v-model="paymentTab" type="card">
        <el-tab-pane label="单笔" name="single" />
        <el-tab-pane label="多笔" name="multi" />
        <el-tab-pane label="历史账期" name="history" />
      </el-tabs>
      <el-form
        ref="paymentFormRef"
        :model="paymentForm"
        :rules="paymentRules"
        label-width="100px"
        style="margin-top:16px"
      >
        <el-form-item label="收款项目" prop="feeItemId">
          <el-select
            v-model="paymentForm.feeItemId"
            placeholder="请选择收款项目"
            style="width:100%"
          >
            <el-option
              v-for="item in feeItems"
              :key="item.id"
              :label="item.itemName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number
            v-model="paymentForm.amount"
            :precision="2"
            :min="0.01"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="账期开始">
          <el-date-picker
            v-model="paymentForm.billingStart"
            type="date"
            value-format="YYYY-MM-DD"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="账期结束">
          <el-date-picker
            v-model="paymentForm.billingEnd"
            type="date"
            value-format="YYYY-MM-DD"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="paymentForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showOneTimePaymentDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitOneTimePayment">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, ArrowLeft } from '@element-plus/icons-vue'
import {
  getLedgerById,
  doubleSign,
  generateReceivable,
  auditLedger,
  pushReceivable,
  addOneTimePayment,
} from '@/api/opr/ledger'
import { getChangeHistory, type ChangeDetailVO } from '@/api/opr/change'
import request from '@/api/request'
import ReceivableCalendar from '@/components/opr/ReceivableCalendar.vue'

const router = useRouter()
const route = useRoute()
const id = Number(route.params.id)

const loading = ref(false)
const detail = ref<Record<string, any> | null>(null)
const activeTab = ref('info')
const changeHistory = ref<ChangeDetailVO[]>([])
const showOneTimePaymentDialog = ref(false)
const submitting = ref(false)
const paymentTab = ref('single')
const feeItems = ref<{ id: number; itemName: string }[]>([])

// 应收计划视图模式：table / calendar
const viewMode = ref<'table' | 'calendar'>('table')
const calendarYear = computed(() => new Date().getFullYear())
const calendarMonth = computed(() => new Date().getMonth() + 1)

const paymentFormRef = ref()
const paymentForm = reactive({
  feeItemId: undefined as number | undefined,
  amount: undefined as number | undefined,
  billingStart: '',
  billingEnd: '',
  entryType: 1,
  remark: '',
})
const paymentRules = {
  feeItemId: [{ required: true, message: '请选择收款项目', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
}

async function loadDetail() {
  loading.value = true
  try {
    const res = await getLedgerById(id)
    detail.value = res.data
  } finally {
    loading.value = false
  }
}

async function loadFeeItems() {
  try {
    const res = await request.get('/api/inv/config/fee-items', { params: { pageSize: 100 } })
    feeItems.value = res.data?.records || []
  } catch {
    feeItems.value = []
  }
}

const pendingCount = computed(() =>
  detail.value?.receivablePlans?.filter((p: any) => p.status === 0).length ?? 0
)

const totalAmount = computed(() => {
  const sum = detail.value?.receivablePlans?.reduce(
    (acc: number, p: any) => acc + Number(p.amount || 0), 0
  ) ?? 0
  return sum.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
})

function formatMoney(v?: number | string): string {
  if (v == null) return '-'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatDt(v?: string): string {
  if (!v) return ''
  return v.replace('T', ' ').substring(0, 19)
}

function daysUntil(d?: string): number {
  if (!d) return Infinity
  return Math.ceil((new Date(d).getTime() - Date.now()) / 86400000)
}

function expiryClass(contractEnd?: string): string {
  const d = daysUntil(contractEnd)
  if (d <= 7) return 'expiry-red'
  if (d <= 15) return 'expiry-orange'
  return ''
}

function dueDateClass(dueDate?: string): string {
  const d = daysUntil(dueDate)
  if (d < 0) return 'expiry-red'
  if (d <= 7) return 'expiry-orange'
  return ''
}

type TagType = 'success' | 'primary' | 'warning' | 'info' | 'danger' | undefined

function contractTypeTagType(type?: number): TagType {
  const m: Record<number, TagType> = { 1: 'primary', 2: 'success', 3: 'info' }
  return type != null ? m[type] : undefined
}

function ledgerStatusType(s?: number): TagType {
  const m: Record<number, TagType> = { 0: 'primary', 1: 'success', 2: 'danger' }
  return s != null ? m[s] : undefined
}

function receivableStatusType(s?: number): TagType {
  const m: Record<number, TagType> = { 0: 'info', 1: 'warning', 2: 'success' }
  return s != null ? m[s] : undefined
}

function auditStatusType(s?: number): TagType {
  const m: Record<number, TagType> = { 0: 'info', 1: 'success', 2: 'danger' }
  return s != null ? m[s] : undefined
}

function receivablePlanStatusLabel(s?: number): string {
  const m: Record<number, string> = { 0: '待收', 1: '部分收款', 2: '已收清', 3: '已作废' }
  return s != null ? (m[s] || String(s)) : '-'
}

function receivablePlanStatusType(s?: number): TagType {
  const m: Record<number, TagType> = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
  return s != null ? m[s] : undefined
}

function sourceTypeLabel(s?: number): string {
  const m: Record<number, string> = { 1: '合同生成', 2: '变更生成', 3: '浮动租金', 4: '一次性录入' }
  return s != null ? (m[s] || String(s)) : '-'
}

async function handleDoubleSign() {
  await ElMessageBox.confirm('确认完成双签？', '双签确认', { type: 'warning' })
  await doubleSign(id)
  ElMessage.success('双签确认成功')
  loadDetail()
}

async function handleGenerateReceivable() {
  await ElMessageBox.confirm('确认生成应收计划？此操作不可撤销。', '生成应收计划', { type: 'warning' })
  const res = await generateReceivable(id)
  ElMessage.success(`应收计划生成成功，共 ${res.data} 条`)
  loadDetail()
}

async function handleAudit(status: 1 | 2) {
  const label = status === 1 ? '通过' : '驳回'
  await ElMessageBox.confirm(`确认审核${label}？`, '审核确认', { type: 'warning' })
  await auditLedger(id, { auditStatus: status })
  ElMessage.success(`审核${label}成功`)
  loadDetail()
}

async function handlePushReceivable() {
  await ElMessageBox.confirm('确认推送应收计划至财务系统？', '推送确认', { type: 'warning' })
  await pushReceivable(id)
  ElMessage.success('推送成功')
  loadDetail()
}

function openOneTimePayment() {
  paymentTab.value = 'single'
  paymentForm.entryType = 1
  showOneTimePaymentDialog.value = true
}

async function submitOneTimePayment() {
  await paymentFormRef.value.validate()
  paymentForm.entryType = paymentTab.value === 'single' ? 1 : paymentTab.value === 'multi' ? 2 : 3
  submitting.value = true
  try {
    await addOneTimePayment(id, paymentForm)
    ElMessage.success('首款录入成功')
    showOneTimePaymentDialog.value = false
    loadDetail()
  } finally {
    submitting.value = false
  }
}

// 切换到变更历史 Tab 时懒加载
watch(activeTab, (tab) => {
  if (tab === 'changes' && changeHistory.value.length === 0 && detail.value?.contractId) {
    loadChangeHistory(detail.value.contractId)
  }
})

async function loadChangeHistory(contractId: number) {
  try {
    const res = await getChangeHistory(contractId)
    changeHistory.value = res.data || []
  } catch {
    changeHistory.value = []
  }
}

function changeTimelineType(status?: number): TagType {
  const m: Record<number, TagType> = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
  return status != null ? m[status] : 'info'
}
function changeStatusTagType(s?: number): TagType {
  const m: Record<number, TagType> = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
  return s != null ? m[s] : undefined
}
const CHANGE_TYPE_TAG: Record<string, TagType> = {
  RENT: 'danger', FEE: 'warning', TERM: 'primary', AREA: 'success',
  BRAND: 'info', TENANT: 'info', COMPANY: 'info', CLAUSE: 'info',
}
function changeTypeTagType(code: string): TagType { return CHANGE_TYPE_TAG[code] }

onMounted(() => {
  loadDetail()
  loadFeeItems()
})
</script>

<style scoped lang="scss">
.ledger-detail-page { display: flex; flex-direction: column; gap: 16px; }

.page-header-bar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 20px; background: #fff;
  border-radius: 12px; border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  .bar-left { display: flex; align-items: center; gap: 12px; }
  .bar-divider { width: 1px; height: 16px; background: #e2e8f0; }
  .bar-title { font-size: 16px; font-weight: 600; color: #1e293b; }
  .bar-right { display: flex; gap: 8px; align-items: center; }
}

.detail-tabs {
  border-radius: 12px !important;
  border: 1px solid rgba(0, 0, 0, 0.06) !important;
  box-shadow: none !important;
  :deep(.el-tabs__header) { background: #f8fafc; }
  :deep(.el-tabs__item.is-active) { color: #3b82f6; }
  :deep(.el-tabs__content) { padding: 20px; }
}

.tab-toolbar {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 12px;
  .tab-stat { color: #64748b; font-size: 13px; }
}

.expiry-red { color: #f56c6c; font-weight: 600; }
.expiry-orange { color: #e6a23c; font-weight: 600; }
.change-card { border: 1px solid #ebeef5; }
.change-card-header { display: flex; justify-content: space-between; align-items: center; }
.change-code { font-weight: 600; font-size: 14px; }
.change-reason { font-size: 12px; color: #606266; }
</style>
