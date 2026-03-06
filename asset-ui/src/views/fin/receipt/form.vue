<template>
  <el-drawer
    v-model="visible"
    :title="drawerTitle"
    size="720px"
    destroy-on-close
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      :disabled="mode === 'view'"
      label-width="100px"
    >
      <!-- 基础信息 -->
      <el-divider content-position="left">基础信息</el-divider>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="合同ID" prop="contractId">
            <el-input-number
              v-model="form.contractId"
              :min="1"
              controls-position="right"
              style="width:100%"
              placeholder="请输入合同ID"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="收款日期" prop="receiptDate">
            <el-date-picker
              v-model="form.receiptDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择收款日期"
              style="width:100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="收款总金额" prop="totalAmount">
            <el-input-number
              v-model="form.totalAmount"
              :precision="2"
              :min="0.01"
              controls-position="right"
              style="width:100%"
              placeholder="0.00"
              @change="checkDetailSum"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="收款方式" prop="paymentMethod">
            <el-select v-model="form.paymentMethod" style="width:100%">
              <el-option label="银行转账" :value="1" />
              <el-option label="现金" :value="2" />
              <el-option label="支票" :value="3" />
              <el-option label="POS" :value="4" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="付款方名称">
            <el-input v-model="form.payerName" placeholder="请输入付款方名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="银行流水号">
            <el-input v-model="form.bankSerialNo" placeholder="银行转账时填写" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="收款银行">
            <el-input v-model="form.bankName" placeholder="收款银行" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="收款账号">
            <el-input v-model="form.bankAccount" placeholder="收款账号" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="收款人">
            <el-input v-model="form.receiver" placeholder="收款人" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="核算主体">
            <el-input v-model="form.accountingEntity" placeholder="核算主体" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="是否未名款">
            <el-switch v-model="form.isUnnamed" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 费项拆分明细 -->
      <el-divider content-position="left">
        费项拆分明细
        <span style="margin-left:8px;font-size:12px;color:#909399">（不填则默认全额一条）</span>
      </el-divider>

      <!-- 合计校验提示 -->
      <el-alert
        v-if="detailSumError"
        :title="detailSumError"
        type="error"
        show-icon
        :closable="false"
        style="margin-bottom:12px"
      />

      <div v-if="mode !== 'view'" style="margin-bottom:8px">
        <el-button size="small" @click="addDetail">+ 添加明细行</el-button>
      </div>

      <el-table :data="form.details" border size="small">
        <el-table-column label="费项名称" min-width="150">
          <template #default="{ row }">
            <el-input v-if="mode !== 'view'" v-model="row.feeName" placeholder="费项名称" size="small" />
            <span v-else>{{ row.feeName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="金额(元)" width="140">
          <template #default="{ row }">
            <el-input-number
              v-if="mode !== 'view'"
              v-model="row.amount"
              :precision="2"
              :min="0.01"
              controls-position="right"
              size="small"
              style="width:100%"
              @change="checkDetailSum"
            />
            <span v-else>{{ row.amount?.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="120">
          <template #default="{ row }">
            <el-input v-if="mode !== 'view'" v-model="row.remark" placeholder="备注" size="small" />
            <span v-else>{{ row.remark }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="mode !== 'view'" label="操作" width="70">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" @click="removeDetail($index)">删除</el-button>
          </template>
        </el-table-column>
        <!-- 合计行 -->
        <template #append>
          <tr style="background:#fafafa">
            <td style="padding:8px 12px;font-weight:600">合计</td>
            <td style="padding:8px 12px;text-align:right;font-weight:600" :class="{ 'sum-error': !!detailSumError }">
              {{ detailSum.toFixed(2) }}
            </td>
            <td colspan="2" style="padding:8px 12px;color:#909399;font-size:12px">
              应等于收款总金额 {{ (form.totalAmount || 0).toFixed(2) }}
            </td>
          </tr>
        </template>
      </el-table>
    </el-form>

    <template #footer>
      <div v-if="mode !== 'view'">
        <el-button @click="handleClose">取消</el-button>
        <el-button
          type="primary"
          :loading="saving"
          :disabled="!!detailSumError"
          @click="handleSave"
        >
          保存
        </el-button>
      </div>
      <el-button v-else @click="handleClose">关闭</el-button>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getReceiptDetail,
  createReceipt,
  updateReceipt,
  type ReceiptCreateDTO,
  type ReceiptDetailItem,
} from '@/api/fin/receipt'

// ─── Props & Emits ────────────────────────────────────────────────────────
const props = defineProps<{
  visible: boolean
  mode: 'create' | 'edit' | 'view'
  rowId?: number
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'saved'): void
}>()

const visible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val),
})

