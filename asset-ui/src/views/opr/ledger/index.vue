<template>
  <div class="ledger-page">
    <!-- 搜索栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form :model="queryForm" inline>
        <el-form-item label="台账编号">
          <el-input v-model="queryForm.ledgerCode" placeholder="请输入台账编号" clearable style="width:160px" />
        </el-form-item>
        <el-form-item label="合同类型">
          <el-select v-model="queryForm.contractType" placeholder="全部" clearable style="width:120px">
            <el-option :value="1" label="租赁合同" />
            <el-option :value="2" label="联营合同" />
            <el-option :value="3" label="临时合同" />
          </el-select>
        </el-form-item>
        <el-form-item label="台账状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width:120px">
            <el-option :value="0" label="进行中" />
            <el-option :value="1" label="已完成" />
            <el-option :value="2" label="已解约" />
          </el-select>
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="queryForm.auditStatus" placeholder="全部" clearable style="width:120px">
            <el-option :value="0" label="待审核" />
            <el-option :value="1" label="已通过" />
            <el-option :value="2" label="已驳回" />
          </el-select>
        </el-form-item>
        <el-form-item label="合同到期">
          <el-date-picker
            v-model="contractEndRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width:220px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <div class="card-header">
        <div class="header-left">
          <span class="header-title">合同台账列表</span>
          <span class="count-tag">共 {{ total }} 条</span>
        </div>
        <div class="header-actions">
          <el-button type="warning" :icon="Warning" size="small" @click="goAlerts">到期预警</el-button>
        </div>
      </div>

      <div class="table-body">
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        :row-class-name="rowClassName"
        @row-click="handleRowClick"
        style="width:100%;cursor:pointer"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="ledgerCode" label="台账编号" width="160" />
        <el-table-column label="合同类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="contractTypeTagType(row.contractType)" size="small">
              {{ contractTypeLabel(row.contractType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="contractStart" label="合同开始" width="110" align="center" />
        <el-table-column prop="contractEnd" label="合同到期" width="110" align="center">
          <template #default="{ row }">
            <span :class="expiryClass(row.contractEnd)">{{ row.contractEnd }}</span>
          </template>
        </el-table-column>
        <el-table-column label="双签状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.doubleSignStatus === 1 ? 'success' : 'info'" size="small">
              {{ row.doubleSignStatus === 1 ? '已双签' : '待双签' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="应收状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="receivableStatusType(row.receivableStatus)" size="small">
              {{ receivableStatusLabel(row.receivableStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="auditStatusType(row.auditStatus)" size="small">
              {{ auditStatusLabel(row.auditStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="台账状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="ledgerStatusType(row.status)" size="small">
              {{ ledgerStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click.stop="goDetail(row.id)">详情</el-button>
            <el-button
              v-if="row.doubleSignStatus === 0"
              type="success" link size="small"
              @click.stop="handleDoubleSign(row)"
            >双签</el-button>
            <el-button
              v-if="row.receivableStatus === 0 && row.doubleSignStatus === 1"
              type="warning" link size="small"
              @click.stop="handleGenerateReceivable(row)"
            >生成应收</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="queryForm.pageNum"
          v-model:page-size="queryForm.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Warning } from '@element-plus/icons-vue'
import {
  getLedgerPage,
  doubleSign,
  generateReceivable,
  type OprContractLedger,
} from '@/api/opr/ledger'

const router = useRouter()

const loading = ref(false)
const tableData = ref<OprContractLedger[]>([])
const total = ref(0)
const contractEndRange = ref<[string, string] | null>(null)

const queryForm = reactive({
  ledgerCode: '',
  contractType: undefined as number | undefined,
  status: undefined as number | undefined,
  auditStatus: undefined as number | undefined,
  contractEndFrom: '',
  contractEndTo: '',
  pageNum: 1,
  pageSize: 20,
})

/** 距离到期天数 */
function daysUntilExpiry(contractEnd?: string): number {
  if (!contractEnd) return Infinity
  const diff = new Date(contractEnd).getTime() - Date.now()
  return Math.ceil(diff / 86400000)
}

/** 到期预警样式：≤7天红色，≤15天橙色 */
function expiryClass(contractEnd?: string): string {
  const days = daysUntilExpiry(contractEnd)
  if (days <= 7) return 'expiry-red'
  if (days <= 15) return 'expiry-orange'
  return ''
}

/** 表格行高亮（≤15天到期预警） */
function rowClassName({ row }: { row: OprContractLedger }): string {
  return daysUntilExpiry(row.contractEnd) <= 15 ? 'row-warning' : ''
}

type TagType = 'success' | 'primary' | 'warning' | 'info' | 'danger' | undefined

function contractTypeLabel(type?: number): string {
  const map: Record<number, string> = { 1: '租赁', 2: '联营', 3: '临时' }
  return type != null ? (map[type] || String(type)) : '-'
}
function contractTypeTagType(type?: number): TagType {
  const map: Record<number, TagType> = { 1: 'primary', 2: 'success', 3: 'info' }
  return type != null ? map[type] : undefined
}

function receivableStatusLabel(s?: number): string {
  const map: Record<number, string> = { 0: '未生成', 1: '已生成', 2: '已推送' }
  return s != null ? (map[s] || String(s)) : '-'
}
function receivableStatusType(s?: number): TagType {
  const map: Record<number, TagType> = { 0: 'info', 1: 'warning', 2: 'success' }
  return s != null ? map[s] : undefined
}

function auditStatusLabel(s?: number): string {
  const map: Record<number, string> = { 0: '待审核', 1: '已通过', 2: '已驳回' }
  return s != null ? (map[s] || String(s)) : '-'
}
function auditStatusType(s?: number): TagType {
  const map: Record<number, TagType> = { 0: 'info', 1: 'success', 2: 'danger' }
  return s != null ? map[s] : undefined
}

function ledgerStatusLabel(s?: number): string {
  const map: Record<number, string> = { 0: '进行中', 1: '已完成', 2: '已解约' }
  return s != null ? (map[s] || String(s)) : '-'
}
function ledgerStatusType(s?: number): TagType {
  const map: Record<number, TagType> = { 0: 'primary', 1: 'success', 2: 'danger' }
  return s != null ? map[s] : undefined
}

async function loadData() {
  if (contractEndRange.value) {
    queryForm.contractEndFrom = contractEndRange.value[0]
    queryForm.contractEndTo = contractEndRange.value[1]
  } else {
    queryForm.contractEndFrom = ''
    queryForm.contractEndTo = ''
  }
  loading.value = true
  try {
    const res = await getLedgerPage(queryForm)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  queryForm.pageNum = 1
  loadData()
}

function handleReset() {
  queryForm.ledgerCode = ''
  queryForm.contractType = undefined
  queryForm.status = undefined
  queryForm.auditStatus = undefined
  contractEndRange.value = null
  queryForm.contractEndFrom = ''
  queryForm.contractEndTo = ''
  queryForm.pageNum = 1
  loadData()
}

function goDetail(id: number) {
  router.push(`/opr/ledgers/${id}`)
}

function handleRowClick(row: OprContractLedger) {
  goDetail(row.id)
}

function goAlerts() {
  router.push('/opr/alerts/contract-expiry')
}

async function handleDoubleSign(row: OprContractLedger) {
  try {
    await ElMessageBox.confirm(`确认为台账【${row.ledgerCode}】完成双签操作？`, '双签确认', {
      type: 'warning',
    })
    await doubleSign(row.id)
    ElMessage.success('双签确认成功')
    loadData()
  } catch (e) {
    // 取消操作
  }
}

async function handleGenerateReceivable(row: OprContractLedger) {
  try {
    await ElMessageBox.confirm(`确认为台账【${row.ledgerCode}】生成应收计划？`, '生成应收计划', {
      type: 'warning',
    })
    const res = await generateReceivable(row.id)
    ElMessage.success(`应收计划生成成功，共 ${res.data} 条`)
    loadData()
  } catch (e) {
    // 取消操作
  }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.ledger-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-card {
  border-radius: 12px !important;
  border: 1px solid rgba(0, 0, 0, 0.06) !important;
  transition: box-shadow 0.2s;
  &:hover { box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08) !important; }
  :deep(.el-card__body) { padding: 14px 20px; }
  :deep(.el-form-item) { margin-bottom: 0; }
}

.table-card {
  border-radius: 12px !important;
  border: 1px solid rgba(0, 0, 0, 0.06) !important;
  overflow: hidden;
  :deep(.el-card__body) { padding: 0; }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid #f1f5f9;
  background: #fff;

  .header-left { display: flex; align-items: center; gap: 10px; }

  .header-title {
    font-size: 15px;
    font-weight: 600;
    color: #1e293b;
    display: flex;
    align-items: center;
    gap: 8px;
    &::before {
      content: '';
      display: inline-block;
      width: 3px;
      height: 16px;
      background: linear-gradient(180deg, #3b82f6, #60a5fa);
      border-radius: 2px;
    }
  }

  .count-tag {
    font-size: 12px;
    background: #eff6ff;
    color: #3b82f6;
    border: 1px solid #bfdbfe;
    border-radius: 10px;
    padding: 2px 10px;
    font-weight: 500;
  }

  .header-actions { display: flex; gap: 8px; align-items: center; }
}

.table-body {
  padding: 16px 20px;

  :deep(.el-table) {
    border-radius: 8px;
    overflow: hidden;

    .el-table__header-wrapper th.el-table__cell {
      background: #f8fafc;
      color: #64748b;
      font-weight: 600;
      font-size: 13px;
      border-bottom: 1px solid #e8edf3;
    }

    .el-table__row:hover > td.el-table__cell { background-color: #f0f7ff !important; }
    .el-table__row--striped > td.el-table__cell { background-color: #fafbfc; }
    td.el-table__cell { border-bottom: 1px solid #f4f6f9; }
  }
}

.pagination { margin-top: 14px; display: flex; justify-content: flex-end; }

:deep(.row-warning) { background-color: #fff8e1 !important; }
.expiry-red { color: #f56c6c; font-weight: 600; }
.expiry-orange { color: #e6a23c; font-weight: 600; }
</style>
