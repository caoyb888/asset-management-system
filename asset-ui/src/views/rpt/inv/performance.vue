<template>
  <div class="inv-performance">
    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form inline :model="filterForm">
        <el-form-item label="项目">
          <el-select v-model="filterForm.projectId" placeholder="全部项目" clearable style="width: 200px">
            <el-option v-for="p in projectList" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item label="时间维度">
          <el-radio-group v-model="filterForm.timeUnit">
            <el-radio-button value="DAY">日</el-radio-button>
            <el-radio-button value="MONTH">月</el-radio-button>
            <el-radio-button value="YEAR">年</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="维度">
          <el-radio-group v-model="dimension">
            <el-radio-button value="project">按项目</el-radio-button>
            <el-radio-button value="contract">按合同统计</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="loadData">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 图表区 -->
    <el-row :gutter="16">
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>
            <div style="display:flex; align-items:center; justify-content:space-between">
              <span>业绩对比柱状图</span>
              <el-radio-group v-model="chartMetric" size="small" @change="updateChart">
                <el-radio-button value="count">数量</el-radio-button>
                <el-radio-button value="area">面积</el-radio-button>
                <el-radio-button value="amount">金额</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="barChartRef" class="chart-container" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <template #header><span>转化率对比</span></template>
          <div ref="rateChartRef" class="chart-container" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 明细表格 -->
    <el-card shadow="never">
      <template #header><span>业绩明细</span></template>
      <el-table :data="tableData" border stripe size="small" v-loading="loading">
        <el-table-column label="项目" min-width="140">
          <template #default="{ row }">{{ projectNameMap[row.projectId!] || `项目${row.projectId}` }}</template>
        </el-table-column>
        <el-table-column prop="intentionCount" label="意向数" align="right" />
        <el-table-column prop="intentionSigned" label="已签意向" align="right" />
        <el-table-column prop="contractCount" label="合同数" align="right" />
        <el-table-column label="签约面积(㎡)" align="right">
          <template #default="{ row }">{{ fmtArea(row.contractArea) }}</template>
        </el-table-column>
        <el-table-column label="合同金额(万元)" align="right">
          <template #default="{ row }">{{ fmtWan(row.contractAmount) }}</template>
        </el-table-column>
        <el-table-column label="转化率" align="right">
          <template #default="{ row }">
            <span :class="row.conversionRate != null && row.conversionRate >= 50 ? 'text-up' : ''">
              {{ fmtRate(row.conversionRate) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="均租(元/㎡/月)" align="right">
          <template #default="{ row }">{{ fmtMoney2(row.avgRentPrice) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import { getPerformance } from '@/api/rpt/investment'
import { getProjectList } from '@/api/base/project'
import type { PerformanceVO } from '@/api/rpt/investment'

const loading = ref(false)
const tableData = ref<PerformanceVO[]>([])
const projectList = ref<{ id: number; projectName: string; projectCode: string }[]>([])
const dateRange = ref<[string, string] | null>(null)
const dimension = ref<'project' | 'contract'>('project')
const chartMetric = ref<'count' | 'area' | 'amount'>('count')
const filterForm = reactive({
  projectId: null as number | null,
  timeUnit: 'MONTH' as 'DAY' | 'MONTH' | 'YEAR',
})

const projectNameMap = computed(() => {
  const map: Record<number, string> = {}
  projectList.value.forEach(p => { map[p.id] = p.projectName })
  return map
})

const barChartRef = ref<HTMLDivElement>()
const rateChartRef = ref<HTMLDivElement>()
let barChart: echarts.ECharts | null = null
let rateChart: echarts.ECharts | null = null

onMounted(async () => {
  await loadProjects()
  await loadData()
  window.addEventListener('resize', handleResize)
})
onUnmounted(() => {
  barChart?.dispose()
  rateChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
function handleResize() {
  barChart?.resize()
  rateChart?.resize()
}

async function loadProjects() {
  projectList.value = await getProjectList()
}

async function loadData() {
  loading.value = true
  try {
    const params: any = {
      projectId: filterForm.projectId ?? undefined,
      timeUnit: filterForm.timeUnit,
    }
    if (dateRange.value) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    tableData.value = await getPerformance(params)
    await nextTick()
    updateChart()
    updateRateChart()
  } finally {
    loading.value = false
  }
}

function resetFilter() {
  filterForm.projectId = null
  filterForm.timeUnit = 'MONTH'
  dateRange.value = null
  loadData()
}

function updateChart() {
  if (!barChart && barChartRef.value) barChart = echarts.init(barChartRef.value)
  if (!barChart) return
  const data = tableData.value
  const names = data.map(p => projectNameMap.value[p.projectId!] || `项目${p.projectId}`)
  let intentionSeries: number[] = []
  let contractSeries: number[] = []
  let legendData: string[] = []
  let yAxisName = ''

  if (chartMetric.value === 'count') {
    intentionSeries = data.map(p => p.intentionCount ?? 0)
    contractSeries = data.map(p => p.contractCount ?? 0)
    legendData = ['意向数', '合同数']
    yAxisName = '数量'
  } else if (chartMetric.value === 'area') {
    intentionSeries = []
    contractSeries = data.map(p => Number(p.contractArea ?? 0))
    legendData = ['签约面积(㎡)']
    yAxisName = '面积(㎡)'
  } else {
    intentionSeries = []
    contractSeries = data.map(p => Number((Number(p.contractAmount ?? 0) / 10000).toFixed(2)))
    legendData = ['合同金额(万元)']
    yAxisName = '金额(万元)'
  }

  const series: any[] = []
  if (chartMetric.value === 'count') {
    series.push(
      { name: '意向数', type: 'bar', data: intentionSeries, itemStyle: { color: '#e6a23c' } },
      { name: '合同数', type: 'bar', data: contractSeries, itemStyle: { color: '#409eff' } },
    )
  } else {
    series.push({ name: legendData[0], type: 'bar', data: contractSeries, itemStyle: { color: '#409eff' } })
  }

  barChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: legendData, top: 0 },
    grid: { left: 60, right: 20, top: 40, bottom: 60 },
    xAxis: { type: 'category', data: names, axisLabel: { rotate: 30, width: 80, overflow: 'truncate' } },
    yAxis: { type: 'value', name: yAxisName },
    series,
  })
}

function updateRateChart() {
  if (!rateChart && rateChartRef.value) rateChart = echarts.init(rateChartRef.value)
  if (!rateChart) return
  const data = tableData.value
  const names = data.map(p => projectNameMap.value[p.projectId!] || `项目${p.projectId}`)
  rateChart.setOption({
    tooltip: {
      trigger: 'axis',
      valueFormatter: (v: number) => `${Number(v).toFixed(2)}%`,
    },
    grid: { left: 50, right: 20, top: 20, bottom: 60 },
    xAxis: { type: 'category', data: names, axisLabel: { rotate: 30, width: 60, overflow: 'truncate' } },
    yAxis: { type: 'value', axisLabel: { formatter: '{value}%' } },
    series: [
      {
        type: 'bar',
        data: data.map(p => p.conversionRate),
        itemStyle: {
          color: (p: any) => (p.data >= 50 ? '#67c23a' : '#409eff'),
        },
        label: { show: true, position: 'top', formatter: (p: any) => `${Number(p.value).toFixed(1)}%` },
      },
    ],
  })
}

function fmtRate(v?: number | null) {
  return v != null ? `${Number(v).toFixed(2)}%` : '-'
}
function fmtArea(v?: number | null) {
  return v != null ? Number(v).toFixed(0) : '-'
}
function fmtWan(v?: number | null) {
  if (v == null) return '-'
  return (Number(v) / 10000).toFixed(2)
}
function fmtMoney2(v?: number | null) {
  return v != null ? Number(v).toFixed(2) : '-'
}
</script>

<style scoped lang="scss">
.inv-performance {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.chart-container { height: 300px; width: 100%; }
.text-up { color: #67c23a; font-weight: 600; }
</style>
