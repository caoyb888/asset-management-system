<template>
  <div class="fin-aging">
    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form inline :model="filterForm">
        <el-form-item label="项目">
          <el-select v-model="filterForm.projectId" placeholder="全部项目" clearable style="width: 200px">
            <el-option v-for="p in projectList" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="商家ID">
          <el-input-number v-model="filterForm.merchantId" placeholder="商家ID" :min="1" :controls="false" clearable style="width: 130px" />
        </el-form-item>
        <el-form-item label="统计日期">
          <el-date-picker
            v-model="filterForm.statDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="默认最新"
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="loadData">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
        <span v-if="latestDate" class="latest-tip">
          <el-icon><Clock /></el-icon>
          最新账龄日期：<strong>{{ latestDate }}</strong>
        </span>
      </el-form>
    </el-card>

    <!-- 账龄堆叠柱状图 -->
    <el-card shadow="never">
      <template #header><span>账龄分布（按商家）</span></template>
      <div ref="agingChartRef" style="height: 340px; width: 100%" />
    </el-card>

    <!-- 明细表格 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header-flex">
          <span>账龄明细</span>
          <el-tag type="info" size="small">共 {{ tableData.length }} 条</el-tag>
        </div>
      </template>
      <el-table :data="tableData" border stripe size="small" v-loading="loading" max-height="480">
        <el-table-column prop="merchantId" label="商家ID" width="90" />
        <el-table-column prop="contractId" label="合同ID" width="90" />
        <el-table-column prop="statDate" label="统计日期" width="110" />
        <el-table-column label="欠款合计（元）" align="right" min-width="120">
          <template #default="{ row }">
            <span class="text-warn">{{ fmtMoney(row.totalOutstanding) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="30天内" align="right" width="110">
          <template #default="{ row }">{{ fmtMoney(row.within30) }}</template>
        </el-table-column>
        <el-table-column label="31-60天" align="right" width="110">
          <template #default="{ row }">{{ fmtMoney(row.days3160) }}</template>
        </el-table-column>
        <el-table-column label="61-90天" align="right" width="110">
          <template #default="{ row }">{{ fmtMoney(row.days6190) }}</template>
        </el-table-column>
        <el-table-column label="91-180天" align="right" width="110">
          <template #default="{ row }">
            <span v-if="Number(row.days91180) > 0" class="text-warn">{{ fmtMoney(row.days91180) }}</span>
            <span v-else>{{ fmtMoney(row.days91180) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="181-365天" align="right" width="110">
          <template #default="{ row }">
            <span v-if="Number(row.days181365) > 0" class="text-down">{{ fmtMoney(row.days181365) }}</span>
            <span v-else>{{ fmtMoney(row.days181365) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="365天以上" align="right" width="110">
          <template #default="{ row }">
            <span v-if="Number(row.over365) > 0" class="text-down">{{ fmtMoney(row.over365) }}</span>
            <span v-else>{{ fmtMoney(row.over365) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.merchantId"
              size="small"
              type="primary"
              link
              @click="drillDown(row.merchantId)"
            >应收明细</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Clock } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { getAgingAnalysis } from '@/api/rpt/finance'
import { getProjectList } from '@/api/base/project'
import type { FinAgingAnalysisVO } from '@/api/rpt/finance'

const router = useRouter()
const loading = ref(false)
const tableData = ref<FinAgingAnalysisVO[]>([])
const projectList = ref<{ id: number; projectName: string; projectCode: string }[]>([])
const latestDate = ref<string | null>(null)
const filterForm = reactive({
  projectId: null as number | null,
  merchantId: null as number | null,
  statDate: null as string | null,
})

const agingChartRef = ref<HTMLDivElement>()
let agingChart: echarts.ECharts | null = null

onMounted(async () => {
  await loadProjects()
  await loadData()
  window.addEventListener('resize', handleResize)
})
onUnmounted(() => {
  agingChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
function handleResize() {
  agingChart?.resize()
}

async function loadProjects() {
  projectList.value = await getProjectList()
}

async function loadData() {
  loading.value = true
  try {
    const params: any = {
      projectId: filterForm.projectId ?? undefined,
      merchantId: filterForm.merchantId ?? undefined,
      statDate: filterForm.statDate ?? undefined,
    }
    const result = await getAgingAnalysis(params)
    tableData.value = result
    if (result.length > 0 && result[0].statDate) {
      latestDate.value = result[0].statDate as string
    }
    await nextTick()
    updateAgingChart()
  } finally {
    loading.value = false
  }
}

function resetFilter() {
  filterForm.projectId = null
  filterForm.merchantId = null
  filterForm.statDate = null
  loadData()
}

function updateAgingChart() {
  if (!agingChart && agingChartRef.value) agingChart = echarts.init(agingChartRef.value)
  if (!agingChart) return
  // 按商家展示账龄（取前20条）
  const displayData = tableData.value
    .filter(r => r.merchantId && Number(r.merchantId) > 0)
    .slice(0, 20)
  const merchants = displayData.map(r => `商家${r.merchantId}`)
  const buckets = [
    { name: '30天内', key: 'within30' as keyof FinAgingAnalysisVO, color: '#409eff' },
    { name: '31-60天', key: 'days3160' as keyof FinAgingAnalysisVO, color: '#67c23a' },
    { name: '61-90天', key: 'days6190' as keyof FinAgingAnalysisVO, color: '#e6a23c' },
    { name: '91-180天', key: 'days91180' as keyof FinAgingAnalysisVO, color: '#f56c6c' },
    { name: '181-365天', key: 'days181365' as keyof FinAgingAnalysisVO, color: '#c0392b' },
    { name: '365天以上', key: 'over365' as keyof FinAgingAnalysisVO, color: '#2c3e50' },
  ]
  agingChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any[]) => {
        const header = params[0]?.axisValue ?? ''
        const lines = params.map(
          p => `${p.marker}${p.seriesName}：${p.value != null ? (Number(p.value) / 10000).toFixed(2) + '万元' : '-'}`,
        )
        return [header, ...lines].join('<br/>')
      },
    },
    legend: { data: buckets.map(b => b.name), top: 0, type: 'scroll' },
    grid: { left: 60, right: 20, top: 50, bottom: 60 },
    xAxis: { type: 'category', data: merchants, axisLabel: { rotate: 30, fontSize: 11 } },
    yAxis: { type: 'value', name: '万元' },
    series: buckets.map(b => ({
      name: b.name,
      type: 'bar',
      stack: 'aging',
      data: displayData.map(r => Number(r[b.key] ?? 0)),
      itemStyle: { color: b.color },
    })),
  })
}

function drillDown(merchantId: number | null) {
  if (!merchantId) return
  router.push({ path: '/fin/receivables', query: { merchantId: String(merchantId) } })
}

function fmtMoney(v?: number | null) {
  if (v == null || Number(v) === 0) return '-'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}
</script>

<style scoped lang="scss">
.fin-aging {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.filter-card .latest-tip {
  margin-left: 16px;
  font-size: 13px;
  color: #909399;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.card-header-flex {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.text-down { color: #f56c6c; font-weight: 600; }
.text-warn { color: #e6a23c; font-weight: 600; }
</style>
