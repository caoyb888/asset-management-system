<template>
  <div class="expiry-page">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
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

    <!-- 预警列表 -->
    <el-card shadow="never" class="table-card">
      <div class="card-header">
        <div class="header-left">
          <span class="header-title">预警台账列表</span>
          <span class="count-tag">共 {{ total }} 条</span>
        </div>
        <div class="header-actions">
          <el-radio-group v-model="daysFilter" size="small" @change="loadData">
            <el-radio-button :value="7">7天内</el-radio-button>
            <el-radio-button :value="15">15天内</el-radio-button>
            <el-radio-button :value="30">30天内</el-radio-button>
            <el-radio-button :value="0">全部</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <div class="table-body">
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

      <div class="pagination">
        <el-pagination
          v-model:current-page="queryForm.pageNum"
          v-model:page-size="queryForm.pageSize"
          :total="total"
          layout="total, prev, pager, next"
          background
          @change="loadData"
        />
      </div>
      </div>
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

<style scoped lang="scss">
.expiry-page { display: flex; flex-direction: column; gap: 16px; }

.stat-row { margin-bottom: 0; }

.stat-card {
  text-align: center;
  padding: 12px 0;
  border-radius: 12px !important;
  border: 1px solid rgba(0, 0, 0, 0.06) !important;
  transition: box-shadow 0.2s, transform 0.2s;
  &:hover { box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08) !important; transform: translateY(-2px); }
}
.stat-num { font-size: 32px; font-weight: 700; line-height: 1.2; }
.stat-label { font-size: 13px; color: #64748b; margin-top: 6px; }
.stat-danger { border-left: 3px solid #f56c6c !important; .stat-num { color: #f56c6c; } }
.stat-warning { border-left: 3px solid #f59e0b !important; .stat-num { color: #f59e0b; } }
.stat-info { border-left: 3px solid #3b82f6 !important; .stat-num { color: #3b82f6; } }
.stat-success { border-left: 3px solid #10b981 !important; .stat-num { color: #10b981; } }

.table-card {
  border-radius: 12px !important;
  border: 1px solid rgba(0, 0, 0, 0.06) !important;
  overflow: hidden;
  :deep(.el-card__body) { padding: 0; }
}

.card-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 20px; border-bottom: 1px solid #f1f5f9; background: #fff;
  .header-left { display: flex; align-items: center; gap: 10px; }
  .header-title {
    font-size: 15px; font-weight: 600; color: #1e293b;
    display: flex; align-items: center; gap: 8px;
    &::before { content: ''; display: inline-block; width: 3px; height: 16px;
      background: linear-gradient(180deg, #f56c6c, #fca5a5); border-radius: 2px; }
  }
  .count-tag {
    font-size: 12px; background: #fff0f0; color: #f56c6c;
    border: 1px solid #fca5a5; border-radius: 10px; padding: 2px 10px; font-weight: 500;
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
.expiry-red { color: #f56c6c; font-weight: 600; }
.expiry-orange { color: #e6a23c; font-weight: 600; }
:deep(.row-danger) { background-color: #fff0f0 !important; }
:deep(.row-warning) { background-color: #fff8e1 !important; }
</style>
