<template>
  <div class="page-container">
    <el-card v-loading="loading" shadow="never">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <div style="display:flex;align-items:center;gap:12px">
            <el-button :icon="ArrowLeft" text @click="$router.back()">返回</el-button>
            <span>解约单详情</span>
            <el-tag v-if="detail" :type="statusTagColor(detail.status)" size="small">
              {{ detail.statusName || statusLabel(detail.status) }}
            </el-tag>
          </div>
          <div v-if="detail">
            <el-button v-if="detail.status === 0 || detail.status === 3" type="primary"
              @click="$router.push(`/opr/terminations/form?id=${detail.id}`)">编辑</el-button>
            <el-button v-if="detail.status === 0 && detail.settlementAmount == null"
              type="warning" @click="doCalcSettlement">计算清算</el-button>
            <el-button v-if="detail.status === 0 && detail.settlementAmount != null"
              type="success" @click="doSubmitApproval">提交审批</el-button>
            <el-button v-if="detail.status === 1" type="primary" plain @click="openCallback">
              审批回调
            </el-button>
          </div>
        </div>
      </template>

      <template v-if="detail">
        <!-- 基本信息 -->
        <div class="section-title">基本信息</div>
        <el-descriptions :column="3" border class="mb-24">
          <el-descriptions-item label="解约单号">{{ detail.terminationCode }}</el-descriptions-item>
          <el-descriptions-item label="解约类型">
            <el-tag :type="typeTagColor(detail.terminationType)" size="small">
              {{ detail.terminationTypeName || typeLabel(detail.terminationType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="解约日期">{{ detail.terminationDate }}</el-descriptions-item>
          <el-descriptions-item label="合同编号">{{ detail.contractCode }}</el-descriptions-item>
          <el-descriptions-item label="合同名称">{{ detail.contractName }}</el-descriptions-item>
          <el-descriptions-item label="商家名称">{{ detail.merchantName }}</el-descriptions-item>
          <el-descriptions-item label="项目名称">{{ detail.projectName }}</el-descriptions-item>
          <el-descriptions-item label="商铺编号">{{ detail.shopCode }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.newContractId" label="新合同ID">
            {{ detail.newContractId }}
          </el-descriptions-item>
          <el-descriptions-item label="解约原因" :span="3">
            {{ detail.reason || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 清算汇总 -->
        <div class="section-title">清算汇总</div>
        <el-descriptions :column="4" border class="mb-24">
          <el-descriptions-item label="未结算应收">
            <span class="money-text text-danger">
              ¥ {{ formatMoney(detail.unsettledAmount) }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="违约金">
            <span class="money-text text-warning">
              ¥ {{ formatMoney(detail.penaltyAmount) }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="退还保证金">
            <span class="money-text text-success">
              ¥ {{ formatMoney(detail.refundDeposit) }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="清算总额">
            <span v-if="detail.settlementAmount != null"
              :class="['money-total', detail.settlementAmount >= 0 ? 'text-danger' : 'text-success']">
              ¥ {{ formatMoney(detail.settlementAmount) }}
            </span>
            <span v-else class="text-gray">未计算</span>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 清算明细 -->
        <div class="section-title">清算明细</div>
        <el-table :data="detail.settlements" border class="mb-24">
          <el-table-column label="明细类型" prop="itemType" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="itemTypeColor(row.itemType)" size="small">
                {{ itemTypeLabel(row.itemType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="明细名称" prop="itemName" min-width="160" />
          <el-table-column label="金额(元)" prop="amount" width="150" align="right">
            <template #default="{ row }">
              <span :class="row.amount >= 0 ? 'text-danger' : 'text-success'" style="font-weight:600">
                {{ formatMoney(row.amount) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="备注" prop="remark" min-width="200" show-overflow-tooltip />
        </el-table>
        <el-empty v-if="!detail.settlements?.length" description="暂无清算明细（请先执行清算计算）" style="padding:20px 0" />

        <el-alert v-if="detail.settlements?.length" type="info" :closable="false">
          正数金额表示「应收（租方欠款）」，负数金额表示「应退（平台退款）」
        </el-alert>

        <!-- 审批信息 -->
        <div v-if="detail.approvalId" class="mt-24">
          <div class="section-title">审批信息</div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="审批流程ID">{{ detail.approvalId }}</el-descriptions-item>
            <el-descriptions-item label="当前状态">{{ detail.statusName }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </template>
    </el-card>

    <!-- 审批回调弹窗 -->
    <el-dialog v-model="callbackVisible" title="审批回调" width="420px">
      <el-form :model="callbackForm" label-width="80px">
        <el-form-item label="审批结果">
          <el-radio-group v-model="callbackForm.status">
            <el-radio :value="2">通过</el-radio>
            <el-radio :value="3">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="callbackForm.comment" type="textarea" :rows="3" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="callbackVisible = false">取消</el-button>
        <el-button type="primary" :loading="callbackLoading" @click="doCallback">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import {
  getTerminationById,
  calculateSettlement,
  submitTerminationApproval,
  terminationApprovalCallback,
  type TerminationDetailVO
} from '@/api/opr/termination'

const route = useRoute()
const router = useRouter()

const id = Number(route.params.id)
const loading = ref(false)
const detail = ref<TerminationDetailVO | null>(null)

const callbackVisible = ref(false)
const callbackLoading = ref(false)
const callbackForm = reactive({ status: 2, comment: '' })

async function loadDetail() {
  loading.value = true
  try {
    const res = await getTerminationById(id)
    detail.value = res.data
  } finally {
    loading.value = false
  }
}

async function doCalcSettlement() {
  try {
    await calculateSettlement(id)
    ElMessage.success('清算计算完成')
    loadDetail()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '计算失败')
  }
}

async function doSubmitApproval() {
  try {
    await submitTerminationApproval(id)
    ElMessage.success('已提交审批')
    loadDetail()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '提交失败')
  }
}

function openCallback() {
  callbackForm.status = 2
  callbackForm.comment = ''
  callbackVisible.value = true
}

async function doCallback() {
  callbackLoading.value = true
  try {
    await terminationApprovalCallback(id, {
      status: callbackForm.status,
      comment: callbackForm.comment
    })
    ElMessage.success(callbackForm.status === 2 ? '审批通过，解约已执行' : '审批已驳回')
    callbackVisible.value = false
    loadDetail()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '操作失败')
  } finally {
    callbackLoading.value = false
  }
}

// 工具函数
function typeLabel(t: number) {
  return { 1: '到期终止', 2: '提前解约', 3: '重签解约' }[t] ?? ''
}
function typeTagColor(t: number): 'success' | 'warning' | 'info' | undefined {
  const m: Record<number, 'success' | 'warning' | 'info'> = { 1: 'success', 2: 'warning', 3: 'info' }
  return m[t]
}
function statusLabel(s: number) {
  return { 0: '草稿', 1: '审批中', 2: '已生效', 3: '已驳回' }[s] ?? ''
}
function statusTagColor(s: number): 'success' | 'warning' | 'info' | 'danger' | undefined {
  const m: Record<number, 'success' | 'warning' | 'info' | 'danger'> = {
    0: 'info', 1: 'warning', 2: 'success', 3: 'danger'
  }
  return m[s]
}
function itemTypeLabel(t: number) {
  return { 1: '未收租费', 2: '违约金', 3: '保证金退还', 4: '其他' }[t] ?? ''
}
function itemTypeColor(t: number): 'success' | 'warning' | 'info' | 'danger' | undefined {
  const m: Record<number, 'success' | 'warning' | 'info' | 'danger'> = {
    1: 'danger', 2: 'warning', 3: 'success', 4: 'info'
  }
  return m[t]
}
function formatMoney(val: number | undefined | null) {
  if (val == null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

onMounted(loadDetail)
</script>

<style scoped>
.mb-24 { margin-bottom: 24px; }
.mt-24 { margin-top: 24px; }
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  padding-left: 8px;
  border-left: 3px solid #2e75b6;
}
.money-text { font-size: 15px; font-weight: 600; }
.money-total { font-size: 18px; font-weight: 700; }
.text-danger { color: #f56c6c; }
.text-success { color: #67c23a; }
.text-warning { color: #e6a23c; }
.text-gray { color: #909399; }
</style>