const drawerTitle = computed(() => {
  return { create: '新增收款单', edit: '编辑收款单', view: '收款单详情' }[props.mode]
})

// ─── 表单状态 ─────────────────────────────────────────────────────────────
const formRef = ref<FormInstance>()
const saving = ref(false)

const initForm = (): ReceiptCreateDTO & { details: ReceiptDetailItem[] } => ({
  contractId: undefined,
  brandId: undefined,
  shopCode: '',
  totalAmount: 0,
  paymentMethod: 1,
  bankSerialNo: '',
  payerName: '',
  bankName: '',
  bankAccount: '',
  isUnnamed: 0,
  accountingEntity: '',
  receiptDate: '',
  receiver: '',
  details: [],
})

const form = ref(initForm())

const rules: FormRules = {
  totalAmount: [{ required: true, message: '请填写收款总金额', trigger: 'blur' }],
  receiptDate: [{ required: true, message: '请选择收款日期', trigger: 'change' }],
  paymentMethod: [{ required: true, message: '请选择收款方式', trigger: 'change' }],
}

// ─── 加载详情 ─────────────────────────────────────────────────────────────
watch(() => [props.visible, props.rowId], async ([vis, id]) => {
  if (!vis) return
  if (id && (props.mode === 'edit' || props.mode === 'view')) {
    const res = await getReceiptDetail(id as number) as any
    const data = res
    form.value = {
      contractId: data.contractId,
      totalAmount: data.totalAmount,
      paymentMethod: data.paymentMethod,
      bankSerialNo: data.bankSerialNo,
      payerName: data.payerName,
      bankName: data.bankName,
      bankAccount: data.bankAccount,
      isUnnamed: data.isUnnamed,
      accountingEntity: data.accountingEntity,
      receiptDate: data.receiptDate,
      receiver: data.receiver,
      details: data.details || [],
    }
  } else {
    form.value = initForm()
  }
})

// ─── 明细操作 ─────────────────────────────────────────────────────────────
function addDetail() {
  form.value.details.push({ feeName: '', amount: 0, remark: '' })
}

function removeDetail(index: number) {
  form.value.details.splice(index, 1)
  checkDetailSum()
}

// 明细合计
const detailSum = computed(() => {
  return (form.value.details || []).reduce((sum, d) => sum + (d.amount || 0), 0)
})

// 合计校验错误文案
const detailSumError = computed(() => {
  if (!form.value.details?.length) return ''
  const diff = Math.abs(detailSum.value - (form.value.totalAmount || 0))
  if (diff > 0.005) {
    return `拆分明细合计 ${detailSum.value.toFixed(2)} 元，与收款总金额 ${(form.value.totalAmount || 0).toFixed(2)} 元不一致，请调整`
  }
  return ''
})

function checkDetailSum() {
  // 触发响应式重算（computed 自动执行）
}

// ─── 保存 ─────────────────────────────────────────────────────────────────
async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (detailSumError.value) {
    ElMessage.error(detailSumError.value)
    return
  }

  saving.value = true
  try {
    const payload: ReceiptCreateDTO = {
      ...form.value,
      details: form.value.details?.length ? form.value.details : undefined,
    }
    if (props.mode === 'create') {
      await createReceipt(payload)
      ElMessage.success('新增成功')
    } else {
      await updateReceipt(props.rowId!, payload)
      ElMessage.success('编辑成功')
    }
    emit('saved')
    handleClose()
  } catch {
  } finally {
    saving.value = false
  }
}

function handleClose() {
  visible.value = false
}
</script>

<style scoped>
.sum-error { color: #f56c6c; }
</style>
