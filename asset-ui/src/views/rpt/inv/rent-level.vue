<template>
  <div class="inv-rent-level">
    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form inline :model="filterForm">
        <el-form-item label="项目">
          <el-select v-model="filterForm.projectId" placeholder="全部项目" clearable style="width: 200px">
            <el-option v-for="p in projectList" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="业态">
          <el-input v-model="filterForm.formatType" placeholder="全部业态" clearable style="width: 140px" />
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
      </el-form>
    </el-card>

    <!-- 图表区 -->
    <el-row :gutter="16">
      <!-- 业态均价柱状图 -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <div style="display:flex; align-items:center; justify-content:space-between">
              <span>各业态平均租金（元/㎡/月）</span>
              <span class="yoy-legend">
                <span class="dot dot-cur"></span>当期均价
                <span class="dot dot-prev"></span>去年同期
              </span>
            </div>
          </template>
          <div ref="barChartRef" class="chart-container" />
        </el-card>
      </el-col>

      <!-- 各项目对比 -->
      <el-col :span="10">
        <el-card shadow="never">
          <template #header><span>签约量分布（㎡）</span></template>
          <div ref="areaChartRef" class="chart-container" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 明细表格 -->
    <el-card shadow="never">
      <template #header><span>租金水平明细</span></template>
      <el-table :data="tableData" border stripe size="small" v-loading="loading">
        <el-table-column label="项目" min-width="140">
          <template #default="{ row }">{{ projectNameMap[row.projectId!] || `项目${row.projectId}` }}</template>
        </el-table-column>
        <el-table-column prop="formatType" label="业态" width="120" />
        <el-table-column prop="contractCount" label="合同数" align="right" width="90" />
        <el-table-column label="签约面积(㎡)" align="right">
          <template #default="{ row }">{{ fmtArea(row.contractArea) }}</template>
        </el-table-column>
        <el-table-column label="合同金额(万元)" align="right">
          <template #default="{ row }">{{ fmtWan(row.contractAmount) }}</template>
        </el-table-column>
        <el-table-column label="均租(元/㎡/月)" align="right">
          <template #default="{ row }">
            <b>{{ fmtMoney(row.avgRentPrice) }}</b>
          </template>
        </el-table-column>
        <el-table-column label="去年同期" align="right">
          <template #default="{ row }">{{ fmtMoney(row.prevAvgRentPrice) }}</template>
        </el-table-column>
        <el-table-column label="同比增长" align="right" width="100">
          <template #default="{ row }">
            <span :class="yoyClass(row.avgRentPriceYoY)">{{ fmtDelta(row.avgRentPriceYoY) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getRentLevel } from '@/api/rpt/investment'
import { getProjectList } from '@/api/base/project'
import type { RentLevelVO } from '@/api/rpt/investment'

const loading = ref(false)
const tableData = ref<RentLevelVO[]>([])
const projectList = ref<{ id: number; projectName: string; projectCode: string }[]>([])
const filterForm = reactive({
  projectId: null as number | null,
  formatType: '',
  statDate: undefined as string | undefined,
})

const projectNameMap = computed(() => {
  const map: Record<number, string> = {}
  projectList.value.forEach(p => { map[p.id] = p.projectName })
  return map
})

const barChartRef = ref<HTMLDivElement>()
const areaChartRef = ref<HTMLDivElement>()
let barChart: echarts.ECharts | null = null
let areaChart: echarts.ECharts | null = null

onMounted(async () => {
  await loadProjects()
  await loadData()
  window.addEventListener('resize', handleResize)
})
onUnmounted(() => {
  barChart?.dispose()
  areaChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
function handleResize() {
  barChart?.resize()
  areaChart?.resize()
}

async function loadProjects() {
  projectList.value = await getProjectList()
}

async function loadData() {
  loading.value = true
  try {
    tableData.value = await getRentLevel({
      projectId: filterForm.projectId ?? undefined,
      formatType: filterForm.formatType || undefined,
      statDate: filterForm.statDate,
    })
    await nextTick()
    updateBarChart()
    updateAreaChart()
  } finally {
    loading.value = false
  }
}

function resetFilter() {
  filterForm.projectId = null
  filterForm.formatType = ''
  filterForm.statDate = undefined
  loadData()
}

function updateBarChart() {
  if (!barChart && barChartRef.value) barChart = echarts.init(barChartRef.value)
  if (!barChart) return
  // 按业态聚合（取均价均值）
  const formatMap: Record<string, { cur: number[]; prev: number[] }> = {}
  tableData.value.forEach(r => {
    const key = r.formatType || '未知'
    if (!formatMap[key]) formatMap[key] = { cur: [], prev: [] }
    if (r.avgRentPrice != null) formatMap[key].cur.push(Number(r.avgRentPrice))
    if (r.prevAvgRentPrice != null) formatMap[key].prev.push(Number(r.prevAvgRentPrice))
  })
  const formats = Object.keys(formatMap)
  const curPrices = formats.map(f => {
    const arr = formatMap[f].cur
    return arr.length ? Number((arr.reduce((a, b) => a + b, 0) / arr.length).toFixed(2)) : 0
  })
  const prevPrices = formats.map(f => {
    const arr = formatMap[f].prev
    return arr.length ? Number((arr.reduce((a, b) => a + b, 0) / arr.length).toFixed(2)) : null
  })
  barChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['当期均价', '去年同期'], top: 0 },
    grid: { left: 55, right: 20, top: 40, bottom: 60 },
    xAxis: { type: 'category', data: formats, axisLabel: { rotate: 30 } },
    yAxis: { type: 'value', name: '元/㎡/月' },
    series: [
      {
        name: '当期均价',
        type: 'bar',
        data: curPrices,
        itemStyle: { color: '#409eff' },
        label: { show: true, position: 'top', formatter: (p: any) => p.value?.toFixed(0) },
      },
      {
        name: '去年同期',
        type: 'bar',
        data: prevPrices,
        itemStyle: { color: '#c0c4cc' },
      },
    ],
  })
}

