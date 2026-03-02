<template>
  <div class="opr-dashboard">
    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form inline :model="filterForm">
        <el-form-item label="项目">
          <el-select
            v-model="filterForm.projectId"
            placeholder="全部项目"
            clearable
            style="width: 200px"
          >
            <el-option v-for="p in projectList" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="loadData">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
        <span v-if="dashboard.latestMonth" class="latest-tip">
          <el-icon><Clock /></el-icon>
          数据更新至：<strong>{{ dashboard.latestMonth }}</strong>
        </span>
      </el-form>
    </el-card>

    <!-- KPI 卡片 -->
    <el-row :gutter="16" class="kpi-row">
      <el-col :span="6" v-for="kpi in kpiCards" :key="kpi.label">
        <el-card class="kpi-card" shadow="never" :style="{ borderTop: `4px solid ${kpi.color}` }">
          <div class="kpi-title">{{ kpi.label }}</div>
          <div class="kpi-value" :style="{ color: kpi.color }">{{ kpi.value }}</div>
          <div v-if="kpi.yoy != null" class="kpi-yoy" :class="kpi.yoyClass">
            <el-icon><component :is="kpi.yoyIcon" /></el-icon>
            同比 {{ fmtDelta(kpi.yoy) }}
          </div>
          <div v-if="kpi.sub" class="kpi-sub">{{ kpi.sub }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 趋势图：营收 + 客流 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>营收趋势（近12月）</span></template>
          <div ref="revenueChartRef" class="chart-container" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>客流趋势（近12月）</span></template>
          <div ref="passengerChartRef" class="chart-container" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 到期预警 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header-flex">
          <span>合同到期预警</span>
          <el-tag type="danger" v-if="dashboard.expiringWithin30">
            30天内到期：{{ dashboard.expiringWithin30 }} 份
          </el-tag>
        </div>
      </template>
      <div ref="warningChartRef" style="height: 220px; width: 100%" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Clock, Top, Bottom, Minus } from '@element-plus/icons-vue'
import { getOprDashboard } from '@/api/rpt/operation'
import { getProjectList } from '@/api/base/project'
import type { OprDashboardVO } from '@/api/rpt/operation'

const loading = ref(false)
const dashboard = ref<OprDashboardVO>({
  latestMonth: null, totalRevenue: null, floatingRentAmount: null,
  avgRevenuePerSqm: null, passengerFlow: null, changeCount: null,
  terminatedContracts: null, revenueYoY: null, passengerFlowYoY: null,
  avgRevenuePerSqmYoY: null, expiringWithin30: null, expiringWithin60: null,
  expiringWithin90: null, revenueTrend: [], passengerTrend: [], projectComparison: [],
})
const projectList = ref<{ id: number; projectName: string; projectCode: string }[]>([])
const filterForm = reactive({ projectId: null as number | null })

const kpiCards = computed(() => [
  {
    label: '本月营收（万元）',
    value: fmtWan(dashboard.value.totalRevenue),
    color: '#409eff',
    yoy: dashboard.value.revenueYoY,
    yoyClass: yoyClass(dashboard.value.revenueYoY, false),
    yoyIcon: yoyIcon(dashboard.value.revenueYoY),
    sub: `浮动租金：${fmtWan(dashboard.value.floatingRentAmount)}万元`,
  },
  {
    label: '均坪效（元/㎡/月）',
    value: fmtMoney(dashboard.value.avgRevenuePerSqm),
    color: '#67c23a',
    yoy: dashboard.value.avgRevenuePerSqmYoY,
    yoyClass: yoyClass(dashboard.value.avgRevenuePerSqmYoY, false),
    yoyIcon: yoyIcon(dashboard.value.avgRevenuePerSqmYoY),
    sub: null,
  },
  {
    label: '客流量（万人次）',
    value: fmtWan(dashboard.value.passengerFlow),
    color: '#e6a23c',
    yoy: dashboard.value.passengerFlowYoY,
    yoyClass: yoyClass(dashboard.value.passengerFlowYoY, false),
    yoyIcon: yoyIcon(dashboard.value.passengerFlowYoY),
    sub: null,
  },
  {
    label: '合同变更 / 解约',
    value: `${dashboard.value.changeCount ?? '-'} / ${dashboard.value.terminatedContracts ?? '-'}`,
    color: '#f56c6c',
    yoy: null,
    yoyClass: '',
    yoyIcon: Minus,
    sub: `30天内到期：${dashboard.value.expiringWithin30 ?? '-'} 份`,
  },
])

const revenueChartRef = ref<HTMLDivElement>()
const passengerChartRef = ref<HTMLDivElement>()
const warningChartRef = ref<HTMLDivElement>()
let revenueChart: echarts.ECharts | null = null
let passengerChart: echarts.ECharts | null = null
let warningChart: echarts.ECharts | null = null

