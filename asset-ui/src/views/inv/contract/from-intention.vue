<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-button :icon="ArrowLeft" text @click="router.back()">返回</el-button>
          <span>意向转合同</span>
          <div />
        </div>
      </template>

      <!-- 意向协议预览卡片 -->
      <div v-if="intention" class="preview-card">
        <el-descriptions title="意向协议信息" border :column="3" size="small">
          <el-descriptions-item label="意向编号">{{ intention.intentionCode }}</el-descriptions-item>
          <el-descriptions-item label="所属项目">{{ intention.projectName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="商家">{{ intention.merchantName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="品牌">{{ intention.brandName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="合同开始">{{ intention.contractStart || '-' }}</el-descriptions-item>
          <el-descriptions-item label="合同结束">{{ intention.contractEnd || '-' }}</el-descriptions-item>
          <el-descriptions-item label="付款周期">{{ paymentCycleLabel(intention.paymentCycle) }}</el-descriptions-item>
          <el-descriptions-item label="结算方式">{{ billingModeLabel(intention.billingMode) }}</el-descriptions-item>
          <el-descriptions-item label="总金额">
            <span class="amount-text">¥ {{ fmtAmount(intention.totalAmount) }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <el-skeleton v-else-if="loading" :rows="4" animated class="preview-skeleton" />

      <el-alert
        title="意向数据（日期/费项/商铺）将自动复制到新合同，请补录合同类型和名称后提交"
        type="info"
        :closable="false"
        class="mb-4"
      />

      <!-- 合同专属信息 -->
      <el-form ref="formRef" :model="form" label-width="120px" class="form-body">
        <el-form-item
          label="合同类型"
          prop="contractType"
          :rules="[{ required: true, message: '请选择合同类型', trigger: 'change' }]"
        >
          <el-select v-model="form.contractType" placeholder="请选择" style="width: 240px">
            <el-option label="标准租赁合同" :value="1" />
            <el-option label="临时租赁合同" :value="2" />
            <el-option label="补充协议" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item
          label="合同名称"
          prop="contractName"
          :rules="[{ required: true, message: '请输入合同名称', trigger: 'blur' }]"
        >
          <el-input
            v-model="form.contractName"
            placeholder="请输入合同名称"
            style="width: 400px"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <div class="form-actions">
        <el-button
          type="primary"
          size="large"
          :loading="submitting"
          :disabled="!intention"
          @click="handleSubmit"
        >
          确认转合同
        </el-button>
        <el-button size="large" @click="router.back()">取消</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'

import { getIntentionDetail, type IntentionVO } from '@/api/inv/intention'
import { convertFromIntention } from '@/api/inv/contract'

const route = useRoute()
const router = useRouter()
const intentionId = ref<number | null>(
  route.query.intentionId ? Number(route.query.intentionId) : null,
)

const formRef = ref<FormInstance>()
const loading = ref(false)
const submitting = ref(false)
const intention = ref<IntentionVO | null>(null)

const form = ref({
  contractType: undefined as number | undefined,
  contractName: '',
})

// ─── 工具函数 ────────────────────────────────────────────────────────────────
function paymentCycleLabel(cycle?: number) {
  const map: Record<number, string> = { 1: '月付', 2: '两月付', 3: '季付', 4: '四月付', 5: '半年付', 6: '年付' }
  return cycle != null ? (map[cycle] ?? '-') : '-'
}

function billingModeLabel(mode?: number) {
  const map: Record<number, string> = { 1: '预付', 2: '当期', 3: '后付' }
  return mode != null ? (map[mode] ?? '-') : '-'
}

function fmtAmount(n: number | null | undefined) {
  if (n == null) return '-'
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ─── 加载意向详情 ─────────────────────────────────────────────────────────────
async function loadIntention(id: number) {
  loading.value = true
  try {
    intention.value = await getIntentionDetail(id)
    // 自动填充合同名称（可手动修改）
    if (intention.value?.intentionCode) {
      form.value.contractName = `${intention.value.intentionCode}-租赁合同`
    }
  } catch {
    ElMessage.error('加载意向协议失败，请返回重试')
  } finally {
    loading.value = false
  }
}

// ─── 提交转合同 ───────────────────────────────────────────────────────────────
async function handleSubmit() {
  if (!intentionId.value) { ElMessage.error('缺少意向ID'); return }
  const valid = await formRef.value?.validate().then(() => true).catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const newContractId = await convertFromIntention(intentionId.value, {
      contractName: form.value.contractName,
      contractType: form.value.contractType!,
    })
    ElMessage.success('转合同成功！即将跳转到合同详情')
    setTimeout(() => {
      router.push({ path: '/inv/contracts/form', query: { id: newContractId } })
    }, 800)
  } catch (err: unknown) {
    ElMessage.error((err as { message?: string })?.message || '转合同失败，请重试')
  } finally {
    submitting.value = false
  }
}

// ─── 初始化 ───────────────────────────────────────────────────────────────────
onMounted(() => {
  if (intentionId.value) {
    loadIntention(intentionId.value)
  } else {
    ElMessage.warning('未获取到意向ID，请从意向列表操作')
  }
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.preview-card {
  margin-bottom: 20px;
}

.preview-skeleton {
  margin-bottom: 20px;
}

.mb-4 {
  margin-bottom: 20px;
}

.form-body {
  max-width: 680px;
  margin-bottom: 8px;
}

.form-actions {
  margin-top: 24px;
  display: flex;
  gap: 12px;
  justify-content: center;
}

.amount-text {
  color: #e6a23c;
  font-weight: 600;
  font-size: 15px;
}
</style>
