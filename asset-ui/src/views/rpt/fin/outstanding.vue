<template>
  <div class="fin-outstanding">
    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form inline :model="filterForm">
        <el-form-item label="项目">
          <el-select v-model="filterForm.projectId" placeholder="全部项目" clearable style="width: 200px">
            <el-option v-for="p in projectList" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="费项类型">
          <el-input v-model="filterForm.feeItemType" placeholder="全部费项" clearable style="width: 130px" />
        </el-form-item>
        <el-form-item label="月份范围">
          <el-date-picker
            v-model="monthRange"
            type="monthrange"
            value-format="YYYY-MM"
            start-placeholder="开始月份"
            end-placeholder="结束月份"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item label="对比">
          <el-radio-group v-model="filterForm.compareMode" @change="loadData">
            <el-radio-button value="NONE">不对比</el-radio-button>
            <el-radio-button value="YOY">同比</el-radio-button>
            <el-radio-button value="MOM">环比</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="loadData">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 图表区 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="16">
        <el-card shadow="never">
          <template #header><span>欠款/逾期金额趋势</span></template>
          <div ref="trendChartRef" class="chart-container" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <template #header><span>逾期率趋势</span></template>
          <div ref="overdueRateChartRef" class="chart-container" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 明细表格 -->
    <el-card shadow="never">
      <template #header><span>欠款汇总明细</span></template>
      <el-table :data="tableData" border stripe size="small" v-loading="loading">
        <el-table-column prop="timeDim" label="月份" width="100" />
        <el-table-column label="项目" min-width="130">
          <template #default="{ row }">{{ projectNameMap[row.projectId!] || `项目${row.projectId}` }}</template>
        </el-table-column>
        <el-table-column prop="feeItemType" label="费项类型" width="100" />
        <el-table-column label="应收（万元）" align="right">
          <template #default="{ row }">{{ fmtWan(row.receivableAmount) }}</template>
        </el-table-column>
        <el-table-column label="欠款（万元）" align="right">
          <template #default="{ row }">
            <span class="text-warn">{{ fmtWan(row.outstandingAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="逾期金额（万元）" align="right">
          <template #default="{ row }">
            <span class="text-down">{{ fmtWan(row.overdueAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="逾期率" align="right" width="90">
          <template #default="{ row }">
            <span :class="row.overdueRate != null && Number(row.overdueRate) > 0 ? 'text-down' : ''">
              {{ fmtPct(row.overdueRate) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="减免（万元）" align="right">
          <template #default="{ row }">{{ fmtWan(row.deductionAmount) }}</template>
        </el-table-column>
        <el-table-column label="调整（万元）" align="right">
          <template #default="{ row }">{{ fmtWan(row.adjustmentAmount) }}</template>
        </el-table-column>
        <el-table-column v-if="filterForm.compareMode !== 'NONE'" label="欠款同比" align="right" width="90">
          <template #default="{ row }">
            <span :class="deltaClass(row.outstandingYoY, true)">{{ fmtDelta(row.outstandingYoY) }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="filterForm.compareMode !== 'NONE'" label="逾期率同比" align="right" width="90">
          <template #default="{ row }">
            <span :class="deltaClass(row.overdueRateYoY, true)">{{ fmtDelta(row.overdueRateYoY) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getOutstandingSummary } from '@/api/rpt/finance'
import { getProjectList } from '@/api/base/project'
import type { FinOutstandingSummaryVO } from '@/api/rpt/finance'

const loading = ref(false)
const tableData = ref<FinOutstandingSummaryVO[]>([])
const projectList = ref<{ id: number; projectName: string; projectCode: string }[]>([])
const monthRange = ref<[string, string] | null>(null)
const filterForm = reactive({
  projectId: null as number | null,
  feeItemType: '',
  compareMode: 'NONE' as 'NONE' | 'YOY' | 'MOM',
})

const projectNameMap = computed(() => {
  const map: Record<number, string> = {}
  projectList.value.forEach(p => { map[p.id] = p.projectName })
  return map
})

const trendChartRef = ref<HTMLDivElement>()
const overdueRateChartRef = ref<HTMLDivElement>()
let trendChart: echarts.ECharts | null = null
let overdueRateChart: echarts.ECharts | null = null

onMounted(async () => {
  await loadProjects()
  await loadData()
  window.addEventListener('resize', handleResize)
})
onUnmounted(() => {
  trendChart?.dispose()
  overdueRateChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
function handleResize() {
  trendChart?.resize()
  overdueRateChart?.resize()
}

async function loadProjects() {
  projectList.value = await getProjectList()
}

async function loadData() {
  loading.value = true
  try {
    const params: any = {
      projectId: filterForm.projectId ?? undefined,
      feeItemType: filterForm.feeItemType || undefined,
      compareMode: filterForm.compareMode,
    }
    if (monthRange.value) {
      params.startMonth = monthRange.value[0]
      params.endMonth = monthRange.value[1]
    }
    tableData.value = await getOutstandingSummary(params)
    await nextTick()
    updateTrendChart()
    updateOverdueRateChart()
  } finally {
    loading.value = false
  }
}

function resetFilter() {
  filterForm.projectId = null
  filterForm.feeItemType = ''
  filterForm.compareMode = 'NONE'
  monthRange.value = null
  loadData()
}

function updateTrendChart() {
  if (!trendChart && trendChartRef.value) trendChart = echarts.init(trendChartRef.value)
  if (!trendChart) return
  // 按时间维度聚合欠款/逾期
  const timeMap: Record<string, { outstanding: number; overdue: number }> = {}
  tableData.value.forEach(r => {
    if (!timeMap[r.timeDim]) timeMap[r.timeDim] = { outstanding: 0, overdue: 0 }
    timeMap[r.timeDim].outstanding += Number(r.outstandingAmount ?? 0)
    timeMap[r.timeDim].overdue += Number(r.overdueAmount ?? 0)
  })
  const times = Object.keys(timeMap).sort()
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['欠款（万元）', '逾期金额（万元）'], top: 0 },
    grid: { left: 60, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: times, axisLabel: { rotate: 30 } },
    yAxis: { type: 'value', name: '万元' },
    series: [
      {
        name: '欠款（万元）',
        type: 'bar',
        data: times.map(t => +(timeMap[t].outstanding / 10000).toFixed(2)),
        itemStyle: { color: '#e6a23c' },
      },
      {
        name: '逾期金额（万元）',
        type: 'bar',
        data: times.map(t => +(timeMap[t].overdue / 10000).toFixed(2)),
        itemStyle: { color: '#f56c6c' },
      },
    ],
  })
}

function updateOverdueRateChart() {
  if (!overdueRateChart && overdueRateChartRef.value) overdueRateChart = echarts.init(overdueRateChartRef.value)
  if (!overdueRateChart) return
  const timeMap: Record<string, { receivable: number; overdue: number }> = {}
  tableData.value.forEach(r => {
    if (!timeMap[r.timeDim]) timeMap[r.timeDim] = { receivable: 0, overdue: 0 }
    timeMap[r.timeDim].receivable += Number(r.receivableAmount ?? 0)
    timeMap[r.timeDim].overdue += Number(r.overdueAmount ?? 0)
  })
  const times = Object.keys(timeMap).sort()
  const rateData = times.map(t => {
    const rec = timeMap[t].receivable
    if (rec === 0) return null
    return +((timeMap[t].overdue / rec) * 100).toFixed(2)
  })
  overdueRateChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params: any[]) =>
        params.map(p => `${p.marker}${p.seriesName}：${p.value != null ? p.value + '%' : '-'}`).join('<br/>'),
    },
    grid: { left: 50, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: times, axisLabel: { rotate: 30 } },
    yAxis: { type: 'value', name: '%', max: 100 },
    series: [
      {
        name: '逾期率(%)',
        type: 'line',
        smooth: true,
        data: rateData,
        itemStyle: { color: '#f56c6c' },
        areaStyle: { color: 'rgba(245,108,108,0.1)' },
      },
    ],
  })
}

function fmtWan(v?: number | null) {
  if (v == null) return '-'
  return (Number(v) / 10000).toFixed(2)
}
function fmtPct(v?: number | null) {
  return v != null ? `${Number(v).toFixed(2)}%` : '-'
}
function fmtDelta(v?: number | null) {
  if (v == null) return '-'
  return `${Number(v) > 0 ? '+' : ''}${Number(v).toFixed(2)}%`
}
function deltaClass(v?: number | null, reverseGood = false) {
  if (v == null) return ''
  const up = Number(v) > 0
  const good = reverseGood ? !up : up
  return good ? 'text-up' : Number(v) < 0 ? 'text-down' : ''
}
</script>

<style scoped lang="scss">
.fin-outstanding {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.chart-row { margin: 0 !important; }
.chart-container { height: 300px; width: 100%; }
.text-up { color: #67c23a; font-weight: 600; }
.text-down { color: #f56c6c; font-weight: 600; }
.text-warn { color: #e6a23c; font-weight: 600; }
</style>
