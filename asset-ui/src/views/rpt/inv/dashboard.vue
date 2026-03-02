<template>
  <div class="inv-dashboard">
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
            <el-option
              v-for="p in projectList"
              :key="p.id"
              :label="p.projectName"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="loadData">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
        <span v-if="dashboard.latestDate" class="latest-date-tip">
          <el-icon><Clock /></el-icon>
          数据更新至：<strong>{{ dashboard.latestDate }}</strong>
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

    <!-- 图表区 第一行：漏斗 + 趋势 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>
            <span>客户跟进漏斗</span>
          </template>
          <div ref="funnelChartRef" class="chart-container" />
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>
            <div class="card-header-flex">
              <span>意向 / 合同新增趋势（近30天）</span>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-container" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区 第二行：项目业绩对比 -->
    <el-card shadow="never">
      <template #header>
        <span>项目业绩对比</span>
      </template>
      <div ref="compareChartRef" style="height: 280px; width: 100%" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Clock, Top, Bottom, Minus } from '@element-plus/icons-vue'
import { getInvDashboard } from '@/api/rpt/investment'
import { getProjectList } from '@/api/base/project'
import type { InvDashboardVO } from '@/api/rpt/investment'

const loading = ref(false)
const dashboard = ref<InvDashboardVO>({
  latestDate: null, intentionCount: null, intentionSigned: null,
  newIntentionToday: null, contractCount: null, contractAmount: null,
  contractArea: null, newContractToday: null, conversionRate: null,
  avgRentPrice: null, contractCountYoY: null, contractAmountYoY: null,
  avgRentPriceYoY: null, conversionRateYoY: null,
  funnel: [], intentionTrend: [], contractTrend: [], projectComparison: [],
})
const projectList = ref<{ id: number; projectName: string; projectCode: string }[]>([])
const filterForm = reactive({ projectId: null as number | null })

const projectNameMap = computed(() => {
  const map: Record<number, string> = {}
  projectList.value.forEach(p => { map[p.id] = p.projectName })
  return map
})

// KPI 卡片
const kpiCards = computed(() => [
  {
    label: '意向协议数',
    value: dashboard.value.intentionCount ?? '-',
    color: '#e6a23c',
    yoy: null,
    yoyClass: '',
    yoyIcon: Minus,
    sub: `当日新增：${dashboard.value.newIntentionToday ?? '-'}`,
  },
  {
    label: '租赁合同数',
    value: dashboard.value.contractCount ?? '-',
    color: '#409eff',
    yoy: dashboard.value.contractCountYoY,
    yoyClass: yoyClass(dashboard.value.contractCountYoY, false),
    yoyIcon: yoyIcon(dashboard.value.contractCountYoY),
    sub: `当日新增：${dashboard.value.newContractToday ?? '-'}`,
  },
  {
    label: '意向转化率',
    value: fmtRate(dashboard.value.conversionRate),
    color: '#67c23a',
    yoy: dashboard.value.conversionRateYoY,
    yoyClass: yoyClass(dashboard.value.conversionRateYoY, false),
    yoyIcon: yoyIcon(dashboard.value.conversionRateYoY),
    sub: null,
  },
  {
    label: '平均租金（元/㎡/月）',
    value: fmtMoney(dashboard.value.avgRentPrice),
    color: '#2e75b6',
    yoy: dashboard.value.avgRentPriceYoY,
    yoyClass: yoyClass(dashboard.value.avgRentPriceYoY, false),
    yoyIcon: yoyIcon(dashboard.value.avgRentPriceYoY),
    sub: `签约面积：${fmtArea(dashboard.value.contractArea)}㎡`,
  },
])

// ECharts
const funnelChartRef = ref<HTMLDivElement>()
const trendChartRef = ref<HTMLDivElement>()
const compareChartRef = ref<HTMLDivElement>()
let funnelChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null
let compareChart: echarts.ECharts | null = null

onMounted(async () => {
  await loadProjects()
  await loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  funnelChart?.dispose()
  trendChart?.dispose()
  compareChart?.dispose()
  window.removeEventListener('resize', handleResize)
})

function handleResize() {
  funnelChart?.resize()
  trendChart?.resize()
  compareChart?.resize()
}

async function loadProjects() {
  projectList.value = await getProjectList()
}

async function loadData() {
  loading.value = true
  try {
    const params = filterForm.projectId ? { projectId: filterForm.projectId } : {}
    dashboard.value = await getInvDashboard(params)
    await nextTick()
    initOrUpdateCharts()
  } finally {
    loading.value = false
  }
}

