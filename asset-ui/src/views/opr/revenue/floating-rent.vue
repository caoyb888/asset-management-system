<template>
  <div class="float-rent-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="filter-card">
      <el-form :model="query" inline>
        <el-form-item label="合同ID">
          <el-input-number
            v-model="query.contractId"
            :min="1"
            :controls="false"
            placeholder="输入合同ID"
            style="width:150px"
          />
        </el-form-item>
        <el-form-item label="月份">
          <el-date-picker
            v-model="query.calcMonth"
            type="month"
            value-format="YYYY-MM"
            placeholder="选择月份"
            style="width:150px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="doSearch">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 浮动租金列表 -->
    <el-card shadow="never" class="table-card">
      <div class="card-header">
        <div class="header-left">
          <span class="header-title">浮动租金列表</span>
          <span class="count-tag">共 {{ total }} 条</span>
        </div>
      </div>
      <div class="table-body">
        <el-table v-loading="loading" :data="tableData" border stripe @row-click="openDetail">
          <el-table-column label="合同ID"     prop="contractId"     width="100" />
          <el-table-column label="计算月份"   prop="calcMonth"      width="110" />
          <el-table-column label="月营业额"   prop="monthlyRevenue" align="right">
            <template #default="{ row }">¥{{ fmt(row.monthlyRevenue) }}</template>
          </el-table-column>
          <el-table-column label="固定租金"   prop="fixedRent"      align="right">
            <template #default="{ row }">{{ row.fixedRent ? '¥' + fmt(row.fixedRent) : '—' }}</template>
          </el-table-column>
          <el-table-column label="提成率(%)"  prop="commissionRate" width="100" align="right" />
          <el-table-column label="提成金额"   prop="commissionAmount" align="right">
            <template #default="{ row }">¥{{ fmt(row.commissionAmount) }}</template>
          </el-table-column>
          <el-table-column label="浮动租金" align="right">
            <template #default="{ row }">
              <span class="floating-rent-val">¥{{ fmt(row.floatingRent) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="应收状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.receivableId" type="success" size="small">已生成应收</el-tag>
              <el-tag v-else type="info" size="small">未生成</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click.stop="openDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination">
          <el-pagination
            v-model:current-page="query.pageNum"
            v-model:page-size="query.pageSize"
            :total="total"
            layout="total, prev, pager, next"
            background
            @change="loadList"
          />
        </div>
      </div>
    </el-card>

    <!-- 触发计算区 -->
    <el-card shadow="never" class="calc-card">
      <div class="card-header">
        <div class="header-left">
          <span class="header-title">触发浮动租金计算</span>
          <el-tooltip content="月度营业额填报完整后才可计算" placement="right">
            <el-icon style="color:#909399"><QuestionFilled /></el-icon>
          </el-tooltip>
        </div>
      </div>
      <div class="calc-body">
        <el-form :model="calcForm" inline>
          <el-form-item label="合同ID" required>
            <el-input-number v-model="calcForm.contractId" :min="1" :controls="false" style="width:150px" />
          </el-form-item>
          <el-form-item label="月份" required>
            <el-date-picker v-model="calcForm.calcMonth" type="month" value-format="YYYY-MM" style="width:150px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="calculating" @click="doCalculate">开始计算</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="浮动租金详情" size="560px" direction="rtl">
      <div v-if="currentDetail" v-loading="detailLoading">
        <!-- 汇总信息 -->
        <el-descriptions title="计算结果" :column="2" border size="small">
          <el-descriptions-item label="合同ID">{{ currentDetail.contractId }}</el-descriptions-item>
          <el-descriptions-item label="计算月份">{{ currentDetail.calcMonth }}</el-descriptions-item>
          <el-descriptions-item label="收费方式">
            <el-tag size="small" :type="chargeTypeColor(currentDetail.chargeType)">
              {{ currentDetail.chargeTypeName }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="月营业额">
            ¥{{ fmt(currentDetail.monthlyRevenue) }}
          </el-descriptions-item>
          <el-descriptions-item v-if="currentDetail.fixedRent" label="固定保底租金">
            ¥{{ fmt(currentDetail.fixedRent) }}
          </el-descriptions-item>
          <el-descriptions-item v-if="currentDetail.commissionRate" label="提成率">
            {{ currentDetail.commissionRate }}%
          </el-descriptions-item>
          <el-descriptions-item label="提成金额">
            ¥{{ fmt(currentDetail.commissionAmount) }}
          </el-descriptions-item>
          <el-descriptions-item label="浮动租金">
            <span style="color:#2E75B6;font-weight:700;font-size:16px">
              ¥{{ fmt(currentDetail.floatingRent) }}
            </span>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 计算公式 -->
        <div v-if="currentDetail.calcFormula" style="margin-top:16px">
          <div style="font-weight:600;margin-bottom:8px">计算公式</div>
          <el-alert :title="currentDetail.calcFormula" type="info" show-icon :closable="false" />
        </div>

        <!-- 阶梯明细 -->
        <div v-if="currentDetail.tiers && currentDetail.tiers.length" style="margin-top:16px">
          <div style="font-weight:600;margin-bottom:8px">阶梯明细</div>
          <el-table :data="currentDetail.tiers" border size="small">
            <el-table-column label="档位" prop="tierNo" width="60" align="center" />
            <el-table-column label="起始营业额（元）" align="right">
              <template #default="{ row }">{{ row.revenueFrom != null ? fmt(row.revenueFrom) : '0' }}</template>
            </el-table-column>
            <el-table-column label="终止营业额（元）" align="right">
              <template #default="{ row }">{{ row.revenueTo != null ? fmt(row.revenueTo) : '无上限' }}</template>
            </el-table-column>
            <el-table-column label="提成率(%)" prop="rate" width="90" align="right" />
            <el-table-column label="本档提成（元）" align="right">
              <template #default="{ row }">
                <span style="color:#67C23A;font-weight:600">¥{{ fmt(row.tierAmount) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 生成应收 -->
        <div style="margin-top:20px">
          <el-tag v-if="currentDetail.receivableId" type="success">
            已生成应收计划（ID: {{ currentDetail.receivableId }}）
          </el-tag>
          <el-button
            v-else
            type="primary"
            :loading="generatingReceivable"
            @click="doGenerateReceivable"
          >生成应收计划</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Refresh, QuestionFilled } from '@element-plus/icons-vue'
import { floatingRentApi, type OprFloatingRent, type FloatingRentDetailVO } from '@/api/opr/revenue'

const route = useRoute()

// ── 列表 ────────────────────────────────────────────────────────
const loading   = ref(false)
const tableData = ref<OprFloatingRent[]>([])
const total     = ref(0)
const query     = reactive<{ contractId?: number; calcMonth?: string; pageNum: number; pageSize: number }>({
  pageNum: 1, pageSize: 20,
})

async function loadList() {
  loading.value = true
  try {
    const res = await floatingRentApi.page(query) as any
    tableData.value = res.records || []; total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function doSearch() { query.pageNum = 1; loadList() }
function resetQuery() {
  Object.assign(query, { contractId: undefined, calcMonth: undefined, pageNum: 1 })
  loadList()
}

// ── 计算触发 ────────────────────────────────────────────────────
const calculating = ref(false)
const calcForm    = reactive<{ contractId: number | null; calcMonth: string }>({
  contractId: null, calcMonth: new Date().toISOString().slice(0, 7),
})

async function doCalculate() {
  if (!calcForm.contractId || !calcForm.calcMonth) {
    ElMessage.warning('请填写合同ID和月份')
    return
  }
  calculating.value = true
  try {
    const res = await floatingRentApi.generate({
      contractId: calcForm.contractId,
      calcMonth: calcForm.calcMonth,
    }) as any
    ElMessage.success('计算成功，记录ID=' + res)
    loadList()
  } finally {
    calculating.value = false
  }
}

// ── 详情抽屉 ────────────────────────────────────────────────────
const drawerVisible      = ref(false)
const detailLoading      = ref(false)
const currentDetail      = ref<FloatingRentDetailVO | null>(null)
const generatingReceivable = ref(false)

async function openDetail(row: OprFloatingRent) {
  drawerVisible.value = true
  detailLoading.value = true
  currentDetail.value = null
  try {
    const res = await floatingRentApi.detail(row.id) as any
    currentDetail.value = res
  } finally {
    detailLoading.value = false
  }
}

async function doGenerateReceivable() {
  if (!currentDetail.value) return
  generatingReceivable.value = true
  try {
    const res = await floatingRentApi.generateReceivable(currentDetail.value.id) as any
    ElMessage.success('应收计划已生成，ID=' + res)
    currentDetail.value.receivableId = res
    loadList()
  } finally {
    generatingReceivable.value = false
  }
}

function chargeTypeColor(type?: number) {
  if (type === 3) return 'warning'
  if (type === 4) return 'danger'
  return 'primary'
}

function fmt(val: number | null | undefined) {
  if (val == null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

// 支持路由参数预填
onMounted(() => {
  if (route.query.contractId) query.contractId = Number(route.query.contractId)
  if (route.query.calcMonth)  query.calcMonth  = String(route.query.calcMonth)
  loadList()
})
</script>

<style scoped lang="scss">
.float-rent-page { display: flex; flex-direction: column; gap: 16px; }

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

.calc-card {
  border-radius: 12px !important;
  border: 1px solid rgba(0, 0, 0, 0.06) !important;
  overflow: hidden;
  :deep(.el-card__body) { padding: 0; }
  .calc-body {
    padding: 16px 20px;
    :deep(.el-form-item) { margin-bottom: 0; }
  }
}

.card-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 20px; border-bottom: 1px solid #f1f5f9; background: #fff;
  .header-left { display: flex; align-items: center; gap: 10px; }
  .header-title {
    font-size: 15px; font-weight: 600; color: #1e293b;
    display: flex; align-items: center; gap: 8px;
    &::before { content: ''; display: inline-block; width: 3px; height: 16px;
      background: linear-gradient(180deg, #3b82f6, #60a5fa); border-radius: 2px; }
  }
  .count-tag {
    font-size: 12px; background: #eff6ff; color: #3b82f6;
    border: 1px solid #bfdbfe; border-radius: 10px; padding: 2px 10px; font-weight: 500;
  }
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
.floating-rent-val { color: #2E75B6; font-weight: 700; }
</style>
