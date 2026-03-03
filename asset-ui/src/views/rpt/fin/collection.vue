<template>
  <div class="fin-collection">
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

    <!-- 图表区：月度收缴率折线图 + 项目对比柱状图 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="14">
        <el-card shadow="never">
          <template #header><span>月度收缴率趋势</span></template>
          <div ref="trendChartRef" class="chart-container" />
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never">
          <template #header><span>项目收缴率对比</span></template>
          <div ref="compareChartRef" class="chart-container" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 明细表格 -->
    <el-card shadow="never">
      <template #header><span>收缴率汇总明细</span></template>
      <el-table :data="tableData" border stripe size="small" v-loading="loading">
        <el-table-column prop="timeDim" label="月份" width="100" />
        <el-table-column label="项目" min-width="130">
          <template #default="{ row }">{{ projectNameMap[row.projectId!] || `项目${row.projectId}` }}</template>
        </el-table-column>
        <el-table-column prop="feeItemType" label="费项类型" width="100" />
        <el-table-column label="应收（万元）" align="right">
          <template #default="{ row }">{{ fmtWan(row.receivableAmount) }}</template>
        </el-table-column>
        <el-table-column label="已收（万元）" align="right">
          <template #default="{ row }">{{ fmtWan(row.receivedAmount) }}</template>
        </el-table-column>
        <el-table-column label="欠款（万元）" align="right">
          <template #default="{ row }">{{ fmtWan(row.outstandingAmount) }}</template>
        </el-table-column>
        <el-table-column label="收缴率" align="right" width="90">
          <template #default="{ row }">
            <span :class="rateClass(row.collectionRate)">{{ fmtPct(row.collectionRate) }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="filterForm.compareMode !== 'NONE'" label="同比增长" align="right" width="90">
          <template #default="{ row }">
            <span :class="deltaClass(row.collectionRateYoY, false)">{{ fmtDelta(row.collectionRateYoY) }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="filterForm.compareMode === 'MOM'" label="环比增长" align="right" width="90">
          <template #default="{ row }">
            <span :class="deltaClass(row.collectionRateMoM, false)">{{ fmtDelta(row.collectionRateMoM) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getCollectionRate } from '@/api/rpt/finance'
import { getProjectList } from '@/api/base/project'
import type { FinCollectionRateVO } from '@/api/rpt/finance'

const loading = ref(false)
const tableData = ref<FinCollectionRateVO[]>([])
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
const compareChartRef = ref<HTMLDivElement>()
let trendChart: echarts.ECharts | null = null
let compareChart: echarts.ECharts | null = null

onMounted(async () => {
  await loadProjects()
  await loadData()
  window.addEventListener('resize', handleResize)
})
onUnmounted(() => {
  trendChart?.dispose()
  compareChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
function handleResize() {
  trendChart?.resize()
  compareChart?.resize()
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
    tableData.value = await getCollectionRate(params)
    await nextTick()
    updateTrendChart()
    updateCompareChart()
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
  // 按时间维度聚合收缴率（加权平均：以应收额为权重）
  const timeMap: Record<string, { receivable: number; received: number; prevRate: number | null }> = {}
  tableData.value.forEach(r => {
    if (!timeMap[r.timeDim]) timeMap[r.timeDim] = { receivable: 0, received: 0, prevRate: null }
    timeMap[r.timeDim].receivable += Number(r.receivableAmount ?? 0)
    timeMap[r.timeDim].received += Number(r.receivedAmount ?? 0)
  })
  const times = Object.keys(timeMap).sort()
  const rateData = times.map(t => {
    const rec = timeMap[t].receivable
    if (rec === 0) return null
    return +((timeMap[t].received / rec) * 100).toFixed(2)
  })
  const hasPrev = filterForm.compareMode !== 'NONE'
  const compareName = filterForm.compareMode === 'YOY' ? '同比收缴率(%)' : '环比收缴率(%)'

  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params: any[]) =>
        params.map(p => `${p.marker}${p.seriesName}：${p.value != null ? Number(p.value).toFixed(2) + '%' : '-'}`).join('<br/>'),
    },
    legend: { data: ['收缴率(%)', ...(hasPrev ? [compareName] : [])], top: 0 },
    grid: { left: 50, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: times, axisLabel: { rotate: 30 } },
    yAxis: { type: 'value', name: '%', max: 100 },
    series: [
      {
        name: '收缴率(%)',
        type: 'line',
        smooth: true,
        data: rateData,
        itemStyle: { color: '#409eff' },
        areaStyle: { color: 'rgba(64,158,255,0.1)' },
        markLine: {
          data: [{ type: 'average', name: '均值' }],
          lineStyle: { color: '#e6a23c', type: 'dashed' },
          label: { formatter: '{c}%' },
        },
      },
    ],
  })
}

function updateCompareChart() {
  if (!compareChart && compareChartRef.value) compareChart = echarts.init(compareChartRef.value)
  if (!compareChart) return
  // 按项目聚合当前时段平均收缴率
  const projectMap: Record<number, { receivable: number; received: number }> = {}
  tableData.value.forEach(r => {
    if (!r.projectId) return
    if (!projectMap[r.projectId]) projectMap[r.projectId] = { receivable: 0, received: 0 }
    projectMap[r.projectId].receivable += Number(r.receivableAmount ?? 0)
    projectMap[r.projectId].received += Number(r.receivedAmount ?? 0)
  })
  const projectIds = Object.keys(projectMap).map(Number)
  const projectNames = projectIds.map(id => projectNameMap.value[id] || `项目${id}`)
  const projectRates = projectIds.map(id => {
    const rec = projectMap[id].receivable
    if (rec === 0) return 0
    return +((projectMap[id].received / rec) * 100).toFixed(2)
  })
  compareChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params: any[]) =>
        params.map(p => `${p.marker}${p.name}：${p.value}%`).join('<br/>'),
    },
    grid: { left: 80, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'value', name: '%', max: 100 },
    yAxis: { type: 'category', data: projectNames, axisLabel: { width: 70, overflow: 'truncate' } },
    series: [
      {
        name: '收缴率',
        type: 'bar',
        data: projectRates,
        itemStyle: {
          color: (params: any) => {
            const val = params.value as number
            if (val >= 90) return '#67c23a'
            if (val >= 70) return '#e6a23c'
            return '#f56c6c'
          },
        },
        label: { show: true, position: 'right', formatter: (p: any) => `${p.value}%` },
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
function rateClass(v?: number | null) {
  if (v == null) return ''
  const n = Number(v)
  if (n >= 90) return 'text-up'
  if (n >= 70) return 'text-warn'
  return 'text-down'
}
function deltaClass(v?: number | null, reverseGood = false) {
  if (v == null) return ''
  const up = Number(v) > 0
  const good = reverseGood ? !up : up
  return good ? 'text-up' : Number(v) < 0 ? 'text-down' : ''
}
</script>

<style scoped lang="scss">
.fin-collection {
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
