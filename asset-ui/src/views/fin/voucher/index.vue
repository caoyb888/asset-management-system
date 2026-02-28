<template>
  <div class="fin-voucher-page">
    <!-- 搜索栏 -->
    <el-card class="mb-4">
      <el-form :model="query" inline>
        <el-form-item label="凭证编号">
          <el-input v-model="query.voucherCode" placeholder="凭证编号" clearable style="width:160px" />
        </el-form-item>
        <el-form-item label="收付类型">
          <el-select v-model="query.payType" placeholder="全部" clearable style="width:110px">
            <el-option label="收款" :value="1" />
            <el-option label="付款" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:110px">
            <el-option label="待审核" :value="0" />
            <el-option label="已审核" :value="1" />
            <el-option label="已上传" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width:240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作栏 -->
    <el-card class="mb-4">
      <el-space>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">手动创建凭证</el-button>
        <el-button type="success" :icon="MagicStick" @click="openGenerateDialog">从收款单生成</el-button>
      </el-space>
    </el-card>

    <!-- 凭证列表 -->
    <el-card>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column label="凭证编号" prop="voucherCode" width="180" />
        <el-table-column label="项目" prop="projectName" min-width="120" show-overflow-tooltip />
        <el-table-column label="账套" prop="accountSet" width="110" />
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.payType === 1 ? 'success' : 'warning'" size="small">
              {{ row.payTypeName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="凭证日期" prop="voucherDate" width="110" />
        <el-table-column label="借方合计" align="right" width="120">
          <template #default="{ row }">{{ formatAmount(row.totalDebit) }}</template>
        </el-table-column>
        <el-table-column label="贷方合计" align="right" width="120">
          <template #default="{ row }">{{ formatAmount(row.totalCredit) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="上传时间" prop="uploadTime" width="160" show-overflow-tooltip />
        <el-table-column label="摘要" prop="remark" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 0"
              link type="success" size="small"
              @click="doAudit(row)"
            >审核</el-button>
            <el-button
              v-if="row.status === 1"
              link type="warning" size="small"
              @click="doUpload(row)"
            >上传</el-button>
            <el-button
              v-if="row.status === 0"
              link type="danger" size="small"
              @click="doDelete(row)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @change="loadList"
        />
      </div>
    </el-card>

    <!-- ─── 凭证详情抽屉 ─── -->
    <el-drawer v-model="detailVisible" title="凭证详情" size="700px" destroy-on-close>
      <template v-if="detailVO">
        <el-descriptions :column="2" border class="mb-4">
          <el-descriptions-item label="凭证编号">{{ detailVO.voucherCode }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTag(detailVO.status)" size="small">{{ detailVO.statusName }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="收付类型">{{ detailVO.payTypeName }}</el-descriptions-item>
          <el-descriptions-item label="凭证日期">{{ detailVO.voucherDate }}</el-descriptions-item>
          <el-descriptions-item label="项目">{{ detailVO.projectName }}</el-descriptions-item>
          <el-descriptions-item label="账套">{{ detailVO.accountSet }}</el-descriptions-item>
          <el-descriptions-item label="借方合计">{{ formatAmount(detailVO.totalDebit) }}</el-descriptions-item>
          <el-descriptions-item label="贷方合计">{{ formatAmount(detailVO.totalCredit) }}</el-descriptions-item>
          <el-descriptions-item label="摘要" :span="2">{{ detailVO.remark || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="detailVO.uploadTime" label="上传时间" :span="2">
            {{ detailVO.uploadTime }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="section-title">分录明细</div>
        <el-table :data="detailVO.entries ?? []" border size="small">
          <el-table-column label="科目编码" prop="accountCode" width="100" />
          <el-table-column label="科目名称" prop="accountName" min-width="140" />
          <el-table-column label="借方（元）" align="right" width="120">
            <template #default="{ row }">
              <span v-if="row.debitAmount > 0" class="text-blue">{{ formatAmount(row.debitAmount) }}</span>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="贷方（元）" align="right" width="120">
            <template #default="{ row }">
              <span v-if="row.creditAmount > 0" class="text-orange">{{ formatAmount(row.creditAmount) }}</span>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="来源类型" width="100">
            <template #default="{ row }">{{ sourceTypeLabel(row.sourceType) }}</template>
          </el-table-column>
          <el-table-column label="摘要" prop="summary" min-width="160" show-overflow-tooltip />
        </el-table>

        <!-- 分录底部合计 -->
        <div class="entry-total">
          <span>借方合计：<strong class="text-blue">{{ formatAmount(detailVO.totalDebit) }}</strong></span>
          <span class="ml-4">贷方合计：<strong class="text-orange">{{ formatAmount(detailVO.totalCredit) }}</strong></span>
          <el-tag
            class="ml-4"
            :type="isBalanced(detailVO) ? 'success' : 'danger'"
            size="small"
          >{{ isBalanced(detailVO) ? '借贷平衡' : '借贷不平！' }}</el-tag>
        </div>

        <div class="drawer-actions">
          <el-button
            v-if="detailVO.status === 0"
            type="success"
            @click="doAuditDetail"
          >审核</el-button>
          <el-button
            v-if="detailVO.status === 1"
            type="warning"
            @click="doUploadDetail"
          >上传到财务系统</el-button>
        </div>
      </template>
      <el-skeleton v-else :rows="6" animated />
    </el-drawer>

    <!-- ─── 手动创建凭证弹窗 ─── -->
    <el-dialog v-model="createVisible" title="手动创建凭证" width="900px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="项目ID" prop="projectId">
              <el-input-number v-model="createForm.projectId" :min="1" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="收付类型" prop="payType">
              <el-select v-model="createForm.payType" style="width:100%">
                <el-option label="收款" :value="1" />
                <el-option label="付款" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="凭证日期" prop="voucherDate">
              <el-date-picker v-model="createForm.voucherDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="账套">
              <el-input v-model="createForm.accountSet" placeholder="默认账套" />
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="摘要">
              <el-input v-model="createForm.remark" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 分录列表 -->
        <div class="section-title">
          分录明细
          <el-button type="primary" link :icon="Plus" @click="addEntry">添加分录</el-button>
        </div>
        <el-table :data="createForm.entries" border size="small">
          <el-table-column label="科目编码" width="130">
            <template #default="{ row }">
              <el-input v-model="row.accountCode" placeholder="如 1002" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="科目名称" min-width="140">
            <template #default="{ row }">
              <el-input v-model="row.accountName" placeholder="如 银行存款" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="借方（元）" width="130">
            <template #default="{ row }">
              <el-input-number v-model="row.debitAmount" :min="0" :precision="2" size="small" style="width:100%" @change="recalcTotals" />
            </template>
          </el-table-column>
          <el-table-column label="贷方（元）" width="130">
            <template #default="{ row }">
              <el-input-number v-model="row.creditAmount" :min="0" :precision="2" size="small" style="width:100%" @change="recalcTotals" />
            </template>
          </el-table-column>
          <el-table-column label="摘要" min-width="160">
            <template #default="{ row }">
              <el-input v-model="row.summary" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="" width="50">
            <template #default="{ $index }">
              <el-button link type="danger" :icon="Delete" @click="removeEntry($index)" />
            </template>
          </el-table-column>
        </el-table>

        <!-- 合计校验行 -->
        <div class="entry-total mt-2">
          <span>借方合计：<strong class="text-blue">{{ formatAmount(calcTotalDebit) }}</strong></span>
          <span class="ml-4">贷方合计：<strong class="text-orange">{{ formatAmount(calcTotalCredit) }}</strong></span>
          <el-tag
            class="ml-4"
            :type="Math.abs(calcTotalDebit - calcTotalCredit) <= 0.01 ? 'success' : 'danger'"
            size="small"
          >{{ Math.abs(calcTotalDebit - calcTotalCredit) <= 0.01 ? '借贷平衡' : '借贷不平！' }}</el-tag>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="submitCreate">创建凭证</el-button>
      </template>
    </el-dialog>

    <!-- ─── 从收款单生成弹窗 ─── -->
    <el-dialog v-model="generateVisible" title="从收款单生成凭证" width="420px" destroy-on-close>
      <el-form ref="generateFormRef" :model="generateForm" :rules="generateRules" label-width="100px">
        <el-form-item label="收款单ID" prop="receiptId">
          <el-input-number v-model="generateForm.receiptId" :min="1" style="width:100%" />
        </el-form-item>
        <el-alert
          class="mt-2"
          title="将自动生成标准收款凭证：借 银行存款(1002) / 贷 应收账款(1122)"
          type="info"
          show-icon
          :closable="false"
        />
      </el-form>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :loading="generateLoading" @click="submitGenerate">生成凭证</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Delete, MagicStick } from '@element-plus/icons-vue'
import {
  getVoucherPage,
  getVoucherDetail,
  createVoucher,
  generateFromReceipt,
  auditVoucher,
  uploadVoucher,
  deleteVoucher,
  type VoucherDetailVO,
  type VoucherEntryDTO,
} from '@/api/fin/voucher'

// ─── 列表 ─────────────────────────────────────────────────────────────────
const loading = ref(false)
const list = ref<VoucherDetailVO[]>([])
const total = ref(0)
const dateRange = ref<string[]>([])

const query = reactive({
  voucherCode: '',
  payType: undefined as number | undefined,
  status: undefined as number | undefined,
  pageNum: 1,
  pageSize: 10,
})

async function loadList() {
  loading.value = true
  try {
    const params: any = { ...query }
    if (dateRange.value?.length === 2) {
      params.dateFrom = dateRange.value[0]
      params.dateTo   = dateRange.value[1]
    }
    const res: any = await getVoucherPage(params)
    list.value  = res.data?.records ?? []
    total.value = res.data?.total   ?? 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadList()
}

function resetQuery() {
  query.voucherCode = ''
  query.payType = undefined
  query.status  = undefined
  dateRange.value = []
  handleSearch()
}

onMounted(loadList)

// ─── 格式化 ────────────────────────────────────────────────────────────────
function formatAmount(v?: number | null) {
  if (v == null) return '0.00'
  return Number(v).toFixed(2)
}

function statusTag(s?: number) {
  const m: Record<number, string> = { 0: 'warning', 1: 'primary', 2: 'success' }
  return s !== undefined ? m[s] ?? '' : ''
}

function sourceTypeLabel(t?: number | null) {
  const m: Record<number, string> = { 1: '收款单', 2: '核销单', 3: '应收单' }
  return t ? m[t] ?? '-' : '-'
}

function isBalanced(vo: VoucherDetailVO) {
  return Math.abs((vo.totalDebit ?? 0) - (vo.totalCredit ?? 0)) <= 0.01
}

// ─── 详情抽屉 ──────────────────────────────────────────────────────────────
const detailVisible = ref(false)
const detailVO = ref<VoucherDetailVO | null>(null)

async function openDetail(row: VoucherDetailVO) {
  detailVO.value = null
  detailVisible.value = true
  const res: any = await getVoucherDetail(row.id)
  detailVO.value = res.data
}

async function doAuditDetail() {
  if (!detailVO.value) return
  await auditVoucher(detailVO.value.id)
  ElMessage.success('审核通过')
  detailVO.value.status = 1
  detailVO.value.statusName = '已审核'
  loadList()
}

async function doUploadDetail() {
  if (!detailVO.value) return
  await uploadVoucher(detailVO.value.id)
  ElMessage.success('已上传到财务系统')
  detailVO.value.status = 2
  detailVO.value.statusName = '已上传'
  loadList()
}

// ─── 行操作 ────────────────────────────────────────────────────────────────
async function doAudit(row: VoucherDetailVO) {
  await auditVoucher(row.id)
  ElMessage.success('审核通过')
  loadList()
}

async function doUpload(row: VoucherDetailVO) {
  await ElMessageBox.confirm(
    `确认上传凭证 ${row.voucherCode} 到财务系统？上传后不可撤销。`,
    '确认上传', { type: 'warning' }
  )
  await uploadVoucher(row.id)
  ElMessage.success('上传成功')
  loadList()
}

async function doDelete(row: VoucherDetailVO) {
  await ElMessageBox.confirm(
    `确认删除凭证 ${row.voucherCode}？此操作不可撤销。`,
    '确认删除', { type: 'warning' }
  )
  await deleteVoucher(row.id)
  ElMessage.success('删除成功')
  loadList()
}

// ─── 手动创建凭证 ──────────────────────────────────────────────────────────
const createVisible = ref(false)
const createLoading = ref(false)
const createFormRef = ref()

const createForm = reactive({
  projectId: undefined as number | undefined,
  payType: 1,
  voucherDate: '',
  accountSet: '',
  remark: '',
  entries: [] as VoucherEntryDTO[],
})

const createRules = {
  projectId:   [{ required: true, message: '项目ID不能为空', trigger: 'blur' }],
  payType:     [{ required: true, message: '请选择收付类型', trigger: 'change' }],
  voucherDate: [{ required: true, message: '请选择凭证日期', trigger: 'change' }],
}

const calcTotalDebit = computed(() =>
  createForm.entries.reduce((s, e) => s + (e.debitAmount || 0), 0)
)
const calcTotalCredit = computed(() =>
  createForm.entries.reduce((s, e) => s + (e.creditAmount || 0), 0)
)

function openCreateDialog() {
  createForm.projectId = undefined
  createForm.payType = 1
  createForm.voucherDate = ''
  createForm.accountSet = ''
  createForm.remark = ''
  createForm.entries = [
    { accountCode: '', accountName: '', debitAmount: 0, creditAmount: 0, summary: '' },
    { accountCode: '', accountName: '', debitAmount: 0, creditAmount: 0, summary: '' },
  ]
  createVisible.value = true
}

function addEntry() {
  createForm.entries.push({ accountCode: '', accountName: '', debitAmount: 0, creditAmount: 0, summary: '' })
}

function removeEntry(idx: number) {
  createForm.entries.splice(idx, 1)
}

function recalcTotals() { /* computed 自动更新 */ }

async function submitCreate() {
  await createFormRef.value?.validate()
  if (createForm.entries.length < 2) {
    ElMessage.warning('至少需要2条分录')
    return
  }
  if (Math.abs(calcTotalDebit.value - calcTotalCredit.value) > 0.01) {
    ElMessage.error('借贷不平衡，请检查分录金额')
    return
  }
  createLoading.value = true
  try {
    await createVoucher({ ...createForm } as any)
    ElMessage.success('凭证创建成功')
    createVisible.value = false
    loadList()
  } finally {
    createLoading.value = false
  }
}

// ─── 从收款单生成 ──────────────────────────────────────────────────────────
const generateVisible = ref(false)
const generateLoading = ref(false)
const generateFormRef = ref()
const generateForm = reactive({ receiptId: undefined as number | undefined })
const generateRules = {
  receiptId: [{ required: true, message: '请输入收款单ID', trigger: 'blur' }],
}

function openGenerateDialog() {
  generateForm.receiptId = undefined
  generateVisible.value = true
}

async function submitGenerate() {
  await generateFormRef.value?.validate()
  generateLoading.value = true
  try {
    await generateFromReceipt(generateForm.receiptId!)
    ElMessage.success('凭证已生成')
    generateVisible.value = false
    loadList()
  } finally {
    generateLoading.value = false
  }
}
</script>

<style scoped>
.fin-voucher-page {
  padding: 16px;
}
.mb-4 { margin-bottom: 16px; }
.mt-2 { margin-top: 8px; }
.ml-4 { margin-left: 16px; }
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  padding: 8px 0 12px;
  display: flex;
  align-items: center;
  gap: 12px;
}
.entry-total {
  display: flex;
  align-items: center;
  padding: 10px 0;
  font-size: 14px;
  color: #606266;
}
.drawer-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
  gap: 12px;
}
.text-blue   { color: #409eff; font-weight: 600; }
.text-orange { color: #e6a23c; font-weight: 600; }
.text-muted  { color: #c0c4cc; }
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
