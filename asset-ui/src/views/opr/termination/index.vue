<template>
  <div class="page-container">
    <!-- 筛选栏 -->
    <el-card class="mb-16" shadow="never">
      <el-form :model="query" inline>
        <el-form-item label="解约单号">
          <el-input v-model="query.terminationCode" placeholder="请输入" clearable style="width:160px" />
        </el-form-item>
        <el-form-item label="解约类型">
          <el-select v-model="query.terminationType" placeholder="全部" clearable style="width:120px">
            <el-option label="到期终止" :value="1" />
            <el-option label="提前解约" :value="2" />
            <el-option label="重签解约" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:120px">
            <el-option label="草稿" :value="0" />
            <el-option label="审批中" :value="1" />
            <el-option label="已生效" :value="2" />
            <el-option label="已驳回" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表卡片 -->
    <el-card shadow="never">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>合同解约列表</span>
          <el-button type="primary" :icon="Plus" @click="$router.push('/opr/terminations/form')">新增解约</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column label="解约单号" prop="terminationCode" min-width="140" />
        <el-table-column label="合同编号" prop="contractCode" min-width="140" />
        <el-table-column label="合同名称" prop="contractName" min-width="180" show-overflow-tooltip />
        <el-table-column label="商家名称" prop="merchantName" min-width="130" show-overflow-tooltip />
        <el-table-column label="解约类型" prop="terminationType" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTagColor(row.terminationType)" size="small">
              {{ row.terminationTypeName || typeLabel(row.terminationType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="解约日期" prop="terminationDate" width="110" align="center" />
        <el-table-column label="清算总额(元)" prop="settlementAmount" width="130" align="right">
          <template #default="{ row }">
            <span v-if="row.settlementAmount != null" :class="row.settlementAmount >= 0 ? 'text-danger' : 'text-success'">
              {{ formatMoney(row.settlementAmount) }}
            </span>
            <span v-else class="text-gray">未计算</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagColor(row.status)" size="small">
              {{ row.statusName || statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row.id)">详情</el-button>
            <el-button v-if="row.status === 0 || row.status === 3" link type="primary"
              @click="editRecord(row.id)">编辑</el-button>
            <el-button v-if="row.status === 0" link type="warning"
              @click="doCalcSettlement(row.id)">计算清算</el-button>
            <el-button v-if="row.status === 0 && row.settlementAmount != null" link type="success"
              @click="doSubmitApproval(row.id)">提交审批</el-button>
            <el-button v-if="row.status === 1" link type="primary"
              @click="openCallback(row)">审批回调</el-button>
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
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import {
  getTerminationPage,
  calculateSettlement,
  submitTerminationApproval,
  terminationApprovalCallback,
  type TerminationDetailVO
} from '@/api/opr/termination'

const router = useRouter()

const query = reactive({
  terminationCode: '',
  terminationType: undefined as number | undefined,
  status: undefined as number | undefined,
  pageNum: 1,
  pageSize: 20
})
const total = ref(0)
const loading = ref(false)
const list = ref<TerminationDetailVO[]>([])

const callbackVisible = ref(false)
const callbackLoading = ref(false)
const callbackTerminationId = ref(0)
const callbackForm = reactive({ status: 2, comment: '' })

async function loadList() {
  loading.value = true
  try {
    const res = await getTerminationPage(query)
    if (res.data) {
      list.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.terminationCode = ''
  query.terminationType = undefined
  query.status = undefined
  query.pageNum = 1
  loadList()
}

function viewDetail(id: number) {
  router.push(`/opr/terminations/${id}`)
}

function editRecord(id: number) {
  router.push(`/opr/terminations/form?id=${id}`)
}

async function doCalcSettlement(id: number) {
  await ElMessageBox.confirm('是否触发清算计算？计算将覆盖已有清算数据。', '提示', { type: 'warning' })
  try {
    await calculateSettlement(id)
    ElMessage.success('清算计算完成')
    loadList()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '计算失败')
  }
}

async function doSubmitApproval(id: number) {
  await ElMessageBox.confirm('确认提交审批？', '提示', { type: 'info' })
  try {
    await submitTerminationApproval(id)
    ElMessage.success('已提交审批')
    loadList()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '提交失败')
  }
}

function openCallback(row: TerminationDetailVO) {
  callbackTerminationId.value = row.id
  callbackForm.status = 2
  callbackForm.comment = ''
  callbackVisible.value = true
}

async function doCallback() {
  callbackLoading.value = true
  try {
    await terminationApprovalCallback(callbackTerminationId.value, {
      status: callbackForm.status,
      comment: callbackForm.comment
    })
    ElMessage.success(callbackForm.status === 2 ? '审批通过，解约已执行' : '审批已驳回')
    callbackVisible.value = false
    loadList()
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
function formatMoney(val: number) {
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

onMounted(loadList)
</script>

<style scoped>
.mb-16 { margin-bottom: 16px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
.text-danger { color: #f56c6c; font-weight: 600; }
.text-success { color: #67c23a; font-weight: 600; }
.text-gray { color: #909399; }
</style>