onMounted(async () => {
  await loadProjects()
  await loadData()
  window.addEventListener('resize', handleResize)
})
onUnmounted(() => {
  revenueChart?.dispose()
  passengerChart?.dispose()
  warningChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
function handleResize() {
  revenueChart?.resize()
  passengerChart?.resize()
  warningChart?.resize()
}

async function loadProjects() {
  projectList.value = await getProjectList()
}

async function loadData() {
  loading.value = true
  try {
    const params = filterForm.projectId ? { projectId: filterForm.projectId } : {}
    dashboard.value = await getOprDashboard(params)
    await nextTick()
    initCharts()
  } finally {
    loading.value = false
  }
}

function resetFilter() {
  filterForm.projectId = null
  loadData()
}

function initCharts() {
  // 营收趋势
  if (!revenueChart && revenueChartRef.value) revenueChart = echarts.init(revenueChartRef.value)
  if (revenueChart) {
    const data = dashboard.value.revenueTrend ?? []
    revenueChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['营收（万元）', '浮动租金（万元）'], top: 0 },
      grid: { left: 55, right: 20, top: 40, bottom: 30 },
      xAxis: { type: 'category', data: data.map(d => d.timeDim), axisLabel: { rotate: 30 } },
      yAxis: { type: 'value', name: '万元' },
      series: [
        {
          name: '营收（万元）',
          type: 'line',
          smooth: true,
          data: data.map(d => d.revenueAmount != null ? +(Number(d.revenueAmount) / 10000).toFixed(2) : null),
          itemStyle: { color: '#409eff' },
          areaStyle: { color: 'rgba(64,158,255,0.1)' },
        },
        {
          name: '浮动租金（万元）',
          type: 'bar',
          data: data.map(d => d.floatingRentAmount != null ? +(Number(d.floatingRentAmount) / 10000).toFixed(2) : null),
          itemStyle: { color: '#67c23a' },
        },
      ],
    })
  }

  // 客流趋势
  if (!passengerChart && passengerChartRef.value) passengerChart = echarts.init(passengerChartRef.value)
  if (passengerChart) {
    const data = dashboard.value.passengerTrend ?? []
    passengerChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 55, right: 20, top: 20, bottom: 30 },
      xAxis: { type: 'category', data: data.map(d => d.timeDim), axisLabel: { rotate: 30 } },
      yAxis: { type: 'value', name: '人次' },
      series: [
        {
          name: '客流量',
          type: 'bar',
          data: data.map(d => d.passengerFlow),
          itemStyle: { color: '#e6a23c' },
        },
        {
          name: '日均客流',
          type: 'line',
          smooth: true,
          data: data.map(d => d.avgDailyPassenger),
          itemStyle: { color: '#f56c6c' },
          yAxisIndex: 0,
        },
      ],
    })
  }

  // 到期预警柱状图
  if (!warningChart && warningChartRef.value) warningChart = echarts.init(warningChartRef.value)
  if (warningChart) {
    warningChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['30天内', '31-60天', '61-90天'], top: 0 },
      grid: { left: 50, right: 20, top: 40, bottom: 30 },
      xAxis: { type: 'category', data: ['当前汇总'] },
      yAxis: { type: 'value', name: '合同数', minInterval: 1 },
      series: [
        {
          name: '30天内',
          type: 'bar',
          stack: 'expiring',
          data: [dashboard.value.expiringWithin30 ?? 0],
          itemStyle: { color: '#f56c6c' },
          label: { show: true, position: 'inside' },
        },
        {
          name: '31-60天',
          type: 'bar',
          stack: 'expiring',
          data: [
            Math.max(0, (dashboard.value.expiringWithin60 ?? 0) - (dashboard.value.expiringWithin30 ?? 0)),
          ],
          itemStyle: { color: '#e6a23c' },
          label: { show: true, position: 'inside' },
        },
        {
          name: '61-90天',
          type: 'bar',
          stack: 'expiring',
          data: [
            Math.max(0, (dashboard.value.expiringWithin90 ?? 0) - (dashboard.value.expiringWithin60 ?? 0)),
          ],
          itemStyle: { color: '#67c23a' },
          label: { show: true, position: 'inside' },
        },
      ],
    })
  }
}

function fmtWan(v?: number | null) {
  if (v == null) return '-'
  return (Number(v) / 10000).toFixed(2)
}
function fmtMoney(v?: number | null) {
  return v != null ? Number(v).toFixed(2) : '-'
}
function fmtDelta(v?: number | null) {
  if (v == null) return '-'
  return `${Number(v) > 0 ? '+' : ''}${Number(v).toFixed(2)}%`
}
function yoyClass(v?: number | null, reverseGood = false) {
  if (v == null) return 'change-neutral'
  const up = Number(v) > 0
  const good = reverseGood ? !up : up
  return good ? 'change-up' : (Number(v) < 0 ? 'change-down' : 'change-neutral')
}
function yoyIcon(v?: number | null) {
  if (v == null || Number(v) === 0) return Minus
  return Number(v) > 0 ? Top : Bottom
}
</script>

<style scoped lang="scss">
.opr-dashboard {
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
.kpi-row, .chart-row { margin: 0 !important; }
.kpi-card {
  text-align: center;
  padding: 4px 0;
  .kpi-title { font-size: 13px; color: #606266; margin-bottom: 6px; }
  .kpi-value { font-size: 28px; font-weight: 700; margin-bottom: 8px; }
  .kpi-yoy {
    font-size: 12px;
    display: inline-flex;
    align-items: center;
    gap: 2px;
    &.change-up { color: #67c23a; }
    &.change-down { color: #f56c6c; }
    &.change-neutral { color: #909399; }
  }
  .kpi-sub { font-size: 12px; color: #c0c4cc; margin-top: 4px; }
}
.card-header-flex {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.chart-container { height: 280px; width: 100%; }
</style>