function updateAreaChart() {
  if (!areaChart && areaChartRef.value) areaChart = echarts.init(areaChartRef.value)
  if (!areaChart) return
  // 按项目聚合签约面积
  const projMap: Record<string, number> = {}
  tableData.value.forEach(r => {
    const key = projectNameMap.value[r.projectId!] || `项目${r.projectId}`
    projMap[key] = (projMap[key] ?? 0) + Number(r.contractArea ?? 0)
  })
  const pieData = Object.entries(projMap).map(([name, value]) => ({ name, value: Number(value.toFixed(0)) }))
  areaChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (p: any) => `${p.name}<br/>签约面积：${p.value}㎡<br/>占比：${p.percent}%`,
    },
    legend: { orient: 'vertical', left: 'left', top: 'middle' },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['65%', '50%'],
        data: pieData,
        label: { formatter: '{b}\n{d}%' },
      },
    ],
  })
}

function fmtMoney(v?: number | null) {
  return v != null ? Number(v).toFixed(2) : '-'
}
function fmtArea(v?: number | null) {
  return v != null ? Number(v).toFixed(0) : '-'
}
function fmtWan(v?: number | null) {
  if (v == null) return '-'
  return (Number(v) / 10000).toFixed(2)
}
function fmtDelta(v?: number | null) {
  if (v == null) return '-'
  return `${Number(v) > 0 ? '+' : ''}${Number(v).toFixed(2)}%`
}
function yoyClass(v?: number | null) {
  if (v == null) return ''
  return Number(v) > 0 ? 'text-up' : Number(v) < 0 ? 'text-down' : ''
}
</script>

<style scoped lang="scss">
.inv-rent-level {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.chart-container { height: 300px; width: 100%; }
.text-up { color: #67c23a; font-weight: 600; }
.text-down { color: #f56c6c; font-weight: 600; }

.yoy-legend {
  font-size: 12px;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 6px;
  .dot {
    display: inline-block;
    width: 10px;
    height: 10px;
    border-radius: 2px;
    &.dot-cur { background: #409eff; }
    &.dot-prev { background: #c0c4cc; }
  }
}
</style>