function resetFilter() {
  filterForm.projectId = null
  loadData()
}

function initOrUpdateCharts() {
  // 漏斗图
  if (!funnelChart && funnelChartRef.value) funnelChart = echarts.init(funnelChartRef.value)
  if (funnelChart) updateFunnelChart()

  // 趋势图
  if (!trendChart && trendChartRef.value) trendChart = echarts.init(trendChartRef.value)
  if (trendChart) updateTrendChart()

  // 项目对比图
  if (!compareChart && compareChartRef.value) compareChart = echarts.init(compareChartRef.value)
  if (compareChart) updateCompareChart()
}

function updateFunnelChart() {
  const funnel = dashboard.value.funnel ?? []
  funnelChart!.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (p: any) => {
        const d = funnel[p.dataIndex]
        if (!d) return ''
        let txt = `<b>${p.name}</b><br/>数量：${d.count ?? '-'}<br/>转化率：${fmtRate(d.conversionRate)}`
        if (d.amount != null) txt += `<br/>金额：${fmtMoney(d.amount)}元`
        if (d.area != null) txt += `<br/>面积：${fmtArea(d.area)}㎡`
        return txt
      },
    },
    series: [
      {
        type: 'funnel',
        left: '10%',
        width: '80%',
        top: 20,
        bottom: 20,
        sort: 'descending',
        gap: 6,
        label: { show: true, position: 'inside', formatter: '{b}\n{c}' },
        data: funnel.map(f => ({ name: f.stageName, value: f.count ?? 0 })),
        color: ['#409eff', '#67c23a', '#e6a23c'],
      },
    ],
  })
}

function updateTrendChart() {
  const iData = dashboard.value.intentionTrend ?? []
  const cData = dashboard.value.contractTrend ?? []
  const dates = iData.length ? iData.map(d => d.timeDim) : cData.map(d => d.timeDim)
  trendChart!.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['新增意向', '新增合同'], top: 0 },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: dates, axisLabel: { rotate: 30 } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '新增意向',
        type: 'bar',
        data: iData.map(d => d.newIntention),
        itemStyle: { color: '#e6a23c' },
      },
      {
        name: '新增合同',
        type: 'bar',
        data: cData.map(d => d.newContract),
        itemStyle: { color: '#409eff' },
      },
    ],
  })
}

function updateCompareChart() {
  const list = dashboard.value.projectComparison ?? []
  const names = list.map(p => projectNameMap.value[p.projectId!] || `项目${p.projectId}`)
  compareChart!.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['意向数', '合同数', '转化率%'], top: 0 },
    grid: { left: 50, right: 60, top: 40, bottom: 60 },
    xAxis: { type: 'category', data: names, axisLabel: { rotate: 30, width: 80, overflow: 'truncate' } },
    yAxis: [
      { type: 'value', name: '数量', minInterval: 1 },
      { type: 'value', name: '转化率%', axisLabel: { formatter: '{value}%' } },
    ],
    series: [
      {
        name: '意向数',
        type: 'bar',
        data: list.map(p => p.intentionCount),
        itemStyle: { color: '#e6a23c' },
      },
      {
        name: '合同数',
        type: 'bar',
        data: list.map(p => p.contractCount),
        itemStyle: { color: '#409eff' },
      },
      {
        name: '转化率%',
        type: 'line',
        yAxisIndex: 1,
        data: list.map(p => p.conversionRate),
        itemStyle: { color: '#67c23a' },
        label: { show: true, formatter: (p: any) => `${p.value?.toFixed(1)}%` },
      },
    ],
  })
}

// ─── 格式化工具 ───
function fmtRate(v?: number | null) {
  return v != null ? `${Number(v).toFixed(2)}%` : '-'
}
function fmtDelta(v?: number | null) {
  if (v == null) return '-'
  return `${Number(v) > 0 ? '+' : ''}${Number(v).toFixed(2)}%`
}
function fmtMoney(v?: number | null) {
  if (v == null) return '-'
  const n = Number(v)
  return n >= 10000 ? (n / 10000).toFixed(2) + 'w' : n.toFixed(2)
}
function fmtArea(v?: number | null) {
  if (v == null) return '-'
  return Number(v).toFixed(0)
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
.inv-dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.filter-card .latest-date-tip {
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
  .kpi-value { font-size: 30px; font-weight: 700; margin-bottom: 8px; }
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
.chart-container { height: 300px; width: 100%; }
</style>
