<template>
  <div class="page-container">
    <el-page-header @back="router.back()" style="margin-bottom:16px">
      <template #content>
        <span style="font-weight:600;font-size:16px">合同到期预警</span>
      </template>
    </el-page-header>

    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="6">
        <el-card shadow="never" class="stat-card stat-danger">
          <div class="stat-num">{{ stats.expiring7 }}</div>
          <div class="stat-label">7天内到期</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card stat-warning">
          <div class="stat-num">{{ stats.expiring15 }}</div>
          <div class="stat-label">15天内到期</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card stat-info">
          <div class="stat-num">{{ stats.expiring30 }}</div>
          <div class="stat-label">30天内到期</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card stat-success">
          <div class="stat-num">{{ stats.total }}</div>
          <div class="stat-label">全部台账</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索栏 -->
    <el-card shadow="never">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span style="font-weight:600">预警台账列表</span>
          <el-radio-group v-model="daysFilter" size="small" @change="loadData">
            <el-radio-button :value="7">7天内</el-radio-button>
            <el-radio-button :value="15">15天内</el-radio-button>
            <el-radio-button :value="30">30天内</el-radio-button>
            <el-radio-button :value="0">全部</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        :row-class-name="rowClassName"
        style="width:100%"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="ledgerCode" label="台账编号" width="160" />
        <el-table-column prop="contractEnd" label="合同到期日" width="120" align="center">
          <template #default="{ row }">
            <span :class="expiryClass(row.contractEnd)">{{ row.contractEnd }}</span>
          </template>
        </el-table-column>
        <el-table-column label="剩余天数" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :type="daysTagType(row.contractEnd)"
              size="small"
            >
              {{ daysLabel(row.contractEnd) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="合同类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="contractTypeTagType(row.contractType)" size="small">
              {{ contractTypeLabel(row.contractType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="台账状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'primary' : 'info'" size="small">
              {{ row.status === 0 ? '进行中' : row.status === 1 ? '已完成' : '已解约' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="goDetail(row.id)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryForm.pageNum"
        v-model:page-size="queryForm.pageSize"
        :total="total"
        layout="total, prev, pager, next"
        style="margin-top:16px;justify-content:flex-end"
        @change="loadData"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getLedgerPage, type OprContractLedger } from '@/api/opr/ledger'

const router = useRouter()
const loading = ref(false)
const tableData = ref<OprContractLedger[]>([])
const total = ref(0)
const daysFilter = ref(30)

const queryForm = reactive({
  status: 0,   // 只看进行中
  contractEndFrom: '',
  contractEndTo: '',
  pageNum: 1,
  pageSize: 20,
})

const stats = reactive({ expiring7: 0, expiring15: 0, expiring30: 0, total: 0 })

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

type TagType = 'success' | 'primary' | 'warning' | 'info' | 'danger' | undefined

function daysTagType(contractEnd?: string): TagType {
  const d = daysUntil(contractEnd)
  if (d <= 7) return 'danger'
  if (d <= 15) return 'warning'
  return 'info'
}

function daysLabel(contractEnd?: string): string {
  const d = daysUntil(contractEnd)
  if (d < 0) return '已过期'
  if (d === 0) return '今日到期'
  return `${d} 天`
}

function rowClassName({ row }: { row: OprContractLedger }): string {
  const d = daysUntil(row.contractEnd)
  if (d <= 7) return 'row-danger'
  if (d <= 15) return 'row-warning'
  return ''
}

function contractTypeLabel(type?: number): string {
  const m: Record<number, string> = { 1: '租赁', 2: '联营', 3: '临时' }
  return type != null ? (m[type] || String(type)) : '-'
}
function contractTypeTagType(type?: number): TagType {
  const m: Record<number, TagType> = { 1: 'primary', 2: 'success', 3: 'info' }
  return type != null ? m[type] : undefined
}

async function loadData() {
  loading.value = true
  const today = new Date().toISOString().substring(0, 10)
  if (daysFilter.value > 0) {
    const future = new Date(Date.now() + daysFilter.value * 86400000).toISOString().substring(0, 10)
    queryForm.contractEndFrom = today
    queryForm.contractEndTo = future
  } else {
    queryForm.contractEndFrom = ''
    queryForm.contractEndTo = ''
  }
  try {
    const res = await getLedgerPage(queryForm)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  const today = new Date().toISOString().substring(0, 10)
  const make = (days: number) =>
    new Date(Date.now() + days * 86400000).toISOString().substring(0, 10)

  const [r7, r15, r30, rAll] = await Promise.allSettled([
    getLedgerPage({ status: 0, contractEndFrom: today, contractEndTo: make(7), pageSize: 1 }),
    getLedgerPage({ status: 0, contractEndFrom: today, contractEndTo: make(15), pageSize: 1 }),
    getLedgerPage({ status: 0, contractEndFrom: today, contractEndTo: make(30), pageSize: 1 }),
    getLedgerPage({ status: 0, pageSize: 1 }),
  ])
  if (r7.status === 'fulfilled') stats.expiring7 = r7.value.data?.total ?? 0
  if (r15.status === 'fulfilled') stats.expiring15 = r15.value.data?.total ?? 0
  if (r30.status === 'fulfilled') stats.expiring30 = r30.value.data?.total ?? 0
  if (rAll.status === 'fulfilled') stats.total = rAll.value.data?.total ?? 0
}

function goDetail(id: number) {
  router.push(`/opr/ledgers/${id}`)
}

onMounted(() => {
  loadData()
  loadStats()
})
</script>

<style scoped>
.stat-card {
  text-align: center;
  padding: 8px 0;
}
.stat-num {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}
.stat-danger .stat-num { color: #f56c6c; }
.stat-warning .stat-num { color: #e6a23c; }
.stat-info .stat-num { color: #409eff; }
.stat-success .stat-num { color: #67c23a; }

.expiry-red { color: #f56c6c; font-weight: 600; }
.expiry-orange { color: #e6a23c; font-weight: 600; }

:deep(.row-danger) { background-color: #fff0f0 !important; }
:deep(.row-warning) { background-color: #fff8e1 !important; }
</style>
